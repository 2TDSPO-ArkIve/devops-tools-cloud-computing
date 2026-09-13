package br.com.fiap.arkive.controller.web;

import br.com.fiap.arkive.dto.request.ClinicaRequest;
import br.com.fiap.arkive.dto.request.ResponsavelRequest;
import br.com.fiap.arkive.dto.request.VeterinarioRequest;
import br.com.fiap.arkive.dto.response.ClinicaResponse;
import br.com.fiap.arkive.dto.response.ResponsavelResponse;
import br.com.fiap.arkive.dto.response.UsuarioContextOption;
import br.com.fiap.arkive.dto.response.VeterinarioResponse;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.service.ClinicaService;
import br.com.fiap.arkive.service.PasswordLifecycleService;
import br.com.fiap.arkive.service.ResponsavelService;
import br.com.fiap.arkive.service.UsuarioService;
import br.com.fiap.arkive.service.VeterinarioService;
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

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
class SysAdminBusinessRegistrationControllerTest {

	private final MockMvc mockMvc;

	@MockitoBean
	private ClinicaService clinicaService;

	@MockitoBean
	private VeterinarioService veterinarioService;

	@MockitoBean
	private ResponsavelService responsavelService;

	@MockitoBean
	private UsuarioService usuarioService;

	@MockitoBean
	private PasswordLifecycleService passwordLifecycleService;

