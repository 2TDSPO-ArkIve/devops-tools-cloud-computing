package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.domain.consulta.StatusConsulta;
import br.com.fiap.arkive.entity.Consulta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConsultaWorkflowResponse(
		Long id,
		LocalDateTime dataHora,
		String modalidade,
		String motivo,
		String sintomas,
		String observacao,
		BigDecimal peso,
		String transcricao,
		String status,
		String statusDescricao,
		Long animalId,
		String animalNome,
		Long veterinarioId,
		String veterinarioNome,
		Long clinicaId,
		String clinicaNome,
		Long diagnosticoId
) {
	public static ConsultaWorkflowResponse fromEntity(Consulta consulta) {
		return fromEntity(consulta, null);
	}

	public static ConsultaWorkflowResponse fromEntity(Consulta consulta, Long diagnosticoId) {
		Long clinicaId = consulta.getClinica() == null ? null : consulta.getClinica().getId();
		String clinicaNome = consulta.getClinica() == null ? null : consulta.getClinica().getNome();
		StatusConsulta statusConsulta = StatusConsulta.fromCodigo(consulta.getStatus());
		return new ConsultaWorkflowResponse(
				consulta.getId(),
				consulta.getDataHora(),
				consulta.getModalidade(),
				consulta.getMotivo(),
				consulta.getSintomas(),
				consulta.getObservacao(),
				consulta.getPeso(),
				consulta.getTranscricao(),
				consulta.getStatus(),
				statusConsulta.getDescricao(),
				consulta.getAnimal().getId(),
				consulta.getAnimal().getNome(),
				consulta.getVeterinario().getId(),
				consulta.getVeterinario().getNome(),
				clinicaId,
				clinicaNome,
				diagnosticoId
		);
	}
}
