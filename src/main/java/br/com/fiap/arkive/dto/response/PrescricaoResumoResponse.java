package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.Prescricao;
import java.time.LocalDate;

public record PrescricaoResumoResponse(
		String medicamento, String dosagem, String frequencia, String viaAdministracao,
		LocalDate dataInicio, LocalDate dataFim, String instrucoes
) {
	public static PrescricaoResumoResponse fromEntity(Prescricao prescricao) {
		return new PrescricaoResumoResponse(prescricao.getMedicamento(), prescricao.getDosagem(),
				prescricao.getFrequencia(), prescricao.getViaAdministracao(), prescricao.getDataInicio(),
				prescricao.getDataFim(), prescricao.getInstrucoes());
	}
}
