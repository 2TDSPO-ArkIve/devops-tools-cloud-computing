package br.com.fiap.arkive.controller.web;

import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.ui.Model;

final class WebModelSupport {

	private WebModelSupport() {
	}

	static void addUserAttributes(Model model, Authentication authentication) {
		model.addAttribute("displayName", displayName(authentication));
		model.addAttribute("roleLabel", roleLabel(authentication));
	}

	static String displayName(Authentication authentication) {
		if (authentication == null) {
			return "";
		}
		Object principal = authentication.getPrincipal();
		if (principal instanceof UsuarioPrincipal usuarioPrincipal) {
			return usuarioPrincipal.getNome();
		}
		return authentication.getName();
	}

	static String roleLabel(Authentication authentication) {
		if (hasRole(authentication, "ROLE_SYSADMIN")) {
			return "SysAdmin";
		}
		if (hasRole(authentication, "ROLE_ADMIN_CLINICA")) {
			return "Administrador da Clínica";
		}
		if (hasRole(authentication, "ROLE_VETERINARIO")) {
			return "Veterinário";
		}
		if (hasRole(authentication, "ROLE_RESPONSAVEL")) {
			return "Responsável";
		}
		return "Usuário ArkIve";
	}

	static String roleLandingPath(Authentication authentication) {
		if (hasRole(authentication, "ROLE_SYSADMIN")) {
			return "/sysadmin/dashboard";
		}
		if (hasRole(authentication, "ROLE_ADMIN_CLINICA")) {
			return "/admin/dashboard";
		}
		return "/acesso-web-restrito";
	}

	static boolean hasRole(Authentication authentication, String authority) {
		if (authentication == null) {
			return false;
		}
		return authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.anyMatch(authority::equals);
	}

}
