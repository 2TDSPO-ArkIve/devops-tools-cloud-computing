package br.com.fiap.arkive.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record FeedbackNpsRequest(
		Long responsavelId,

		Long animalId,

		Long clinicaId,

		Long veterinarioId,

		Long consultaId,

		@NotNull
		@Min(0)
		@Max(5)
		Integer nota,

		String comentario,

		LocalDateTime dataFeedback
) {
}
