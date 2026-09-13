package br.com.fiap.arkive.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Conclusao veterinaria usada para finalizar uma consulta.")
public record FinalizarConsultaRequest(
		@NotBlank
		@Schema(description = "Diagnostico confirmado pelo veterinario.", example = "Otite externa bacteriana")
		String diagnostico,

		@Size(max = 20)
		@Schema(description = "Severidade da conclusao veterinaria. Valores aceitos: LEVE, MODERADA ou GRAVE.", example = "MODERADA")
		String severidade,

		@Schema(description = "Doenca catalogada associada, quando aplicavel.", example = "7")
		Long doencaId,

		@Schema(description = "Conclusao clinica livre registrada na consulta.", example = "Conduta inicial definida e retorno recomendado em sete dias.")
		String conclusao
) {
}
