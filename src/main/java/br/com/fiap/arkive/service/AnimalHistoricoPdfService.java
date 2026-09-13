package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.response.AnimalHistoricoResponse;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
@Profile("!local-nodb")
public class AnimalHistoricoPdfService {
	private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
	private final AnimalHistoricoService historicoService;

	public AnimalHistoricoPdfService(AnimalHistoricoService historicoService) {
		this.historicoService = historicoService;
	}

	public HistoricoPdf gerar(Long animalId, UsuarioPrincipal principal) {
		AnimalHistoricoResponse historico = historicoService.buscar(animalId, principal);
		try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			try (PdfRenderer renderer = new PdfRenderer(document, "HIST\u00d3RICO CL\u00cdNICO DO PACIENTE")) {
				render(renderer, historico);
			}
			document.save(output);
			return new HistoricoPdf(output.toByteArray(), "arkive-historico-" + animalId + ".pdf");
		} catch (IOException ex) {
			throw new BusinessException("Nao foi possivel gerar o historico em PDF.", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private void render(PdfRenderer pdf, AnimalHistoricoResponse historico) throws IOException {
		var paciente = historico.paciente();
		pdf.section("Paciente");
		pdf.field("Nome do paciente", paciente.nome());
		pdf.field("Esp\u00e9cie", paciente.especie());
		pdf.field("Ra\u00e7a", paciente.raca());
		pdf.field("Sexo", "M".equals(paciente.sexo()) ? "Macho" : "F".equals(paciente.sexo()) ? "F\u00eamea" : null);
		pdf.field("Castrado", "S".equals(paciente.castrado()) ? "Sim" : "N".equals(paciente.castrado()) ? "N\u00e3o" : null);
		pdf.field("Data de nascimento", paciente.dataNascimento() == null ? null : DATA.format(paciente.dataNascimento()));
		pdf.field("Respons\u00e1vel atual", paciente.responsavelNome());
		pdf.section("Consultas (mais recentes primeiro)");
		if (historico.consultas().isEmpty()) {
			pdf.paragraph("Hist\u00f3rico", "Nenhuma consulta dispon\u00edvel no hist\u00f3rico deste paciente.");
		}
		for (var consulta : historico.consultas()) {
			pdf.section("Consulta - " + DATA_HORA.format(consulta.dataHora()));
			pdf.field("Situa\u00e7\u00e3o", consulta.statusDescricao());
			pdf.field("Modalidade", "PRESENCIAL".equals(consulta.modalidade()) ? "Presencial" : "Remota");
			pdf.field("Endere\u00e7o", consulta.endereco());
			pdf.paragraph("Motivo da consulta", consulta.motivo());
			pdf.field("Veterin\u00e1rio", consulta.veterinarioNome());
			pdf.field("CRMV", consulta.crmv());
			pdf.field("Cl\u00ednica", consulta.clinicaNome());
			pdf.paragraph("Diagn\u00f3stico confirmado pelo veterin\u00e1rio", consulta.diagnosticoConfirmado());
			pdf.field("Severidade final", consulta.severidadeFinal());
			pdf.paragraph("Conclus\u00e3o cl\u00ednica / orienta\u00e7\u00f5es finais", consulta.conclusao());
			pdf.prescriptions(consulta.prescricoes());
		}
	}

	public record HistoricoPdf(byte[] bytes, String filename) {
	}
}
