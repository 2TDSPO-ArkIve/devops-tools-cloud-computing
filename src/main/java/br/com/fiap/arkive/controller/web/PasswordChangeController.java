package br.com.fiap.arkive.controller.web;

import br.com.fiap.arkive.dto.request.AlterarSenhaRequest;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.PasswordLifecycleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PasswordChangeController {

	private final ObjectProvider<PasswordLifecycleService> passwordLifecycleService;

	public PasswordChangeController(ObjectProvider<PasswordLifecycleService> passwordLifecycleService) {
		this.passwordLifecycleService = passwordLifecycleService;
	}

	@GetMapping("/alterar-senha")
	public String form(Model model, Authentication authentication) {
		WebModelSupport.addUserAttributes(model, authentication);
		model.addAttribute("pageTitle", isForced(authentication) ? "Primeiro acesso" : "Alterar senha");
		model.addAttribute("forced", isForced(authentication));
		if (!model.containsAttribute("senha")) {
			model.addAttribute("senha", new AlterarSenhaRequest("", "", ""));
		}
		return "auth/alterar-senha";
	}

	@PostMapping("/alterar-senha")
	public String alterar(
			@Valid @ModelAttribute("senha") AlterarSenhaRequest request,
			BindingResult bindingResult,
			Authentication authentication,
			Model model,
			RedirectAttributes redirectAttributes
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		boolean forced = isForced(authentication);
		model.addAttribute("pageTitle", forced ? "Primeiro acesso" : "Alterar senha");
		model.addAttribute("forced", forced);
		if (bindingResult.hasErrors()) {
			return "auth/alterar-senha";
		}
		try {
			UsuarioPrincipal principal = principal(authentication);
			Usuario usuario = forced
					? passwordLifecycleService.getObject().alterarSenhaObrigatoria(principal.getUsuarioId(), request.novaSenha(), request.confirmarNovaSenha())
					: passwordLifecycleService.getObject().alterarSenhaAutenticado(principal.getUsuarioId(), request.senhaAtual(), request.novaSenha(), request.confirmarNovaSenha());
			atualizarPrincipal(authentication, usuario);
		} catch (BusinessException | ResourceNotFoundException ex) {
			bindingResult.reject("senha.invalida", ex.getMessage());
			return "auth/alterar-senha";
		}
		redirectAttributes.addFlashAttribute("sucesso", "Senha alterada com sucesso.");
		return "redirect:" + WebModelSupport.roleLandingPath(SecurityContextHolder.getContext().getAuthentication());
	}

	private boolean isForced(Authentication authentication) {
		return principal(authentication).isTrocaSenhaObrigatoria();
	}

	private UsuarioPrincipal principal(Authentication authentication) {
		if (authentication != null && authentication.getPrincipal() instanceof UsuarioPrincipal usuarioPrincipal) {
			return usuarioPrincipal;
		}
		throw new BusinessException("Usuario autenticado invalido.");
	}

	private void atualizarPrincipal(Authentication authentication, Usuario usuario) {
		UsuarioPrincipal novoPrincipal = UsuarioPrincipal.fromEntity(usuario);
		UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
				novoPrincipal,
				novoPrincipal.getPassword(),
				novoPrincipal.getAuthorities()
		);
		token.setDetails(authentication.getDetails());
		SecurityContextHolder.getContext().setAuthentication(token);
	}
}
