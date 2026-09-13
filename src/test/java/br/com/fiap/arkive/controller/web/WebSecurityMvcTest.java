package br.com.fiap.arkive.controller.web;

import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.PasswordLifecycleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local-nodb")
class WebSecurityMvcTest {

	private final MockMvc mockMvc;

	@MockitoBean
	private PasswordLifecycleService passwordLifecycleService;

	@Autowired
	WebSecurityMvcTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void loginAnonimoRenderizaPaginaArkive() throws Exception {
		mockMvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("ArkIve")))
				.andExpect(content().string(containsString("Entrar")))
				.andExpect(content().string(containsString("name=\"username\"")))
				.andExpect(content().string(containsString("name=\"password\"")));
	}

	@Test
	void loginComErroRenderizaMensagemGenerica() throws Exception {
		mockMvc.perform(get("/login?error"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Login ou senha inválidos.")))
				.andExpect(content().string(not(containsString("usuário não existe"))));
	}

	@Test
	void loginComLogoutRenderizaMensagemDeSaida() throws Exception {
		mockMvc.perform(get("/login?logout"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Sessão encerrada com sucesso.")));
	}

	@Test
	@WithMockUser(roles = "SYSADMIN")
	void raizRedirecionaSysadminParaDashboardGlobal() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/sysadmin/dashboard"));
	}

	@Test
	@WithMockUser(roles = "ADMIN_CLINICA")
	void raizRedirecionaAdminClinicaParaDashboardDaClinica() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/dashboard"));
	}

	@Test
	@WithMockUser(roles = "VETERINARIO")
	void raizRedirecionaVeterinarioParaAcessoRestrito() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/acesso-web-restrito"));
	}

	@Test
	@WithMockUser(roles = "RESPONSAVEL")
	void raizRedirecionaResponsavelParaAcessoRestrito() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/acesso-web-restrito"));
	}

	@Test
	void rotaAdministrativaAnonimaRedirecionaParaLogin() throws Exception {
		mockMvc.perform(get("/sysadmin/dashboard").accept(MediaType.TEXT_HTML))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("**/login"));

		mockMvc.perform(get("/admin/dashboard").accept(MediaType.TEXT_HTML))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("**/login"));
	}

	@Test
	void apiHealthPermanecePublica() throws Exception {
		mockMvc.perform(get("/api/health"))
				.andExpect(status().isOk());
	}

	@Test
	void apiProtegidaPermaneceAutenticada() throws Exception {
		mockMvc.perform(get("/api/animais"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void sysadminAcessaDashboardsAdministrativos() throws Exception {
		mockMvc.perform(get("/sysadmin/dashboard").with(user("Ana Sys").roles("SYSADMIN")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Ana Sys")))
				.andExpect(content().string(containsString("SysAdmin")))
				.andExpect(content().string(containsString("sidebar-account")))
				.andExpect(content().string(containsString("sidebar-logout")))
				.andExpect(content().string(containsString("Sair")))
				.andExpect(content().string(not(containsString("user-menu"))))
				.andExpect(content().string(containsString("Indicadores globais")))
				.andExpect(content().string(not(containsString("senhaHash"))))
				.andExpect(content().string(not(containsString("$2a$"))));

		mockMvc.perform(get("/admin/dashboard").with(user("Ana Sys").roles("SYSADMIN")))
				.andExpect(status().isOk());
	}

	@Test
	void adminClinicaAcessaDashboardDaClinicaMasNaoSysadmin() throws Exception {
		mockMvc.perform(get("/admin/dashboard").with(user("Clara Admin").roles("ADMIN_CLINICA")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Clara Admin")))
				.andExpect(content().string(containsString("Administrador da Clínica")))
				.andExpect(content().string(not(containsString("senhaHash"))))
				.andExpect(content().string(not(containsString("$2a$"))));

		mockMvc.perform(get("/sysadmin/dashboard").with(user("Clara Admin").roles("ADMIN_CLINICA")))
				.andExpect(status().isForbidden());
	}

	@Test
	void veterinarioNaoAcessaDashboardsAdministrativos() throws Exception {
		mockMvc.perform(get("/admin/dashboard").with(user("Dra. Vera").roles("VETERINARIO")))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/sysadmin/dashboard").with(user("Dra. Vera").roles("VETERINARIO")))
				.andExpect(status().isForbidden());
	}

	@Test
	void responsavelNaoAcessaDashboardsAdministrativos() throws Exception {
		mockMvc.perform(get("/admin/dashboard").with(user("Rui Responsavel").roles("RESPONSAVEL")))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/sysadmin/dashboard").with(user("Rui Responsavel").roles("RESPONSAVEL")))
				.andExpect(status().isForbidden());
	}

	@Test
	void acessoRestritoMostraUsuarioEPerfilSemSenha() throws Exception {
		mockMvc.perform(get("/acesso-web-restrito").with(user("Dra. Vera").roles("VETERINARIO")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Dra. Vera")))
				.andExpect(content().string(containsString("Veterinário")))
				.andExpect(content().string(containsString("Acesso web restrito")))
				.andExpect(content().string(not(containsString("senhaHash"))))
				.andExpect(content().string(not(containsString("$2a$"))));
	}

	@Test
	void logoutUsaPostComCsrf() throws Exception {
		mockMvc.perform(post("/logout").accept(MediaType.TEXT_HTML).with(user("Ana Sys").roles("SYSADMIN")).with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login?logout"));
	}

	@Test
	void usuarioComTrocaObrigatoriaERedirecionadoParaAlterarSenha() throws Exception {
		mockMvc.perform(get("/sysadmin/dashboard").with(user(principal(true))))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/alterar-senha"));
	}

	@Test
	void usuarioComTrocaObrigatoriaNaoBypassaApi() throws Exception {
		mockMvc.perform(get("/api/animais").with(user(principal(true))))
				.andExpect(status().isForbidden())
				.andExpect(content().string(containsString("Troca de senha obrigatoria")));
	}

	@Test
	void usuarioComTrocaObrigatoriaAindaAcessaOpenApi() throws Exception {
		mockMvc.perform(get("/v3/api-docs").with(user(principal(true))))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
	}

	@Test
	void paginaAlterarSenhaAcessivelParaTrocaObrigatoria() throws Exception {
		mockMvc.perform(get("/alterar-senha").with(user(principal(true))))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Primeiro acesso")))
				.andExpect(content().string(containsString("Defina uma nova senha")));
	}

	@Test
	void trocaObrigatoriaBemSucedidaAtualizaSenhaERedireciona() throws Exception {
		Usuario usuario = new Usuario();
		usuario.setId(1L);
		usuario.setNome("Ana Sys");
		usuario.setLogin("ana@arkive.com");
		usuario.setSenhaHash("$2a$10$novoHash");
		usuario.setTipo(TipoUsuario.SYSADMIN);
		usuario.setAtivo("S");
		usuario.setTrocaSenha("N");
		when(passwordLifecycleService.alterarSenhaObrigatoria(1L, "NovaSenha1", "NovaSenha1")).thenReturn(usuario);

		mockMvc.perform(post("/alterar-senha")
						.with(user(principal(true)))
						.with(csrf())
						.param("novaSenha", "NovaSenha1")
						.param("confirmarNovaSenha", "NovaSenha1"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/sysadmin/dashboard"))
				.andExpect(authenticated().withUsername("ana@arkive.com"));
	}

	@Test
	void trocaObrigatoriaBemSucedidaRedirecionaVeterinarioParaLandingRestrita() throws Exception {
		Usuario usuario = new Usuario();
		usuario.setId(1L);
		usuario.setNome("Dra Vera");
		usuario.setLogin("vera@arkive.com");
		usuario.setSenhaHash("$2a$10$novoHash");
		usuario.setTipo(TipoUsuario.VETERINARIO);
		usuario.setAtivo("S");
		usuario.setTrocaSenha("N");
		when(passwordLifecycleService.alterarSenhaObrigatoria(1L, "NovaSenha1", "NovaSenha1")).thenReturn(usuario);

		mockMvc.perform(post("/alterar-senha")
						.with(user(new UsuarioPrincipal(1L, "Dra Vera", "vera@arkive.com", "$2a$10$hash", TipoUsuario.VETERINARIO, "S", true)))
						.with(csrf())
						.param("novaSenha", "NovaSenha1")
						.param("confirmarNovaSenha", "NovaSenha1"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/acesso-web-restrito"))
				.andExpect(authenticated().withUsername("vera@arkive.com"));
	}

	@Test
	void usuarioSemTrocaObrigatoriaSegueFluxoNormal() throws Exception {
		mockMvc.perform(get("/sysadmin/dashboard").with(user(principal(false))))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Indicadores globais")));
	}

	private UsuarioPrincipal principal(boolean trocaSenhaObrigatoria) {
		return new UsuarioPrincipal(1L, "Ana Sys", "ana@arkive.com", "$2a$10$hash", TipoUsuario.SYSADMIN, "S", trocaSenhaObrigatoria);
	}

}
