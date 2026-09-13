package br.com.fiap.arkive.dto.request;

import br.com.fiap.arkive.entity.TipoUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
		@NotBlank
		@Size(max = 150)
		String nome,

		@NotNull
		TipoUsuario tipo,

		@NotBlank
		@Email
		@Size(max = 200)
		String login,

		@NotBlank
		@Size(min = 8, max = 72)
		String senha,

		Long responsavelId,

		Long veterinarioId,

		Long clinicaId,

		@Size(max = 1)
		String ativo
) {
}
