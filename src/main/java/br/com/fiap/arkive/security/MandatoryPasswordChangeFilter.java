package br.com.fiap.arkive.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class MandatoryPasswordChangeFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			filterChain.doFilter(request, response);
			return;
		}
		Object principal = authentication.getPrincipal();
		if (!(principal instanceof UsuarioPrincipal usuarioPrincipal) || !usuarioPrincipal.isTrocaSenhaObrigatoria()) {
			filterChain.doFilter(request, response);
			return;
		}
		String path = request.getRequestURI().substring(request.getContextPath().length());
		if (isAllowed(path)) {
			filterChain.doFilter(request, response);
			return;
		}
		if (path.startsWith("/api/")) {
			response.setStatus(HttpStatus.FORBIDDEN.value());
			response.setContentType("application/json");
			response.getWriter().write("{\"message\":\"Troca de senha obrigatoria antes de continuar.\"}");
			return;
		}
		response.sendRedirect(request.getContextPath() + "/alterar-senha");
	}

	private boolean isAllowed(String path) {
		return path.equals("/alterar-senha")
				|| path.equals("/logout")
				|| path.equals("/login")
				|| path.equals("/error")
				|| path.equals("/api/auth/me")
				|| path.equals("/api/auth/change-password")
				|| path.startsWith("/css/")
				|| path.startsWith("/js/")
				|| path.startsWith("/images/")
				|| path.startsWith("/swagger-ui/")
				|| path.equals("/swagger-ui.html")
				|| path.equals("/v3/api-docs")
				|| path.startsWith("/v3/api-docs/");
	}
}
