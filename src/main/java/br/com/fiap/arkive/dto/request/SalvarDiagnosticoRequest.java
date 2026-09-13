package br.com.fiap.arkive.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Diagnostico informado por cliente autorizado. Campos de IA, confianca, confirmacao e validacao veterinaria sao controlados pelo servidor.")
public record SalvarDiagnosticoRequest(
		@NotBlank
		@Schema(description = "Texto do diagnostico ou conclusao clinica.", example = "Otite externa")
		String diagnostico,

		@Size(max = 20)
		@Schema(description = "Severidade sugerida pelo veterinario. Valores aceitos: LEVE, MODERADA ou GRAVE.", example = "MODERADA")
		String severidade,

		@NotNull
		@Schema(description = "Consulta associada ao diagnostico.", example = "42")
		Long consultaId,

		@Schema(description = "Doenca catalogada associada, quando aplicavel.", example = "7")
		Long doencaId
) {

	public DiagnosticoRequest toDiagnosticoRequest() {
		return new DiagnosticoRequest(diagnostico, severidade, null, null, null, null, consultaId, doencaId);
	}

}
