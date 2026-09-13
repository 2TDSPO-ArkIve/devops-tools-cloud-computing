package br.com.fiap.arkive.dto.response;

public record PasswordResetResult(
		Long usuarioId,
		String senhaTemporaria
) {
}
