package br.com.fiap.arkive.service;

import br.com.fiap.arkive.domain.consulta.StatusConsulta;
import br.com.fiap.arkive.dto.response.AnimalHistoricoResponse;
import br.com.fiap.arkive.dto.response.AnimalHistoricoResponse.ConsultaHistorico;
import br.com.fiap.arkive.dto.response.AnimalHistoricoResponse.Paciente;
import br.com.fiap.arkive.dto.response.PrescricaoResumoResponse;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Diagnostico;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.AnimalRepository;
import br.com.fiap.arkive.repository.AnimalResponsavelRepository;
import br.com.fiap.arkive.repository.ConsultaRepository;
import br.com.fiap.arkive.repository.DiagnosticoRepository;
import br.com.fiap.arkive.repository.PrescricaoRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Profile("!local-nodb")
public class AnimalHistoricoService {
	private final AnimalRepository animais;
	private final ConsultaRepository consultas;
	private final DiagnosticoRepository diagnosticos;
	private final PrescricaoRepository prescricoes;
	private final AnimalResponsavelRepository responsaveis;
	private final ClinicalAccessService acesso;

	public AnimalHistoricoService(AnimalRepository animais, ConsultaRepository consultas,
			DiagnosticoRepository diagnosticos, PrescricaoRepository prescricoes,
			AnimalResponsavelRepository responsaveis, ClinicalAccessService acesso) {
		this.animais = animais;
		this.consultas = consultas;
		this.diagnosticos = diagnosticos;
		this.prescricoes = prescricoes;
		this.responsaveis = responsaveis;
		this.acesso = acesso;
	}

	@Transactional(readOnly = true)
	public AnimalHistoricoResponse buscar(Long animalId, UsuarioPrincipal principal) {
		var animal = animais.findById(animalId)
				.orElseThrow(() -> new ResourceNotFoundException("Animal nao encontrado."));
		acesso.exigirLeituraAnimal(principal, animal);
		var vinculos = responsaveis.buscarResponsaveisPrincipaisAtivosVigentes(animalId, LocalDate.now());
		String responsavel = vinculos.size() == 1 && vinculos.get(0).getResponsavel() != null
				? vinculos.get(0).getResponsavel().getNome() : null;
		var paciente = new Paciente(animal.getId(), animal.getNome(),
				animal.getEspecie() == null ? null : animal.getEspecie().getNome(),
				animal.getRaca() == null ? null : animal.getRaca().getNome(), animal.getSexo(),
				animal.getCastrado(), animal.getDataNascimento(), responsavel);
		// Animal access can be broader than consultation access (notably for clinic veterinarians).
		var historico = consultas.buscarHistoricoPorAnimal(animalId).stream()
				.filter(consulta -> acesso.podeLerConsulta(principal, consulta))
				.map(this::resumir).toList();
		return new AnimalHistoricoResponse(paciente, historico);
	}

	private ConsultaHistorico resumir(Consulta consulta) {
		boolean finalizada = StatusConsulta.FI.getCodigo().equals(consulta.getStatus());
		Diagnostico diagnostico = finalizada
				? diagnosticos.buscarDiagnosticosConfirmadosVeterinario(consulta.getId(), PageRequest.of(0, 1))
						.stream().findFirst().orElse(null) : null;
		var medicamentos = finalizada ? prescricoes.buscarPorConsulta(consulta.getId()).stream()
				.map(PrescricaoResumoResponse::fromEntity).toList() : List.<PrescricaoResumoResponse>of();
		String endereco = consulta.getEndereco();
		return new ConsultaHistorico(consulta.getDataHora(), consulta.getModalidade(),
				"PRESENCIAL".equals(consulta.getModalidade()) && endereco != null && !endereco.isBlank()
						? endereco.trim() : null,
				consulta.getMotivo(), consulta.getStatus(), StatusConsulta.fromCodigo(consulta.getStatus()).getDescricao(),
				consulta.getVeterinario().getNome(), consulta.getVeterinario().getCrmv(),
				consulta.getClinica() == null ? null : consulta.getClinica().getNome(),
				diagnostico == null ? null : diagnostico.getDiagnostico(),
				diagnostico == null ? null : diagnostico.getSeveridade(),
				finalizada ? consulta.getObservacao() : null, medicamentos);
	}
}
