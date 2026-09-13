package br.com.fiap.arkive.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Prescricao criada pelo veterinario responsavel por uma consulta finalizada.")
public record PrescricaoRequest(
		@NotBlank
		@Size(max = 150)
		@Schema(description = "Nome do medicamento definido pelo veterinario.", example = "Amoxicilina com clavulanato")
		String medicamento,

		@NotBlank
		@Size(max = 50)
		@Schema(description = "Dosagem prescrita pelo veterinario.", example = "1 comprimido")
		String dosagem,

		@Size(max = 100)
		@Schema(description = "Frequencia de administracao.", example = "12/12h")
		String frequencia,

		@Size(max = 50)
		@Schema(description = "Via de administracao. Valores aceitos: ORAL, INJETAVEL, TOPICO, OCULAR, OTOLOGICO ou OUTRO.", example = "ORAL")
		String viaAdministracao,

		@NotNull
		@Schema(description = "Data de inicio do tratamento.", example = "2026-08-29")
		LocalDate dataInicio,

		@Schema(description = "Data final do tratamento, quando houver.", example = "2026-09-05")
		LocalDate dataFim,

		@Schema(description = "Instrucoes adicionais para o responsavel.", example = "Administrar com alimento.")
		String instrucoes,

		@NotNull
		@Schema(description = "Consulta finalizada associada a prescricao. Este vinculo nao pode ser alterado depois da criacao.", example = "42")
		Long consultaId
) {
}
