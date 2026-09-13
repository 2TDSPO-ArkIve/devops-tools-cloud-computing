package br.com.fiap.arkive.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Registro publico de adesao terapeutica. Responsavel, animal e data do registro sao definidos pelo servidor.")
public record RegistrarAdesaoPrescricaoRequest(
		@NotNull
		@Schema(description = "Prescricao sendo acompanhada.", example = "90")
		Long prescricaoId,

		@NotBlank
		@Size(max = 1)
		@Schema(description = "Indica se o tratamento/dose foi seguido. Valores aceitos: S ou N.", example = "S")
		String tomou,

		@Schema(description = "Observacao livre do responsavel.", example = "Administrado sem resistencia.")
		String observacao
) {
}
