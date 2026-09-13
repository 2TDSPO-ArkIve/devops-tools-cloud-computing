package br.com.fiap.arkive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RacaRequest(
		@NotBlank
		@Size(max = 50)
		String nome,

		@NotNull
		Long especieId,

		@Size(max = 20)
		String porte
) {
}
