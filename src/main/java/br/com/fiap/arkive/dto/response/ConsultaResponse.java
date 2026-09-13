package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.domain.consulta.StatusConsulta;
import br.com.fiap.arkive.entity.Consulta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConsultaResponse(
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
		String endereco
) {
	public ConsultaResponse(Long id, LocalDateTime dataHora, String modalidade, String motivo,
			String sintomas, String observacao, BigDecimal peso, String transcricao, String status,
			String statusDescricao, Long animalId, String animalNome, Long veterinarioId,
			String veterinarioNome, Long clinicaId, String clinicaNome) {
		this(id, dataHora, modalidade, motivo, sintomas, observacao, peso, transcricao, status,
				statusDescricao, animalId, animalNome, veterinarioId, veterinarioNome, clinicaId, clinicaNome, null);
	}
	public static ConsultaResponse fromEntity(Consulta consulta) {
		Long clinicaId = consulta.getClinica() == null ? null : consulta.getClinica().getId();
		String clinicaNome = consulta.getClinica() == null ? null : consulta.getClinica().getNome();
		StatusConsulta statusConsulta = StatusConsulta.fromCodigo(consulta.getStatus());
		return new ConsultaResponse(
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
				consulta.getEndereco()
		);
	}
}
