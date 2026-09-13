package br.com.fiap.arkive.service;

import br.com.fiap.arkive.domain.consulta.StatusConsulta;
import br.com.fiap.arkive.dto.response.PrescricaoResumoResponse;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.AnimalResponsavel;
import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Diagnostico;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.repository.AnimalResponsavelRepository;
import br.com.fiap.arkive.repository.DiagnosticoRepository;
import br.com.fiap.arkive.repository.PrescricaoRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@Profile("!local-nodb")
public class ConsultaResumoPdfService {

	private static final DateTimeFormatter DATA_HORA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	private final ConsultaService consultaService;
	private final ClinicalAccessService clinicalAccessService;
	private final DiagnosticoRepository diagnosticoRepository;
	private final PrescricaoRepository prescricaoRepository;
	private final AnimalResponsavelRepository animalResponsavelRepository;

	public ConsultaResumoPdfService(
			ConsultaService consultaService,
			ClinicalAccessService clinicalAccessService,
			DiagnosticoRepository diagnosticoRepository,
			PrescricaoRepository prescricaoRepository,
			AnimalResponsavelRepository animalResponsavelRepository
	) {
		this.consultaService = consultaService;
		this.clinicalAccessService = clinicalAccessService;
		this.diagnosticoRepository = diagnosticoRepository;
		this.prescricaoRepository = prescricaoRepository;
		this.animalResponsavelRepository = animalResponsavelRepository;
	}

	@Transactional(readOnly = true)
	public ConsultaResumoPdf gerarResumo(Long consultaId, UsuarioPrincipal principal) {
		Consulta consulta = consultaService.buscarEntidade(consultaId);
		clinicalAccessService.exigirLeituraConsulta(principal, consulta);
		if (!StatusConsulta.FI.getCodigo().equals(consulta.getStatus())) {
			throw new BusinessException("Resumo em PDF disponivel apenas para consulta finalizada.", HttpStatus.CONFLICT);
		}
		Diagnostico diagnostico = diagnosticoRepository.buscarDiagnosticosConfirmadosVeterinario(consultaId, PageRequest.of(0, 1))
				.stream()
				.findFirst()
				.orElseThrow(() -> new BusinessException("Consulta finalizada sem diagnostico confirmado pelo veterinario.", HttpStatus.CONFLICT));
		List<PrescricaoResumoResponse> prescricoes = prescricaoRepository.buscarPorConsulta(consultaId).stream()
				.map(PrescricaoResumoResponse::fromEntity)
				.toList();
		ConsultaResumoPdfData data = ConsultaResumoPdfData.fromEntities(
				consulta,
				diagnostico,
				prescricoes,
				responsavelPrincipalAtual(consulta)
		);
		return new ConsultaResumoPdf(render(data), filename(consulta));
	}

	private String responsavelPrincipalAtual(Consulta consulta) {
		Long animalId = consulta.getAnimal() == null ? null : consulta.getAnimal().getId();
		if (animalId == null) {
			return null;
		}
		List<AnimalResponsavel> responsaveis = animalResponsavelRepository.buscarResponsaveisPrincipaisAtivosVigentes(animalId, LocalDate.now());
		if (responsaveis.size() != 1 || responsaveis.get(0).getResponsavel() == null) {
			return null;
		}
		return responsaveis.get(0).getResponsavel().getNome();
	}

