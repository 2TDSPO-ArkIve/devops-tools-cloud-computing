package br.com.fiap.arkive.dto.request;

import br.com.fiap.arkive.entity.TipoUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioProvisioningRequest(
		@NotBlank
		@Size(max = 150)
		String nome,

		@NotNull
		TipoUsuario tipo,

		@NotBlank
		@Email
		@Size(max = 72)
		String login,

		Long responsavelId,

		Long veterinarioId,

		Long clinicaId
) {
}
