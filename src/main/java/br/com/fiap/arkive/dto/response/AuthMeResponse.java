package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.entity.Veterinario;

public record AuthMeResponse(
		Long usuarioId,
		String nome,
		TipoUsuario tipo,
		String login,
		boolean trocaSenhaObrigatoria,
		Long responsavelId,
		Long veterinarioId,
		String crmv,
		String email,
		Long clinicaId
) {
	public static AuthMeResponse fromEntity(Usuario usuario) {
		Long responsavelId = usuario.getResponsavel() == null ? null : usuario.getResponsavel().getId();
		Veterinario veterinario = usuario.getVeterinario();
		Long veterinarioId = veterinario == null ? null : veterinario.getId();
		String crmv = veterinario == null ? null : veterinario.getCrmv();
		String email = veterinario == null ? null : veterinario.getEmail();
		Long clinicaId = clinicaId(usuario, veterinario);

		return new AuthMeResponse(
				usuario.getId(),
				usuario.getNome(),
				usuario.getTipo(),
				usuario.getLogin(),
				"S".equals(usuario.getTrocaSenha()),
				responsavelId,
				veterinarioId,
				crmv,
				email,
				clinicaId
		);
	}

	private static Long clinicaId(Usuario usuario, Veterinario veterinario) {
		if (veterinario != null) {
			return veterinario.getClinica() == null ? null : veterinario.getClinica().getId();
		}
		return usuario.getClinica() == null ? null : usuario.getClinica().getId();
	}
}
