package br.com.fiap.arkive.security;

import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UsuarioPrincipal implements UserDetails {

	private final Long usuarioId;
	private final String nome;
	private final String login;
	private final String senhaHash;
	private final TipoUsuario tipoUsuario;
	private final String ativo;
	private final boolean trocaSenhaObrigatoria;
	private final Long responsavelId;
	private final Long veterinarioId;
	private final Long clinicaId;

	public UsuarioPrincipal(Long usuarioId, String nome, String login, String senhaHash, TipoUsuario tipoUsuario, String ativo) {
		this(usuarioId, nome, login, senhaHash, tipoUsuario, ativo, false);
	}

	public UsuarioPrincipal(Long usuarioId, String nome, String login, String senhaHash, TipoUsuario tipoUsuario, String ativo, boolean trocaSenhaObrigatoria) {
		this(usuarioId, nome, login, senhaHash, tipoUsuario, ativo, trocaSenhaObrigatoria, null, null, null);
	}

	public UsuarioPrincipal(
			Long usuarioId,
			String nome,
			String login,
			String senhaHash,
			TipoUsuario tipoUsuario,
			String ativo,
			boolean trocaSenhaObrigatoria,
			Long responsavelId,
			Long veterinarioId,
			Long clinicaId
	) {
		this.usuarioId = usuarioId;
		this.nome = nome;
		this.login = login;
		this.senhaHash = senhaHash;
		this.tipoUsuario = tipoUsuario;
		this.ativo = ativo;
		this.trocaSenhaObrigatoria = trocaSenhaObrigatoria;
		this.responsavelId = responsavelId;
		this.veterinarioId = veterinarioId;
		this.clinicaId = clinicaId;
	}

	public static UsuarioPrincipal fromEntity(Usuario usuario) {
		Long responsavelId = usuario.getResponsavel() == null ? null : usuario.getResponsavel().getId();
		Long veterinarioId = usuario.getVeterinario() == null ? null : usuario.getVeterinario().getId();
		Long clinicaId = usuario.getClinica() == null ? null : usuario.getClinica().getId();
		return new UsuarioPrincipal(
				usuario.getId(),
				usuario.getNome(),
				usuario.getLogin(),
				usuario.getSenhaHash(),
				usuario.getTipo(),
				usuario.getAtivo(),
				"S".equals(usuario.getTrocaSenha()),
				responsavelId,
				veterinarioId,
				clinicaId
		);
	}

	public Long getUsuarioId() {
		return usuarioId;
	}

	public String getNome() {
		return nome;
	}

	public TipoUsuario getTipoUsuario() {
		return tipoUsuario;
	}

	public boolean isTrocaSenhaObrigatoria() {
		return trocaSenhaObrigatoria;
	}

	public Long getResponsavelId() {
		return responsavelId;
	}

	public Long getVeterinarioId() {
		return veterinarioId;
	}

	public Long getClinicaId() {
		return clinicaId;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + tipoUsuario.name()));
	}

	@Override
	public String getPassword() {
		return senhaHash;
	}

	@Override
	public String getUsername() {
		return login;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return "S".equals(ativo);
	}

}
