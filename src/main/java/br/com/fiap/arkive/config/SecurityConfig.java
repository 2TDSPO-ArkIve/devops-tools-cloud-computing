package br.com.fiap.arkive.config;

import br.com.fiap.arkive.security.ApiAuthenticationEntryPoint;
import br.com.fiap.arkive.security.ArkiveUserDetailsService;
import br.com.fiap.arkive.security.MandatoryPasswordChangeFilter;
import br.com.fiap.arkive.security.PasswordChangeAuthenticationSuccessHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

	private static final RequestMatcher API_REQUESTS =
			PathPatternRequestMatcher.withDefaults().matcher("/api/**");

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public ApiAuthenticationEntryPoint apiAuthenticationEntryPoint(ObjectMapper objectMapper) {
		return new ApiAuthenticationEntryPoint(objectMapper);
	}

	@Bean
	@Profile("!local-nodb")
	public DaoAuthenticationProvider authenticationProvider(
			ArkiveUserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder
	) {
		DaoAuthenticationProvider provider =
				new DaoAuthenticationProvider((UserDetailsService) userDetailsService);

		provider.setPasswordEncoder(passwordEncoder);
		return provider;
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();

		configuration.setAllowedOrigins(List.of(
				"https://mobile-application-sooty.vercel.app"
		));

		// Expo Web / navegador durante desenvolvimento local.
		// O wildcard vale apenas para a porta.
		configuration.setAllowedOriginPatterns(List.of(
				"http://localhost:*",
				"http://127.0.0.1:*"
		));

		configuration.setAllowedMethods(List.of(
				"GET",
				"POST",
				"PUT",
				"PATCH",
				"DELETE",
				"OPTIONS"
		));

		configuration.setAllowedHeaders(List.of(
				"Authorization",
				"Content-Type",
				"Accept",
				"Origin"
		));

		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source =
				new UrlBasedCorsConfigurationSource();

		source.registerCorsConfiguration("/api/**", configuration);

		return source;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			ApiAuthenticationEntryPoint apiAuthenticationEntryPoint,
			CorsConfigurationSource corsConfigurationSource
	) throws Exception {

		return http
				.cors(cors -> cors.configurationSource(corsConfigurationSource))

				.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))

				.exceptionHandling(exceptionHandling -> exceptionHandling
						.defaultAuthenticationEntryPointFor(
								apiAuthenticationEntryPoint,
								API_REQUESTS
						)
						.defaultAuthenticationEntryPointFor(
								new LoginUrlAuthenticationEntryPoint("/login"),
								AnyRequestMatcher.INSTANCE
						)
				)

				.authorizeHttpRequests(authorize -> authorize

						// O preflight CORS do navegador não envia Basic Auth.
						.requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()

						.requestMatchers(
								"/api/health",
								"/swagger-ui/**",
								"/swagger-ui.html",
								"/v3/api-docs/**",
								"/css/**",
								"/js/**",
								"/images/**",
								"/login"
						).permitAll()

						// The one deliberate public exception on /api/**: a veterinarian
						// self-registration endpoint. Method-specific on purpose — this
						// must never accidentally open GET/PUT/DELETE on /api/auth/register
						// (there are none today, but a future addition to this controller
						// must not silently inherit this permitAll).
						.requestMatchers(HttpMethod.POST, "/api/auth/register")
						.permitAll()

						.requestMatchers("/sysadmin/**")
						.hasRole("SYSADMIN")

						.requestMatchers("/admin/**")
						.hasAnyRole("SYSADMIN", "ADMIN_CLINICA")

						.requestMatchers("/api/clinicas", "/api/clinicas/**")
						.hasRole("SYSADMIN")
						.requestMatchers("/api/veterinarios", "/api/veterinarios/**")
						.hasRole("SYSADMIN")

						.requestMatchers(HttpMethod.GET, "/api/especies", "/api/especies/**",
								"/api/protocolos-preventivos", "/api/protocolos-preventivos/**")
						.hasAnyRole("SYSADMIN", "ADMIN_CLINICA", "VETERINARIO", "RESPONSAVEL")
						.requestMatchers("/api/especies", "/api/especies/**",
								"/api/protocolos-preventivos", "/api/protocolos-preventivos/**")
						.hasRole("SYSADMIN")

						.requestMatchers(HttpMethod.GET, "/api/avaliacoes-bem-estar", "/api/avaliacoes-bem-estar/**")
						.hasAnyRole("SYSADMIN", "ADMIN_CLINICA", "VETERINARIO", "RESPONSAVEL")
						.requestMatchers(HttpMethod.POST, "/api/avaliacoes-bem-estar")
						.hasAnyRole("SYSADMIN", "VETERINARIO", "RESPONSAVEL")
						.requestMatchers("/api/avaliacoes-bem-estar", "/api/avaliacoes-bem-estar/**")
						.hasRole("SYSADMIN")

						.requestMatchers(HttpMethod.GET, "/api/eventos-preventivos", "/api/eventos-preventivos/**")
						.hasAnyRole("SYSADMIN", "ADMIN_CLINICA", "VETERINARIO", "RESPONSAVEL")
						.requestMatchers(HttpMethod.POST, "/api/eventos-preventivos")
						.hasAnyRole("SYSADMIN", "VETERINARIO")
						.requestMatchers(HttpMethod.PUT, "/api/eventos-preventivos/**")
						.hasAnyRole("SYSADMIN", "VETERINARIO")
						.requestMatchers("/api/eventos-preventivos", "/api/eventos-preventivos/**")
						.hasRole("SYSADMIN")

						.requestMatchers(HttpMethod.GET, "/api/alertas", "/api/alertas/**")
						.hasAnyRole("SYSADMIN", "ADMIN_CLINICA", "VETERINARIO", "RESPONSAVEL")
						.requestMatchers(HttpMethod.POST, "/api/alertas")
						.hasAnyRole("SYSADMIN", "ADMIN_CLINICA", "VETERINARIO")
						.requestMatchers(HttpMethod.PATCH, "/api/alertas/*/ler")
						.hasAnyRole("SYSADMIN", "ADMIN_CLINICA", "RESPONSAVEL")
						.requestMatchers(HttpMethod.PUT, "/api/alertas/**")
						.hasAnyRole("SYSADMIN", "ADMIN_CLINICA")
						.requestMatchers("/api/alertas", "/api/alertas/**")
						.hasRole("SYSADMIN")

						.requestMatchers(HttpMethod.GET, "/api/feedbacks-nps", "/api/feedbacks-nps/**")
						.hasAnyRole("SYSADMIN", "ADMIN_CLINICA", "VETERINARIO", "RESPONSAVEL")
						.requestMatchers(HttpMethod.POST, "/api/feedbacks-nps")
						.hasAnyRole("SYSADMIN", "RESPONSAVEL")
						.requestMatchers("/api/feedbacks-nps", "/api/feedbacks-nps/**")
						.hasRole("SYSADMIN")

						.requestMatchers(HttpMethod.GET, "/api/eventos-jornada/animal/*/timeline")
						.hasAnyRole("SYSADMIN", "ADMIN_CLINICA", "VETERINARIO", "RESPONSAVEL")
						.requestMatchers(HttpMethod.GET, "/api/eventos-jornada", "/api/eventos-jornada/**")
						.hasAnyRole("SYSADMIN", "ADMIN_CLINICA")
						.requestMatchers("/api/eventos-jornada", "/api/eventos-jornada/**")
						.hasRole("SYSADMIN")

						.requestMatchers(HttpMethod.POST, "/api/racas")
						.hasAnyRole("SYSADMIN", "ADMIN_CLINICA", "VETERINARIO")
						.requestMatchers(HttpMethod.PUT, "/api/racas/**")
						.hasAnyRole("SYSADMIN", "ADMIN_CLINICA")
						.requestMatchers(HttpMethod.DELETE, "/api/racas/**")
						.hasAnyRole("SYSADMIN", "ADMIN_CLINICA")
						.requestMatchers(HttpMethod.GET, "/api/responsaveis/busca")
						.hasAnyRole("SYSADMIN", "ADMIN_CLINICA", "VETERINARIO")
						.requestMatchers(HttpMethod.GET, "/api/responsaveis", "/api/responsaveis/**")
						.hasAnyRole("SYSADMIN", "ADMIN_CLINICA", "RESPONSAVEL")
						.requestMatchers("/api/responsaveis", "/api/responsaveis/**")
						.hasAnyRole("SYSADMIN", "ADMIN_CLINICA")

						.requestMatchers("/api/**")
						.authenticated()

						.anyRequest()
						.authenticated()
				)

				.formLogin(formLogin -> formLogin
						.loginPage("/login")
						.loginProcessingUrl("/login")
						.successHandler(
								new PasswordChangeAuthenticationSuccessHandler()
						)
						.failureUrl("/login?error")
						.permitAll()
				)

				.logout(logout -> logout
						.logoutSuccessUrl("/login?logout")
						.permitAll()
				)

				.httpBasic(httpBasic ->
						httpBasic.authenticationEntryPoint(
								apiAuthenticationEntryPoint
						)
				)

				.addFilterAfter(
						new MandatoryPasswordChangeFilter(),
						AuthorizationFilter.class
				)

				.build();
	}
}
