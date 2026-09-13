package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.request.AlterarSenhaApiRequest;
import br.com.fiap.arkive.dto.request.RegistroVeterinarioRequest;
import br.com.fiap.arkive.dto.response.AuthMeResponse;
import br.com.fiap.arkive.dto.response.VeterinarioResponse;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Profile("!local-nodb")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@GetMapping("/me")
	public AuthMeResponse me(@AuthenticationPrincipal UsuarioPrincipal principal) {
		return authService.me(principal);
	}

	@PostMapping("/change-password")
	public ResponseEntity<Void> alterarSenha(
			@Valid @RequestBody AlterarSenhaApiRequest request,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		authService.alterarSenha(principal, request.novaSenha());
		return ResponseEntity.noContent().build();
	}

	/**
	 * The only unauthenticated endpoint on `/api/**` — see SecurityConfig's
	 * matcher for this exact method+path. No `@AuthenticationPrincipal` here
	 * on purpose: this is how an unauthenticated veterinarian gets an account
	 * in the first place.
	 */
	@PostMapping("/register")
	public ResponseEntity<VeterinarioResponse> registrar(@Valid @RequestBody RegistroVeterinarioRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrarVeterinario(request));
	}
}
