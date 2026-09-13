package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.RegistroVeterinarioRequest;
import br.com.fiap.arkive.dto.request.VeterinarioRequest;
import br.com.fiap.arkive.dto.response.AuthMeResponse;
import br.com.fiap.arkive.dto.response.VeterinarioResponse;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.UsuarioRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!local-nodb")
public class AuthService {

	private final UsuarioRepository usuarioRepository;
	private final PasswordLifecycleService passwordLifecycleService;
	private final VeterinarioService veterinarioService;

	public AuthService(
			UsuarioRepository usuarioRepository,
			PasswordLifecycleService passwordLifecycleService,
			VeterinarioService veterinarioService
	) {
		this.usuarioRepository = usuarioRepository;
		this.passwordLifecycleService = passwordLifecycleService;
		this.veterinarioService = veterinarioService;
	}

	@Transactional(readOnly = true)
	public AuthMeResponse me(UsuarioPrincipal principal) {
		return AuthMeResponse.fromEntity(buscarUsuario(principal));
	}

	@Transactional
	public void alterarSenha(UsuarioPrincipal principal, String novaSenha) {
		passwordLifecycleService.alterarSenhaObrigatoria(usuarioId(principal), novaSenha, novaSenha);
	}

	/**
	 * Public self-registration entry point (`POST /api/auth/register`, the
	 * only unauthenticated exception carved into `/api/**` for this feature —
	 * see SecurityConfig). The caller can never choose a role or a clinic:
	 * this delegates to the exact same transactional
	 * `VeterinarioService.criar` path the SysAdmin veterinarian-management
	 * screen already uses, which always assigns `TipoUsuario.VETERINARIO`
	 * server-side and provisions the linked Usuario account (initial
	 * password = the e-mail itself, BCrypt-hashed, `trocaSenha=S`) in one
	 * transaction. The veterinarian must change that temporary password on
	 * first login via the app's existing mandatory-password-change flow —
	 * this method never accepts or stores a client-chosen password.
	 */
	@Transactional
	public VeterinarioResponse registrarVeterinario(RegistroVeterinarioRequest request) {
		return veterinarioService.criar(new VeterinarioRequest(
				request.nome(),
				request.crmv(),
				null,
				request.email(),
				null,
				null
		));
	}

	private Usuario buscarUsuario(UsuarioPrincipal principal) {
		return usuarioRepository.findById(usuarioId(principal))
				.orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado."));
	}

	private Long usuarioId(UsuarioPrincipal principal) {
		if (principal == null || principal.getUsuarioId() == null) {
			throw new BusinessException("Usuario autenticado invalido.");
		}
		return principal.getUsuarioId();
	}
}
