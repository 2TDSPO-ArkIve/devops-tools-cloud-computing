package br.com.fiap.arkive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlterarSenhaRequest(
		String senhaAtual,

		@NotBlank
		@Size(min = 8, max = 72)
		String novaSenha,

		@NotBlank
		@Size(min = 8, max = 72)
		String confirmarNovaSenha
) {
}
