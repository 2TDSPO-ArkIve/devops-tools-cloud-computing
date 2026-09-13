package br.com.fiap.arkive.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PasswordChangeAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
		Object principal = authentication.getPrincipal();
		if (principal instanceof UsuarioPrincipal usuarioPrincipal && usuarioPrincipal.isTrocaSenhaObrigatoria()) {
			response.sendRedirect(request.getContextPath() + "/alterar-senha");
			return;
		}
		response.sendRedirect(request.getContextPath() + "/");
	}
}