	@Autowired
	SysAdminBusinessRegistrationControllerTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@BeforeEach
	void setUp() {
		when(clinicaService.listar(any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
		when(clinicaService.listarPorTexto(any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
		when(veterinarioService.listar(any(), any(), any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
		when(veterinarioService.listarPorTexto(any(), any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
		when(responsavelService.listar(any(), any(), any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
		when(responsavelService.listarPorTexto(any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
		when(usuarioService.listarClinicasAtivas()).thenReturn(List.of(new UsuarioContextOption(1L, "Clínica Central")));
		when(responsavelService.listarTipos()).thenReturn(List.of("TUTOR", "ONG"));
	}

	@Test
	@WithMockUser(roles = "SYSADMIN")
	void sidebarSysadminIncluiCadastrosAdministrativos() throws Exception {
		mockMvc.perform(get("/sysadmin/clinicas"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("/sysadmin/dashboard")))
				.andExpect(content().string(containsString("/sysadmin/usuarios")))
				.andExpect(content().string(containsString("/sysadmin/clinicas")))
				.andExpect(content().string(containsString("/sysadmin/veterinarios")))
				.andExpect(content().string(containsString("/sysadmin/responsaveis")));
	}

	@Test
	@WithMockUser(roles = "SYSADMIN")
	void listaClinicasRenderizaDadosReais() throws Exception {
		when(clinicaService.listar(any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(clinica())));
		when(clinicaService.listarPorTexto(any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(clinica())));

		mockMvc.perform(get("/sysadmin/clinicas"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Clínica Central")))
				.andExpect(content().string(containsString("12345678000199")))
				.andExpect(content().string(containsString("clinicacentral@arkive.com")))
				.andExpect(content().string(containsString("Nova clínica")))
				.andExpect(content().string(containsString("list-toolbar")))
				.andExpect(content().string(containsString("Nome, CNPJ ou e-mail")))
				.andExpect(content().string(containsString("data-auto-filter")))
				.andExpect(content().string(not(containsString(">Filtrar</button>"))));
	}

	@Test
	@WithMockUser(roles = "SYSADMIN")
	void listaClinicasRenderizaEstadoVazio() throws Exception {
		mockMvc.perform(get("/sysadmin/clinicas"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Nenhuma clínica cadastrada.")));
	}

	@Test
	@WithMockUser(roles = "SYSADMIN")
	void formularioNovaClinicaRenderiza() throws Exception {
		mockMvc.perform(get("/sysadmin/clinicas/nova"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Nova clínica")))
				.andExpect(content().string(containsString("name=\"nome\"")))
				.andExpect(content().string(containsString("name=\"cnpj\"")))
				.andExpect(content().string(containsString("name=\"email\"")));
	}

	@Test
	void criarClinicaValidaChamaServiceERedireciona() throws Exception {
		ArgumentCaptor<ClinicaRequest> captor = ArgumentCaptor.forClass(ClinicaRequest.class);

		mockMvc.perform(post("/sysadmin/clinicas")
						.with(user("Ana Sys").roles("SYSADMIN"))
						.with(csrf())
						.param("nome", "Clínica Nova")
						.param("cnpj", "12345678000199")
						.param("email", "nova@arkive.com")
						.param("telefone", "11999999999")
						.param("endereco", "Rua A"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/sysadmin/clinicas"));

		verify(clinicaService).criar(captor.capture());
		assertEquals("Clínica Nova", captor.getValue().nome());
		assertEquals("nova@arkive.com", captor.getValue().email());
	}

	@Test
	void criarClinicaComErroDeValidacaoRetornaFormulario() throws Exception {
		mockMvc.perform(post("/sysadmin/clinicas")
						.with(user("Ana Sys").roles("SYSADMIN"))
						.with(csrf())
						.param("nome", "")
						.param("cnpj", "")
						.param("email", "email-invalido"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Nova clínica")));

		verify(clinicaService, never()).criar(any());
	}

	@Test
	void criarClinicaComErroDeNegocioRetornaFormulario() throws Exception {
		doThrow(new BusinessException("Login de usuário já cadastrado.")).when(clinicaService).criar(any());

		mockMvc.perform(post("/sysadmin/clinicas")
						.with(user("Ana Sys").roles("SYSADMIN"))
						.with(csrf())
						.param("nome", "Clínica Nova")
						.param("cnpj", "12345678000199")
						.param("email", "nova@arkive.com"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Login de usuário já cadastrado.")));
	}

	@Test
	@WithMockUser(roles = "SYSADMIN")
	void editarClinicaRenderizaEAtualiza() throws Exception {
		when(clinicaService.buscarPorId(1L)).thenReturn(clinica());

		mockMvc.perform(get("/sysadmin/clinicas/1/editar"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Editar clínica")))
				.andExpect(content().string(containsString("Cl&iacute;nica Central")));

		mockMvc.perform(post("/sysadmin/clinicas/1/editar")
						.with(csrf())
						.param("nome", "Clínica Editada")
						.param("cnpj", "12345678000199")
						.param("email", "editada@arkive.com"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/sysadmin/clinicas"));

		verify(clinicaService).atualizar(anyLong(), any());
	}

	@Test
	void desativarClinicaExigeCsrf() throws Exception {
		mockMvc.perform(post("/sysadmin/clinicas/1/desativar").with(user("Ana Sys").roles("SYSADMIN")))
				.andExpect(status().isForbidden());
		verify(clinicaService, never()).excluir(anyLong());
	}

	@Test
	@WithMockUser(roles = "SYSADMIN")
	void listaVeterinariosRenderizaDadosReais() throws Exception {
		when(veterinarioService.listar(any(), any(), any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(veterinario())));
		when(veterinarioService.listarPorTexto(any(), any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(veterinario())));

		mockMvc.perform(get("/sysadmin/veterinarios"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Dra. Vera")))
				.andExpect(content().string(containsString("12345SP")))
				.andExpect(content().string(containsString("Clínica Central")))
				.andExpect(content().string(containsString("Novo veterinário")))
				.andExpect(content().string(containsString("list-toolbar")))
				.andExpect(content().string(containsString("Nome, CRMV ou e-mail")))
				.andExpect(content().string(containsString("data-auto-filter")))
				.andExpect(content().string(not(containsString(">Filtrar</button>"))));
	}

	@Test
	@WithMockUser(roles = "SYSADMIN")
	void listaVeterinariosRenderizaEstadoVazio() throws Exception {
		mockMvc.perform(get("/sysadmin/veterinarios"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Nenhum veterinário cadastrado.")));
	}

	@Test
	@WithMockUser(roles = "SYSADMIN")
	void formularioNovoVeterinarioRenderizaClinicasAtivas() throws Exception {
		mockMvc.perform(get("/sysadmin/veterinarios/novo"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Novo veterinário")))
				.andExpect(content().string(containsString("Clínica Central")));
	}

	@Test
	void criarVeterinarioValidoChamaServiceQuePreservaProvisionamentoAutomatico() throws Exception {
		ArgumentCaptor<VeterinarioRequest> captor = ArgumentCaptor.forClass(VeterinarioRequest.class);

		mockMvc.perform(post("/sysadmin/veterinarios")
						.with(user("Ana Sys").roles("SYSADMIN"))
						.with(csrf())
						.param("nome", "Dra. Vera")
						.param("crmv", "12345SP")
						.param("especialidade", "Felinos")
						.param("email", "vera@arkive.com")
						.param("clinicaId", "1"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/sysadmin/veterinarios"));

		verify(veterinarioService).criar(captor.capture());
		assertEquals("Dra. Vera", captor.getValue().nome());
		assertEquals("vera@arkive.com", captor.getValue().email());
		assertEquals(1L, captor.getValue().clinicaId());
	}

	@Test
	void criarVeterinarioComErroDeValidacaoRetornaFormulario() throws Exception {
		mockMvc.perform(post("/sysadmin/veterinarios")
						.with(user("Ana Sys").roles("SYSADMIN"))
						.with(csrf())
						.param("nome", "")
						.param("crmv", "")
						.param("email", "email-invalido"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Novo veterinário")));

		verify(veterinarioService, never()).criar(any());
	}

	@Test
	@WithMockUser(roles = "SYSADMIN")
	void editarVeterinarioRenderizaEAtualiza() throws Exception {
		when(veterinarioService.buscarPorId(2L)).thenReturn(veterinario());

		mockMvc.perform(get("/sysadmin/veterinarios/2/editar"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Editar veterinário")))
				.andExpect(content().string(containsString("Dra. Vera")));

		mockMvc.perform(post("/sysadmin/veterinarios/2/editar")
						.with(csrf())
						.param("nome", "Dra. Vera Editada")
						.param("crmv", "12345SP")
						.param("email", "vera.editada@arkive.com"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/sysadmin/veterinarios"));

		verify(veterinarioService).atualizar(anyLong(), any());
	}

	@Test
	void desativarVeterinarioExigeCsrf() throws Exception {
		mockMvc.perform(post("/sysadmin/veterinarios/2/desativar").with(user("Ana Sys").roles("SYSADMIN")))
				.andExpect(status().isForbidden());
		verify(veterinarioService, never()).excluir(anyLong());
	}

	@Test
	@WithMockUser(roles = "SYSADMIN")
	void listaResponsaveisRenderizaDadosReais() throws Exception {
		when(responsavelService.listar(any(), any(), any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(responsavel())));
		when(responsavelService.listarPorTexto(any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(responsavel())));

		mockMvc.perform(get("/sysadmin/responsaveis"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Rui Tutor")))
				.andExpect(content().string(containsString("12345678900")))
				.andExpect(content().string(containsString("TUTOR")))
				.andExpect(content().string(containsString("Novo responsável")))
				.andExpect(content().string(containsString("list-toolbar")))
				.andExpect(content().string(containsString("Nome, documento ou e-mail")))
				.andExpect(content().string(containsString("data-auto-filter")))
				.andExpect(content().string(not(containsString(">Filtrar</button>"))));
	}

	@Test
	@WithMockUser(roles = "SYSADMIN")
	void filtrosSysadminChamamServicosComParametros() throws Exception {
		mockMvc.perform(get("/sysadmin/clinicas")
						.param("busca", "central")
						.param("ativo", "S"))
				.andExpect(status().isOk());
		verify(clinicaService).listarPorTexto(eq("central"), eq("S"), any(Pageable.class));

		mockMvc.perform(get("/sysadmin/veterinarios")
						.param("busca", "vera")
						.param("clinicaId", "1")
						.param("ativo", "N"))
				.andExpect(status().isOk());
		verify(veterinarioService).listarPorTexto(eq("vera"), eq(1L), eq("N"), any(Pageable.class));

		mockMvc.perform(get("/sysadmin/responsaveis")
						.param("busca", "rui")
						.param("ativo", "S"))
				.andExpect(status().isOk());
		verify(responsavelService).listarPorTexto(eq("rui"), eq("S"), any(Pageable.class));
	}

	@Test
	@WithMockUser(roles = "SYSADMIN")
	void listaResponsaveisRenderizaEstadoVazio() throws Exception {
		mockMvc.perform(get("/sysadmin/responsaveis"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Nenhum responsável cadastrado.")));
	}

	@Test
	@WithMockUser(roles = "SYSADMIN")
	void formularioNovoResponsavelRenderizaTipos() throws Exception {
		mockMvc.perform(get("/sysadmin/responsaveis/novo"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Novo responsável")))
				.andExpect(content().string(containsString("TUTOR")))
				.andExpect(content().string(containsString("ONG")));
	}

	@Test
	void criarResponsavelValidoChamaApenasServiceDeResponsavel() throws Exception {
		ArgumentCaptor<ResponsavelRequest> captor = ArgumentCaptor.forClass(ResponsavelRequest.class);

		mockMvc.perform(post("/sysadmin/responsaveis")
						.with(user("Ana Sys").roles("SYSADMIN"))
						.with(csrf())
						.param("nome", "Rui Tutor")
						.param("documento", "12345678900")
						.param("email", "rui@arkive.com")
						.param("telefone", "11999999999")
						.param("tipo", "TUTOR"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/sysadmin/responsaveis"));

		verify(responsavelService).criar(captor.capture());
		assertEquals("Rui Tutor", captor.getValue().nome());
		assertEquals("rui@arkive.com", captor.getValue().email());
		assertEquals("TUTOR", captor.getValue().tipo());
	}

	@Test
	void criarResponsavelComErroDeValidacaoRetornaFormulario() throws Exception {
		mockMvc.perform(post("/sysadmin/responsaveis")
						.with(user("Ana Sys").roles("SYSADMIN"))
						.with(csrf())
						.param("nome", "")
						.param("documento", "")
						.param("tipo", ""))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Novo responsável")));

		verify(responsavelService, never()).criar(any());
	}

	@Test
	@WithMockUser(roles = "SYSADMIN")
	void editarResponsavelRenderizaEAtualiza() throws Exception {
		when(responsavelService.buscarPorId(3L)).thenReturn(responsavel());

		mockMvc.perform(get("/sysadmin/responsaveis/3/editar"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Editar responsável")))
				.andExpect(content().string(containsString("Rui Tutor")));

		mockMvc.perform(post("/sysadmin/responsaveis/3/editar")
						.with(csrf())
						.param("nome", "Rui Editado")
						.param("documento", "12345678900")
						.param("email", "rui.editado@arkive.com")
						.param("tipo", "TUTOR"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/sysadmin/responsaveis"));

		verify(responsavelService).atualizar(anyLong(), any());
	}

	@Test
	void desativarResponsavelExigeCsrf() throws Exception {
		mockMvc.perform(post("/sysadmin/responsaveis/3/desativar").with(user("Ana Sys").roles("SYSADMIN")))
				.andExpect(status().isForbidden());
		verify(responsavelService, never()).excluir(anyLong());
	}

	@Test
	void rotasSysadminExigemSysadmin() throws Exception {
		mockMvc.perform(get("/sysadmin/clinicas").with(user("Clara Admin").roles("ADMIN_CLINICA")))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/sysadmin/veterinarios").with(user("Dra. Vera").roles("VETERINARIO")))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/sysadmin/responsaveis").with(user("Rui Tutor").roles("RESPONSAVEL")))
				.andExpect(status().isForbidden());
	}

	@Test
	void rotasSysadminAnonimasRedirecionamParaLogin() throws Exception {
		mockMvc.perform(get("/sysadmin/clinicas").accept(MediaType.TEXT_HTML))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("**/login"));
		mockMvc.perform(get("/sysadmin/veterinarios").accept(MediaType.TEXT_HTML))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("**/login"));
		mockMvc.perform(get("/sysadmin/responsaveis").accept(MediaType.TEXT_HTML))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("**/login"));
	}

	private ClinicaResponse clinica() {
		return new ClinicaResponse(
				1L,
				"Clínica Central",
				"12345678000199",
				"Rua ArkIve, 10",
				"11999999999",
				"clinicacentral@arkive.com",
				"S"
		);
	}

	private VeterinarioResponse veterinario() {
		return new VeterinarioResponse(
				2L,
				"Dra. Vera",
				"12345SP",
				"Felinos",
				"vera@arkive.com",
				1L,
				"Clínica Central",
				"S"
		);
	}

	private ResponsavelResponse responsavel() {
		return new ResponsavelResponse(
				3L,
				"Rui Tutor",
				"12345678900",
				"rui@arkive.com",
				"11999999999",
				"TUTOR",
				null,
				"S",
				"S"
		);
	}

}
