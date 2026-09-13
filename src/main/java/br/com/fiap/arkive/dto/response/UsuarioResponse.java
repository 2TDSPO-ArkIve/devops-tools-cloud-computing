package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.Responsavel;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.entity.Veterinario;

import java.time.LocalDateTime;

public record UsuarioResponse(
		Long id,
		String nome,
		TipoUsuario tipo,
		String login,
		String ativo,
		LocalDateTime dataCadastro,
		Long responsavelId,
		String responsavelNome,
		Long veterinarioId,
		String veterinarioNome,
		Long clinicaId,
		String clinicaNome,
		boolean trocaSenhaObrigatoria,
		LocalDateTime dataUltimaTrocaSenha
) {
	public static UsuarioResponse fromEntity(Usuario usuario) {
		Responsavel responsavel = usuario.getResponsavel();
		Veterinario veterinario = usuario.getVeterinario();
		Clinica clinica = usuario.getClinica();
		return new UsuarioResponse(
				usuario.getId(),
				usuario.getNome(),
				usuario.getTipo(),
				usuario.getLogin(),
				usuario.getAtivo(),
				usuario.getDataCadastro(),
				responsavel == null ? null : responsavel.getId(),
				responsavel == null ? null : responsavel.getNome(),
				veterinario == null ? null : veterinario.getId(),
				veterinario == null ? null : veterinario.getNome(),
				clinica == null ? null : clinica.getId(),
				clinica == null ? null : clinica.getNome(),
				"S".equals(usuario.getTrocaSenha()),
				usuario.getDataUltimaTrocaSenha()
		);
	}
}
