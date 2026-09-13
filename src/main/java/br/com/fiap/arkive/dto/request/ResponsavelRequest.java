package br.com.fiap.arkive.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ResponsavelRequest(
		@NotBlank
		@Size(max = 50)
		String nome,

		@NotBlank
		@Size(max = 20)
		String documento,

		@Email
		@Size(max = 200)
		String email,

		@Size(max = 20)
		String telefone,

		@NotBlank
		@Size(max = 30)
		String tipo,

		LocalDate dataCadastro,

		@Size(max = 1)
		String notificacao,

		@Size(max = 1)
		String ativo
) {
}
