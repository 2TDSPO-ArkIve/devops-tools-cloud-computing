package br.com.fiap.arkive.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AvaliacaoBemEstarRequest(
		@NotNull
		Long animalId,

		Long responsavelId,

		Long veterinarioId,

		Long consultaId,

		LocalDateTime dataAvaliacao,

		@Positive
		BigDecimal peso,

		@Positive
		BigDecimal idade,

		@Size(max = 20)
		String apetite,

		@Size(max = 20)
		String atividade,

		@Size(max = 30)
		String comportamento,

		String observacao
) {
}
