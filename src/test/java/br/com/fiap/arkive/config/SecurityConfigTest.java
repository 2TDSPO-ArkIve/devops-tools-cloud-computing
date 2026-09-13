package br.com.fiap.arkive.config;

import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigTest.TestEndpoints.class)
@ActiveProfiles("local-nodb")
@Import({ SecurityConfig.class, SecurityConfigTest.TestEndpointConfig.class })
class SecurityConfigTest {

	private static final String PRODUCTION_FRONTEND_ORIGIN =
			"https://mobile-application-sooty.vercel.app";

	private final MockMvc mockMvc;
	private final CorsConfigurationSource corsConfigurationSource;

	@Autowired
	SecurityConfigTest(
			MockMvc mockMvc,
			CorsConfigurationSource corsConfigurationSource
	) {
		this.mockMvc = mockMvc;
		this.corsConfigurationSource = corsConfigurationSource;
	}

	@Test
	void anonimoAcessaHealth() throws Exception {
		mockMvc.perform(get("/api/health")).andExpect(status().isOk());
	}

	@Test
	void anonimoNaoAcessaApiProtegida() throws Exception {
		mockMvc.perform(get("/api/protegido"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().doesNotExist("Location"))
				.andExpect(header().string("WWW-Authenticate", "Basic realm=\"ArkIve API\""))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.error").value("Unauthorized"))
				.andExpect(jsonPath("$.message").value("Autenticacao obrigatoria."))
				.andExpect(jsonPath("$.path").value("/api/protegido"));
	}

	@Test
	void anonimoNaoAcessaEscritasGenericasDeConsulta() throws Exception {
		mockMvc.perform(post("/api/consultas")).andExpect(status().isUnauthorized());
		mockMvc.perform(put("/api/consultas/1")).andExpect(status().isUnauthorized());
		mockMvc.perform(delete("/api/consultas/1")).andExpect(status().isUnauthorized());
	}

	@Test
	void corsApiIncluiOrigemDeProducaoSemRemoverDesenvolvimento() {
		MockHttpServletRequest request =
				new MockHttpServletRequest("GET", "/api/consultas");

		CorsConfiguration configuration =
				corsConfigurationSource.getCorsConfiguration(request);

		assertNotNull(configuration);
		assertEquals(
				List.of(PRODUCTION_FRONTEND_ORIGIN),
				configuration.getAllowedOrigins()
		);
		assertEquals(
				List.of("http://localhost:*", "http://127.0.0.1:*"),
				configuration.getAllowedOriginPatterns()
		);
		assertEquals(
				List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"),
				configuration.getAllowedMethods()
		);
		assertEquals(
				List.of("Authorization", "Content-Type", "Accept", "Origin"),
				configuration.getAllowedHeaders()
		);
		assertEquals(Boolean.TRUE, configuration.getAllowCredentials());
	}

	@Test
	void preflightApiAceitaOrigemDeProducaoComAuthorization() throws Exception {
		mockMvc.perform(options("/api/consultas")
						.header("Origin", PRODUCTION_FRONTEND_ORIGIN)
						.header("Access-Control-Request-Method", "GET")
						.header("Access-Control-Request-Headers", "authorization"))
				.andExpect(status().isOk())
				.andExpect(header().string(
						"Access-Control-Allow-Origin",
						PRODUCTION_FRONTEND_ORIGIN
				))
				.andExpect(header().string(
						"Access-Control-Allow-Credentials",
						"true"
				))
				.andExpect(header().string(
						"Access-Control-Allow-Methods",
						containsString("GET")
				))
				.andExpect(header().string(
						"Access-Control-Allow-Methods",
						containsString("POST")
				))
				.andExpect(header().string(
						"Access-Control-Allow-Methods",
						containsString("PUT")
				))
				.andExpect(header().string(
						"Access-Control-Allow-Methods",
						containsString("PATCH")
				))
				.andExpect(header().string(
						"Access-Control-Allow-Methods",
						containsString("DELETE")
				))
				.andExpect(header().string(
						"Access-Control-Allow-Methods",
						containsString("OPTIONS")
				))
				.andExpect(header().string(
						"Access-Control-Allow-Headers",
						containsString("authorization")
				));
	}

	@Test
	void preflightApiNaoAceitaOrigemDesconhecida() throws Exception {
		mockMvc.perform(options("/api/consultas")
						.header("Origin", "https://unknown.example")
						.header("Access-Control-Request-Method", "GET")
						.header("Access-Control-Request-Headers", "authorization"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
	}

	@Test
	void anonimoAcessaSwaggerEOpenApi() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
		mockMvc.perform(get("/swagger-ui.html")).andExpect(status().isOk());
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
	}

	@Test
	void trocaObrigatoriaBloqueiaApiMasNaoSwaggerEOpenApi() throws Exception {
		UsuarioPrincipal principal = new UsuarioPrincipal(
				1L,
				"Ana Sys",
				"ana@arkive.com",
				"$2a$10$hash",
				TipoUsuario.SYSADMIN,
				"S",
				true
		);

		mockMvc.perform(get("/api/protegido").with(user(principal)))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/swagger-ui/index.html").with(user(principal)))
				.andExpect(status().isOk());
		mockMvc.perform(get("/swagger-ui.html").with(user(principal)))
				.andExpect(status().isOk());
		mockMvc.perform(get("/v3/api-docs").with(user(principal)))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser
	void metodoNaoSuportadoEmApiRetornaJson405() throws Exception {
		mockMvc.perform(put("/api/adesoes-prescricao/80"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.status").value(405))
				.andExpect(jsonPath("$.message").value("Metodo HTTP nao suportado para este recurso."))
				.andExpect(jsonPath("$.path").value("/api/adesoes-prescricao/80"));
	}

	@Test
	@WithMockUser(roles = "SYSADMIN")
	void sysadminAcessaSysadminEAdmin() throws Exception {
		mockMvc.perform(get("/sysadmin/painel")).andExpect(status().isOk());
		mockMvc.perform(get("/admin/painel")).andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "ADMIN_CLINICA")
	void adminClinicaAcessaAdminMasNaoSysadmin() throws Exception {
		mockMvc.perform(get("/admin/painel")).andExpect(status().isOk());
		mockMvc.perform(get("/sysadmin/painel")).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "VETERINARIO")
	void veterinarioNaoAcessaAdminOuSysadmin() throws Exception {
		mockMvc.perform(get("/admin/painel")).andExpect(status().isForbidden());
		mockMvc.perform(get("/sysadmin/painel")).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "RESPONSAVEL")
	void responsavelNaoAcessaAdminOuSysadmin() throws Exception {
		mockMvc.perform(get("/admin/painel")).andExpect(status().isForbidden());
		mockMvc.perform(get("/sysadmin/painel")).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "SYSADMIN")
	void sysadminPodeCriarClinica() throws Exception {
		mockMvc.perform(post("/api/clinicas")).andExpect(status().isOk());
	}

	@ParameterizedTest
	@ValueSource(strings = { "ADMIN_CLINICA", "VETERINARIO", "RESPONSAVEL" })
	void somenteSysadminPodeCriarClinica(String role) throws Exception {
		mockMvc.perform(post("/api/clinicas").with(user("user").roles(role)))
				.andExpect(status().isForbidden());
	}

	@Test
	void mutacoesAdministrativasRepresentativasSaoProtegidasPorPerfil() throws Exception {
		mockMvc.perform(post("/api/veterinarios").with(user("admin").roles("ADMIN_CLINICA")))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/especies").with(user("vet").roles("VETERINARIO")))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/protocolos-preventivos").with(user("admin").roles("ADMIN_CLINICA")))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/eventos-jornada").with(user("vet").roles("VETERINARIO")))
				.andExpect(status().isForbidden());
	}

	@Test
	void recursosDeDominioMantemPermissoesEspecificas() throws Exception {
		mockMvc.perform(get("/api/especies").with(user("vet").roles("VETERINARIO")))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/protocolos-preventivos").with(user("responsavel").roles("RESPONSAVEL")))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/avaliacoes-bem-estar").with(user("responsavel").roles("RESPONSAVEL")))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/eventos-preventivos").with(user("vet").roles("VETERINARIO")))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/alertas").with(user("responsavel").roles("RESPONSAVEL")))
				.andExpect(status().isOk());
		mockMvc.perform(patch("/api/alertas/1/ler").with(user("responsavel").roles("RESPONSAVEL")))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/feedbacks-nps").with(user("responsavel").roles("RESPONSAVEL")))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/eventos-jornada/animal/1/timeline").with(user("vet").roles("VETERINARIO")))
				.andExpect(status().isOk());
	}

	@Test
	void fluxosClinicosPrincipaisContinuamAutorizados() throws Exception {
		mockMvc.perform(post("/api/consultas").with(user("vet").roles("VETERINARIO")))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/adesoes-prescricao").with(user("responsavel").roles("RESPONSAVEL")))
				.andExpect(status().isOk());
	}

	@TestConfiguration
	static class TestEndpointConfig {

		@Bean
		TestEndpoints testEndpoints() {
			return new TestEndpoints();
		}

	}

	@RestController
	static class TestEndpoints {

		@GetMapping("/api/health")
		String health() {
			return "ok";
		}

		@GetMapping("/api/protegido")
		String apiProtegida() {
			return "ok";
		}

		@org.springframework.web.bind.annotation.PostMapping("/api/clinicas")
		String criarClinica() {
			return "ok";
		}

		@org.springframework.web.bind.annotation.PostMapping("/api/veterinarios")
		String criarVeterinario() {
			return "ok";
		}

		@GetMapping("/api/especies")
		String listarEspecies() {
			return "ok";
		}

		@org.springframework.web.bind.annotation.PostMapping("/api/especies")
		String criarEspecie() {
			return "ok";
		}

		@GetMapping("/api/protocolos-preventivos")
		String listarProtocolosPreventivos() {
			return "ok";
		}

		@org.springframework.web.bind.annotation.PostMapping("/api/protocolos-preventivos")
		String criarProtocoloPreventivo() {
			return "ok";
		}

		@org.springframework.web.bind.annotation.PostMapping("/api/avaliacoes-bem-estar")
		String criarAvaliacaoBemEstar() {
			return "ok";
		}

		@org.springframework.web.bind.annotation.PostMapping("/api/eventos-preventivos")
		String criarEventoPreventivo() {
			return "ok";
		}

		@GetMapping("/api/alertas")
		String listarAlertas() {
			return "ok";
		}

		@org.springframework.web.bind.annotation.PatchMapping("/api/alertas/{id}/ler")
		String lerAlerta(@org.springframework.web.bind.annotation.PathVariable Long id) {
			return "ok";
		}

		@org.springframework.web.bind.annotation.PostMapping("/api/feedbacks-nps")
		String criarFeedbackNps() {
			return "ok";
		}

		@org.springframework.web.bind.annotation.PostMapping("/api/eventos-jornada")
		String criarEventoJornada() {
			return "ok";
		}

		@GetMapping("/api/eventos-jornada/animal/{animalId}/timeline")
		String timelineAnimal(@org.springframework.web.bind.annotation.PathVariable Long animalId) {
			return "ok";
		}

		@org.springframework.web.bind.annotation.PostMapping("/api/consultas")
		String criarConsulta() {
			return "ok";
		}

		@org.springframework.web.bind.annotation.PostMapping("/api/adesoes-prescricao")
		String registrarAdesao() {
			return "ok";
		}

		@GetMapping("/api/adesoes-prescricao/{id}")
		String buscarAdesao(@org.springframework.web.bind.annotation.PathVariable Long id) {
			return "ok";
		}

		@GetMapping("/swagger-ui/index.html")
		String swaggerUiIndex() {
			return "ok";
		}

		@GetMapping("/swagger-ui.html")
		String swaggerUiHtml() {
			return "ok";
		}

		@GetMapping("/v3/api-docs")
		String apiDocs() {
			return "ok";
		}

		@GetMapping("/sysadmin/painel")
		String sysadmin() {
			return "ok";
		}

		@GetMapping("/admin/painel")
		String admin() {
			return "ok";
		}

	}

}