	private byte[] render(ConsultaResumoPdfData data) {
		try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			try (PdfRenderer renderer = new PdfRenderer(document, "RESUMO DO ATENDIMENTO VETERINÁRIO")) {
				render(renderer, data);
			}
			document.save(output);
			return output.toByteArray();
		} catch (IOException ex) {
			throw new BusinessException("Nao foi possivel gerar o resumo em PDF.", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private String filename(Consulta consulta) {
		String animal = consulta.getAnimal() == null ? "paciente" : consulta.getAnimal().getNome();
		String slug = Normalizer.normalize(nuloSeVazio(animal) == null ? "paciente" : animal, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9]+", "-")
				.replaceAll("(^-|-$)", "");
		if (slug.isBlank()) {
			slug = "paciente";
		}
		return "arkive-consulta-" + slug + "-" + consulta.getId() + ".pdf";
	}

	private static String nuloSeVazio(String valor) {
		return valor == null || valor.isBlank() ? null : valor.trim();
	}

	public record ConsultaResumoPdf(byte[] bytes, String filename) {
	}

	private record ConsultaResumoPdfData(
			String animalNome,
			String especie,
			String raca,
			String sexo,
			String castrado,
			LocalDateTime dataConsulta,
			String modalidade,
			String endereco,
			String motivo,
			String veterinarioNome,
			String crmv,
			String clinicaNome,
			String responsavelNome,
			String diagnostico,
			String severidade,
			String conclusao,
			List<PrescricaoResumoResponse> prescricoes
	) {
		private static ConsultaResumoPdfData fromEntities(
				Consulta consulta,
				Diagnostico diagnostico,
				List<PrescricaoResumoResponse> prescricoes,
				String responsavelNome
		) {
			Animal animal = consulta.getAnimal();
			Veterinario veterinario = consulta.getVeterinario();
			Clinica clinica = consulta.getClinica();
			return new ConsultaResumoPdfData(
					animal == null ? null : animal.getNome(),
					animal == null || animal.getEspecie() == null ? null : animal.getEspecie().getNome(),
					animal == null || animal.getRaca() == null ? null : animal.getRaca().getNome(),
					animal == null ? null : formatarSexo(animal.getSexo()),
					animal == null ? null : formatarCastrado(animal.getCastrado()),
					consulta.getDataHora(),
					formatarModalidade(consulta.getModalidade()),
					"PRESENCIAL".equals(consulta.getModalidade()) ? nuloSeVazio(consulta.getEndereco()) : null,
					consulta.getMotivo(),
					veterinario == null ? null : veterinario.getNome(),
					veterinario == null ? null : veterinario.getCrmv(),
					clinica == null ? null : clinica.getNome(),
					responsavelNome,
					diagnostico.getDiagnostico(),
					diagnostico.getSeveridade(),
					consulta.getObservacao(),
					List.copyOf(prescricoes)
			);
		}

		private static String formatarSexo(String sexo) {
			return switch (sexo == null ? "" : sexo) {
				case "M" -> "Macho";
				case "F" -> "Fêmea";
				default -> null;
			};
		}

		private static String formatarCastrado(String castrado) {
			return switch (castrado == null ? "" : castrado) {
				case "S" -> "Sim";
				case "N" -> "Não";
				default -> null;
			};
		}

		private static String formatarModalidade(String modalidade) {
			return switch (modalidade == null ? "" : modalidade) {
				case "PRESENCIAL" -> "Presencial";
				case "REMOTA" -> "Remota";
				default -> modalidade;
			};
		}
	}

	private void render(PdfRenderer renderer, ConsultaResumoPdfData data) throws IOException {
		renderer.section("Paciente");
		renderer.field("Nome do paciente", data.animalNome());
		renderer.field("Espécie", data.especie());
		renderer.field("Raça", data.raca());
		renderer.field("Sexo", data.sexo());
		renderer.field("Castrado", data.castrado());
		renderer.field("Responsável", data.responsavelNome());

		renderer.section("Atendimento");
		renderer.field("Data da consulta", data.dataConsulta() == null ? null : DATA_HORA_FORMATTER.format(data.dataConsulta()));
		renderer.field("Modalidade", data.modalidade());
		renderer.field("Endere\u00e7o", data.endereco());
		renderer.paragraph("Motivo da consulta", data.motivo());

		renderer.section("Veterinário");
		renderer.field("Nome do veterinário", data.veterinarioNome());
		renderer.field("CRMV", data.crmv());
		renderer.field("Clínica", data.clinicaNome());

		renderer.section("Decisão Clínica Final");
		renderer.paragraph("Diagnóstico confirmado pelo veterinário", data.diagnostico());
		renderer.field("Severidade final", data.severidade());
		renderer.paragraph("Conclusão clínica / orientações finais", data.conclusao());

		renderer.prescriptions(data.prescricoes());
	}
}
