package br.com.fiap.arkive.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AnimalHistoricoResponse(Paciente paciente, List<ConsultaHistorico> consultas) {
	public record Paciente(Long id, String nome, String especie, String raca, String sexo,
			String castrado, LocalDate dataNascimento, String responsavelNome) {
	}

	public record ConsultaHistorico(LocalDateTime dataHora, String modalidade, String endereco,
			String motivo, String status, String statusDescricao, String veterinarioNome, String crmv,
			String clinicaNome, String diagnosticoConfirmado, String severidadeFinal, String conclusao,
			List<PrescricaoResumoResponse> prescricoes) {
	}
}
