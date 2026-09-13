package br.com.fiap.arkive.controller.web;

import br.com.fiap.arkive.dto.request.UsuarioEditRequest;
import br.com.fiap.arkive.dto.request.UsuarioProvisioningRequest;
import br.com.fiap.arkive.dto.response.PasswordResetResult;
import br.com.fiap.arkive.dto.response.UsuarioContextOption;
import br.com.fiap.arkive.dto.response.UsuarioResponse;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.AccountProvisioningService;
import br.com.fiap.arkive.service.UsuarioService;
import br.com.fiap.arkive.service.PasswordLifecycleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local-nodb")
class SysAdminUsuarioControllerTest {

	private final MockMvc mockMvc;
	private final RequestMappingHandlerMapping handlerMapping;

	@MockitoBean
	private UsuarioService usuarioService;

	@MockitoBean
	private PasswordLifecycleService passwordLifecycleService;

	@MockitoBean
	private AccountProvisioningService accountProvisioningService;

	@Autowired
	SysAdminUsuarioControllerTest(MockMvc mockMvc, RequestMappingHandlerMapping handlerMapping) {
		this.mockMvc = mockMvc;
		this.handlerMapping = handlerMapping;
	}

	@BeforeEach
	void setUp() {
		when(usuarioService.listar(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
		when(usuarioService.listarPorTipos(anyList(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
		when(usuarioService.listarClinicasAtivas()).thenReturn(List.of(new UsuarioContextOption(1L, "Clínica Central")));
		when(usuarioService.listarVeterinariosDisponiveisParaCriacao()).thenReturn(List.of(new UsuarioContextOption(2L, "Dra. Vera")));
		when(usuarioService.listarResponsaveisDisponiveisParaCriacao()).thenReturn(List.of(new UsuarioContextOption(3L, "Rui Responsável")));
		when(usuarioService.listarVeterinariosDisponiveisParaEdicao(10L)).thenReturn(List.of(new UsuarioContextOption(2L, "Dra. Vera")));
		when(usuarioService.listarResponsaveisDisponiveisParaEdicao(10L)).thenReturn(List.of(new UsuarioContextOption(3L, "Rui Responsável")));
	}

	@Test
	@WithMockUser(roles = "SYSADMIN")
	void sysadminAcessaListagem() throws Exception {
		mockMvc.perform(get("/sysadmin/usuarios"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Usuários")))
				.andExpect(content().string(containsString("Novo usuário")));
	}

	@Test
	@WithMockUser(roles = "SYSADMIN")
	void filtroUsuariosNaoMostraLabelPerfilStandalone() throws Exception {
		mockMvc.perform(get("/sysadmin/usuarios"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("class=\"sr-only\" for=\"perfil-filter\"")))
				.andExpect(content().string(not(containsString("<label for=\"perfil-filter\">Perfil</label>"))))
				.andExpect(content().string(containsString("name=\"perfil\"")))
				.andExpect(content().string(containsString("data-auto-filter")))
				.andExpect(content().string(not(containsString(">Filtrar</button>"))))
				.andExpect(content().string(containsString("Limpar filtros")));
	}

	@Test
	@WithMockUser(roles = "ADMIN_CLINICA")
	void adminClinicaNaoAcessaListagemSysadmin() throws Exception {
		mockMvc.perform(get("/sysadmin/usuarios"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "VETERINARIO")
	void veterinarioNaoAcessaListagemSysadmin() throws Exception {
		mockMvc.perform(get("/sysadmin/usuarios"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "RESPONSAVEL")
	void responsavelNaoAcessaListagemSysadmin() throws Exception {
		mockMvc.perform(get("/sysadmin/usuarios"))
				.andExpect(status().isForbidden());
	}

	@Test
	void anonimoRedirecionaParaLogin() throws Exception {
		mockMvc.perform(get("/sysadmin/usuarios").accept(MediaType.TEXT_HTML))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("**/login"));
	}

	@Test
	@WithMockUser(roles = "SYSADMIN")
	void listagemRenderizaCamposSegurosESidebar() throws Exception {
		UsuarioResponse usuario = new UsuarioResponse(
				10L,
				"Ana Sys",
				TipoUsuario.SYSADMIN,
				"ana@arkive.com",
				"S",
				LocalDateTime.of(2026, 8, 24, 10, 0),
				null,
				null,
				null,
				null,
				null,
				null,
				false,
				null
		);
		when(usuarioService.listar(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(usuario)));
		when(usuarioService.listarPorTipos(anyList(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(usuario)));

		mockMvc.perform(get("/sysadmin/usuarios"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Ana Sys")))
				.andExpect(content().string(containsString("ana@arkive.com")))
				.andExpect(content().string(containsString("SysAdmin")))
				.andExpect(content().string(containsString("Ativo")))
				.andExpect(content().string(containsString("Usuários")))
				.andExpect(content().string(not(containsString("senhaHash"))))
				.andExpect(content().string(not(containsString("$2a$"))));
	}

	@Test
	@WithMockUser(roles = "SYSADMIN")
	void listagemUsaOrdenacaoPadraoPorCadastroMaisRecente() throws Exception {
		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

		mockMvc.perform(get("/sysadmin/usuarios"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("name=\"sortField\" value=\"dataCadastro\"")))
				.andExpect(content().string(containsString("name=\"sortDir\" value=\"desc\"")));

		verify(usuarioService).listarPorTipos(eq(List.of()), pageableCaptor.capture());
		assertEquals("dataCadastro: DESC,id: DESC", pageableCaptor.getValue().getSort().toString());
	}

	@Test
	@SuppressWarnings("unchecked")
	@WithMockUser(roles = "SYSADMIN")
	void listagemFiltraUsuariosAdministrativos() throws Exception {
		ArgumentCaptor<List<TipoUsuario>> tiposCaptor = ArgumentCaptor.forClass(List.class);

		mockMvc.perform(get("/sysadmin/usuarios").param("perfil", "ADMINISTRATIVOS"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Administrativos")));

		verify(usuarioService).listarPorTipos(tiposCaptor.capture(), any(Pageable.class));
		assertEquals(List.of(TipoUsuario.SYSADMIN, TipoUsuario.ADMIN_CLINICA), tiposCaptor.getValue());
	}

	@Test
	@SuppressWarnings("unchecked")
	@WithMockUser(roles = "SYSADMIN")
	void listagemCombinaFiltroDePerfilComOrdenacao() throws Exception {
		ArgumentCaptor<List<TipoUsuario>> tiposCaptor = ArgumentCaptor.forClass(List.class);
		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

		mockMvc.perform(get("/sysadmin/usuarios")
						.param("perfil", "RESPONSAVEL")
						.param("sortField", "login")
				.param("sortDir", "asc"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("name=\"sortField\" value=\"login\"")))
				.andExpect(content().string(containsString("name=\"sortDir\" value=\"asc\"")));

		verify(usuarioService).listarPorTipos(tiposCaptor.capture(), pageableCaptor.capture());
		assertEquals(List.of(TipoUsuario.RESPONSAVEL), tiposCaptor.getValue());
		assertEquals("login: ASC,dataCadastro: DESC,id: DESC", pageableCaptor.getValue().getSort().toString());
	}

	@Test
	@WithMockUser(roles = "SYSADMIN")
	void formularioNovoRenderizaPerfisEOpcoesDeContexto() throws Exception {
		mockMvc.perform(get("/sysadmin/usuarios/novo"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("SysAdmin")))
				.andExpect(content().string(containsString("Administrador da Clínica")))
				.andExpect(content().string(containsString("Veterinário")))
				.andExpect(content().string(containsString("Responsável")))
				.andExpect(content().string(containsString("Clínica Central")))
				.andExpect(content().string(containsString("Dra. Vera")))
				.andExpect(content().string(containsString("Rui Responsável")))
				.andExpect(content().string(containsString("Primeiro acesso")))
				.andExpect(content().string(containsString("e-mail cadastrado como login e senha inicial")))
				.andExpect(content().string(not(containsString("name=\"senha\""))));
	}

	@Test
	@WithMockUser(roles = "SYSADMIN")
	void formularioNovoOfereceSomenteVeterinariosEResponsaveisDisponiveis() throws Exception {
		when(usuarioService.listarVeterinariosDisponiveisParaCriacao()).thenReturn(List.of(new UsuarioContextOption(2L, "Dra. Livre")));
		when(usuarioService.listarResponsaveisDisponiveisParaCriacao()).thenReturn(List.of(new UsuarioContextOption(3L, "Rui Livre")));

		mockMvc.perform(get("/sysadmin/usuarios/novo"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Dra. Livre")))
				.andExpect(content().string(not(containsString("Dra. Associada"))))
				.andExpect(content().string(containsString("Rui Livre")))
				.andExpect(content().string(not(containsString("Rui Associado"))))
				.andExpect(content().string(containsString("Clínica Central")));

		verify(usuarioService).listarVeterinariosDisponiveisParaCriacao();
		verify(usuarioService).listarResponsaveisDisponiveisParaCriacao();
		verify(usuarioService).listarClinicasAtivas();
	}

	@Test
	@WithMockUser(roles = "SYSADMIN")
	void formularioNovoExibeEstadoVazioParaVinculosIndisponiveis() throws Exception {
		when(usuarioService.listarVeterinariosDisponiveisParaCriacao()).thenReturn(List.of());
		when(usuarioService.listarResponsaveisDisponiveisParaCriacao()).thenReturn(List.of());

		mockMvc.perform(get("/sysadmin/usuarios/novo"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Nenhum veterinário disponível")))
				.andExpect(content().string(containsString("Nenhum responsável disponível")));
	}

	@Test
	@WithMockUser(roles = "SYSADMIN")
	void formularioEditarRenderizaPerfisSemCampoSenha() throws Exception {
		when(usuarioService.buscarPorId(10L)).thenReturn(usuarioResponse());

		mockMvc.perform(get("/sysadmin/usuarios/10/editar"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Editar usuário")))
				.andExpect(content().string(containsString("Clínica Central")))
				.andExpect(content().string(not(containsString("name=\"senha\""))));
	}

	@Test
	@WithMockUser(roles = "SYSADMIN")
	void formularioEditarMantemVeterinarioAtualEExcluiVinculosDeOutrosUsuarios() throws Exception {
		when(usuarioService.buscarPorId(10L)).thenReturn(usuarioVeterinarioResponse());
		when(usuarioService.listarVeterinariosDisponiveisParaEdicao(10L)).thenReturn(List.of(
				new UsuarioContextOption(2L, "Dra. Atual"),
				new UsuarioContextOption(4L, "Dr. Livre")
		));

		mockMvc.perform(get("/sysadmin/usuarios/10/editar"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Dra. Atual")))
				.andExpect(content().string(containsString("Dr. Livre")))
				.andExpect(content().string(not(containsString("Dra. Outro Usuario"))));

		verify(usuarioService).listarVeterinariosDisponiveisParaEdicao(10L);
	}

	@Test
	@WithMockUser(roles = "SYSADMIN")
	void formularioEditarMantemResponsavelAtualEExcluiVinculosDeOutrosUsuarios() throws Exception {
		when(usuarioService.buscarPorId(10L)).thenReturn(usuarioResponsavelResponse());
		when(usuarioService.listarResponsaveisDisponiveisParaEdicao(10L)).thenReturn(List.of(
				new UsuarioContextOption(3L, "Rui Atual"),
				new UsuarioContextOption(5L, "Mia Livre")
		));

		mockMvc.perform(get("/sysadmin/usuarios/10/editar"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Rui Atual")))
				.andExpect(content().string(containsString("Mia Livre")))
				.andExpect(content().string(not(containsString("Rui Outro Usuario"))));

		verify(usuarioService).listarResponsaveisDisponiveisParaEdicao(10L);
	}

	@Test
	void criarUsuarioExigeCsrf() throws Exception {
		mockMvc.perform(post("/sysadmin/usuarios").with(user("Ana Sys").roles("SYSADMIN")))
				.andExpect(status().isForbidden());
		verify(accountProvisioningService, never()).provisionar(any());
	}

	@Test
	void criarUsuarioValidoChamaProvisionamentoSemCampoSenhaERedireciona() throws Exception {
		ArgumentCaptor<UsuarioProvisioningRequest> captor = ArgumentCaptor.forClass(UsuarioProvisioningRequest.class);

		mockMvc.perform(post("/sysadmin/usuarios")
						.with(user("Ana Sys").roles("SYSADMIN"))
						.with(csrf())
						.param("nome", "Novo Admin")
						.param("login", "novo@arkive.com")
						.param("tipo", "SYSADMIN"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/sysadmin/usuarios"));

		verify(accountProvisioningService).provisionar(captor.capture());
		assertEquals("Novo Admin", captor.getValue().nome());
		assertEquals("novo@arkive.com", captor.getValue().login());
		assertEquals(TipoUsuario.SYSADMIN, captor.getValue().tipo());
	}

	@Test
	void criarUsuarioComBeanValidationInvalidaRenderizaFormulario() throws Exception {
		mockMvc.perform(post("/sysadmin/usuarios")
						.with(user("Ana Sys").roles("SYSADMIN"))
						.with(csrf())
						.param("nome", "")
						.param("login", "")
						.param("tipo", ""))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Novo usuário")));

		verify(accountProvisioningService, never()).provisionar(any());
	}

	@Test
	void criarUsuarioExigeLoginComFormatoDeEmail() throws Exception {
		mockMvc.perform(post("/sysadmin/usuarios")
						.with(user("Ana Sys").roles("SYSADMIN"))
						.with(csrf())
						.param("nome", "Novo Admin")
						.param("login", "login-invalido")
						.param("tipo", "SYSADMIN"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Novo usuário")));

		verify(accountProvisioningService, never()).provisionar(any());
	}

	@Test
	void criarUsuarioComErroDeNegocioRenderizaFormularioSemSenha() throws Exception {
		doThrow(new BusinessException("Login de usuario ja cadastrado.")).when(accountProvisioningService).provisionar(any());

		mockMvc.perform(post("/sysadmin/usuarios")
						.with(user("Ana Sys").roles("SYSADMIN"))
						.with(csrf())
						.param("nome", "Novo Admin")
						.param("login", "novo@arkive.com")
						.param("tipo", "SYSADMIN"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Login de usuario ja cadastrado.")))
				.andExpect(content().string(not(containsString("name=\"senha\""))))
				.andExpect(content().string(not(containsString("senhaHash"))));
	}

	@Test
	void atualizarUsuarioExigeCsrf() throws Exception {
		mockMvc.perform(post("/sysadmin/usuarios/10/editar").with(user("Ana Sys").roles("SYSADMIN")))
				.andExpect(status().isForbidden());
		verify(usuarioService, never()).atualizar(any(), any(), any());
	}

	@Test
	void atualizarUsuarioValidoChamaServiceComUsuarioAtual() throws Exception {
		UsuarioPrincipal principal = new UsuarioPrincipal(99L, "Ana Sys", "ana@arkive.com", "$2a$10$hash", TipoUsuario.SYSADMIN, "S");
		ArgumentCaptor<UsuarioEditRequest> captor = ArgumentCaptor.forClass(UsuarioEditRequest.class);

		mockMvc.perform(post("/sysadmin/usuarios/10/editar")
						.with(user(principal))
						.with(csrf())
						.param("nome", "Admin Editado")
						.param("login", "admin.editado@arkive.com")
						.param("tipo", "ADMIN_CLINICA")
						.param("clinicaId", "1"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/sysadmin/usuarios"));

		verify(usuarioService).atualizar(eq(10L), captor.capture(), eq(99L));
		assertEquals("Admin Editado", captor.getValue().nome());
		assertEquals(TipoUsuario.ADMIN_CLINICA, captor.getValue().tipo());
		assertEquals(1L, captor.getValue().clinicaId());
	}

	@Test
	void desativarUsuarioExigeCsrf() throws Exception {
		mockMvc.perform(post("/sysadmin/usuarios/10/desativar").with(user("Ana Sys").roles("SYSADMIN")))
				.andExpect(status().isForbidden());
		verify(usuarioService, never()).desativar(any(), any());
	}

	@Test
	void desativarUsuarioChamaServiceComUsuarioAtual() throws Exception {
		UsuarioPrincipal principal = new UsuarioPrincipal(99L, "Ana Sys", "ana@arkive.com", "$2a$10$hash", TipoUsuario.SYSADMIN, "S");

		mockMvc.perform(post("/sysadmin/usuarios/10/desativar")
						.with(user(principal))
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/sysadmin/usuarios"));

		verify(usuarioService).desativar(10L, 99L);
	}

	@Test
	void ativarUsuarioChamaService() throws Exception {
		mockMvc.perform(post("/sysadmin/usuarios/10/ativar")
						.with(user("Ana Sys").roles("SYSADMIN"))
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/sysadmin/usuarios"));

		verify(usuarioService).ativar(10L);
	}

	@Test
	void resetarSenhaExigeCsrf() throws Exception {
		mockMvc.perform(post("/sysadmin/usuarios/10/resetar-senha").with(user("Ana Sys").roles("SYSADMIN")))
				.andExpect(status().isForbidden());
		verify(passwordLifecycleService, never()).resetarSenha(any());
	}

	@Test
	void resetarSenhaChamaServiceEMantemSenhaForaDaUrl() throws Exception {
		when(passwordLifecycleService.resetarSenha(10L)).thenReturn(new PasswordResetResult(10L, "TempSenha1"));

		mockMvc.perform(post("/sysadmin/usuarios/10/resetar-senha")
						.with(user("Ana Sys").roles("SYSADMIN"))
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/sysadmin/usuarios"));

		verify(passwordLifecycleService).resetarSenha(10L);
	}

	@Test
	void naoExisteRotaGetParaMudancaDeEstado() {
		boolean hasGetStateRoute = handlerMapping.getHandlerMethods().keySet().stream()
				.map(RequestMappingInfo::toString)
				.anyMatch(mapping -> mapping.contains("GET") && (mapping.contains("/desativar") || mapping.contains("/ativar")));

		assertTrue(!hasGetStateRoute);
	}

	private UsuarioResponse usuarioResponse() {
		return new UsuarioResponse(
				10L,
				"Ana Sys",
				TipoUsuario.SYSADMIN,
				"ana@arkive.com",
				"S",
				LocalDateTime.of(2026, 8, 24, 10, 0),
				null,
				null,
				null,
				null,
				null,
				null,
				false,
				null
		);
	}

	private UsuarioResponse usuarioVeterinarioResponse() {
		return new UsuarioResponse(
				10L,
				"Dra. Atual",
				TipoUsuario.VETERINARIO,
				"vera@arkive.com",
				"S",
				LocalDateTime.of(2026, 8, 24, 10, 0),
				null,
				null,
				2L,
				"Dra. Atual",
				null,
				null,
				false,
				null
		);
	}

	private UsuarioResponse usuarioResponsavelResponse() {
		return new UsuarioResponse(
				10L,
				"Rui Atual",
				TipoUsuario.RESPONSAVEL,
				"rui@arkive.com",
				"S",
				LocalDateTime.of(2026, 8, 24, 10, 0),
				3L,
				"Rui Atual",
				null,
				null,
				null,
				null,
				false,
				null
		);
	}

}
