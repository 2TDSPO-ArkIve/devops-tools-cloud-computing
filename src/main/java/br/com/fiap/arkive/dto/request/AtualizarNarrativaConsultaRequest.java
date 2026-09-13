package br.com.fiap.arkive.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Narrativa clinica registrada pelo veterinario durante a consulta.")
public record AtualizarNarrativaConsultaRequest(
		@NotBlank
		@Schema(description = "Texto narrativo da consulta, sintomas observados e evolucao clinica.", example = "Paciente com prurido auricular e secrecao moderada ha tres dias.")
		String narrativa
) {
}
