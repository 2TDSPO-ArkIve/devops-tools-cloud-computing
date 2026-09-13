package br.com.fiap.arkive.controller.web;

import br.com.fiap.arkive.domain.consulta.StatusConsulta;
import br.com.fiap.arkive.dto.response.AdesaoPrescricaoResponse;
import br.com.fiap.arkive.dto.response.AnimalResponse;
import br.com.fiap.arkive.dto.response.AnimalResponsavelResponse;
import br.com.fiap.arkive.dto.response.ClinicaResponse;
import br.com.fiap.arkive.dto.response.ConsultaResponse;
import br.com.fiap.arkive.dto.response.DiagnosticoResponse;
import br.com.fiap.arkive.dto.response.EspecieResponse;
import br.com.fiap.arkive.dto.response.PrescricaoResponse;
import br.com.fiap.arkive.dto.response.RacaResponse;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.AdesaoPrescricaoService;
import br.com.fiap.arkive.service.AnimalResponsavelService;
import br.com.fiap.arkive.service.AnimalService;
import br.com.fiap.arkive.service.ClinicaService;
import br.com.fiap.arkive.service.ConsultaService;
import br.com.fiap.arkive.service.DiagnosticoService;
import br.com.fiap.arkive.service.EspecieService;
import br.com.fiap.arkive.service.PasswordLifecycleService;
import br.com.fiap.arkive.service.PrescricaoService;
import br.com.fiap.arkive.service.RacaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local-nodb")
class AdminClinicaControllerTest {

	private final MockMvc mockMvc;

	@MockitoBean
	private AnimalService animalService;

	@MockitoBean
	private AnimalResponsavelService animalResponsavelService;

	@MockitoBean
	private ClinicaService clinicaService;

	@MockitoBean
	private ConsultaService consultaService;

	@MockitoBean
	private DiagnosticoService diagnosticoService;

	@MockitoBean
	private PrescricaoService prescricaoService;

	@MockitoBean
	private AdesaoPrescricaoService adesaoPrescricaoService;

	@MockitoBean
	private EspecieService especieService;

	@MockitoBean
	private RacaService racaService;

	@MockitoBean
	private PasswordLifecycleService passwordLifecycleService;

	@Autowired
	AdminClinicaControllerTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@BeforeEach
	void setUp() {
		when(animalService.listarAutorizado(any(), any(), any(), any(), any(), any(Pageable.class), any()))
				.thenReturn(new PageImpl<>(List.of(animal())));
		when(consultaService.listarAutorizado(any(), any(), any(), any(), any(), any(Pageable.class), any()))
				.thenReturn(new PageImpl<>(List.of(consulta())));
		when(diagnosticoService.listarAutorizado(any(), any(), any(), any(), any(Pageable.class), any()))
				.thenReturn(new PageImpl<>(List.of(apoioIa(), parecerVeterinario())));
		when(prescricaoService.listarAutorizado(any(), any(), any(Pageable.class), any()))
				.thenReturn(new PageImpl<>(List.of(prescricao())));
		when(adesaoPrescricaoService.listarAutorizado(any(), any(), any(), any(), any(Pageable.class), any()))
				.thenReturn(new PageImpl<>(List.of(adesao())));
		when(animalService.buscarPorIdAutorizado(eq(1L), any())).thenReturn(animal());
		when(animalResponsavelService.listarAtivosPorAnimal(eq(1L), any())).thenReturn(List.of(responsavelPrincipal(), responsavelSecundario()));
		when(consultaService.buscarPorIdAutorizado(eq(10L), any())).thenReturn(consulta());
		when(prescricaoService.buscarPorIdAutorizado(eq(20L), any())).thenReturn(prescricao());
		when(adesaoPrescricaoService.buscarPorIdAutorizado(eq(30L), any())).thenReturn(adesao());
		when(especieService.listar(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(especie())));
		when(racaService.listar(any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(raca())));
		when(clinicaService.listar(any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(clinica())));
		when(clinicaService.buscarPorId(1L)).thenReturn(clinica());
	}

	@Test
	void novoAnimalNaoCarregaCatalogoDeRacasSemEspecie() throws Exception {
		mockMvc.perform(get("/admin/animais/novo").with(user(adminPrincipal())))
				.andExpect(status().isOk()).andExpect(content().string(containsString("/js/animal-racas.js")))
				.andExpect(content().string(containsString("salvar-raca")));
		verify(racaService, never()).listar(any(), any(), any());
	}

	@Test
	void edicaoCarregaRacasSomenteDaEspecieDoAnimal() throws Exception {
		mockMvc.perform(get("/admin/animais/1/editar").with(user(adminPrincipal())))
				.andExpect(status().isOk());
		verify(racaService).listar(eq(null), eq(1L), any(Pageable.class));
	}

	@Test
	void listaDeAnimaisPreservaFiltroDeRacaSemExigirEspecie() throws Exception {
		mockMvc.perform(get("/admin/animais").with(user(adminPrincipal())))
				.andExpect(status().isOk());
		verify(racaService).listar(eq(null), eq(null), any(Pageable.class));
	}

	@Test
	void edicaoJavaPreservaNascimentoInformadoPelaApi() throws Exception {
		mockMvc.perform(post("/admin/animais/1/editar").with(user(adminPrincipal())).with(csrf())
				.param("nome", "Rex").param("especieId", "1").param("racaId", "2")
				.param("sexo", "M").param("castrado", "N").param("ativo", "S")
				.param("clinicaId", "1").param("dataNascimento", "2021-04-17"))
				.andExpect(status().is3xxRedirection());
		verify(animalService).atualizar(eq(1L), org.mockito.ArgumentMatchers.argThat(r -> LocalDate.parse("2021-04-17").equals(r.dataNascimento())), any());
	}

	@Test
	void adminClinicaSidebarIncluiNavegacaoOperacional() throws Exception {
		mockMvc.perform(get("/admin/dashboard").with(user(adminPrincipal())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("/admin/dashboard")))
				.andExpect(content().string(containsString("/admin/animais")))
				.andExpect(content().string(containsString("/admin/consultas")))
				.andExpect(content().string(containsString("/admin/prescricoes")))
				.andExpect(content().string(containsString("/admin/adesoes")))
				.andExpect(content().string(not(containsString("/sysadmin/usuarios"))));
	}

	@Test
	void dashboardAdminClinicaRenderizaAcessosEContagensReais() throws Exception {
		mockMvc.perform(get("/admin/dashboard").with(user(adminPrincipal())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Animais ativos")))
				.andExpect(content().string(containsString("Aguardando parecer")))
				.andExpect(content().string(containsString("Cadastro e manutenção dos animais da clínica.")))
				.andExpect(content().string(containsString("Acompanhamento de consultas no escopo da clínica.")))
				.andExpect(content().string(containsString("Consulta de prescrições registradas")))
				.andExpect(content().string(containsString("Consulta de registros de adesão terapêutica.")));
	}

	@Test
	void paginasClinicScopedRenderizam() throws Exception {
		mockMvc.perform(get("/admin/animais").with(user(adminPrincipal())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Rex")))
				.andExpect(content().string(containsString("Novo animal")))
				.andExpect(content().string(containsString("Macho")))
				.andExpect(content().string(containsString("Clínica Central")));

		mockMvc.perform(get("/admin/consultas").with(user(adminPrincipal())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Rex")))
				.andExpect(content().string(containsString("Dra. Vera")));

		mockMvc.perform(get("/admin/prescricoes").with(user(adminPrincipal())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Meloxicam")))
				.andExpect(content().string(containsString("Ver")));

		mockMvc.perform(get("/admin/adesoes").with(user(adminPrincipal())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Rui Tutor")))
				.andExpect(content().string(containsString("Sim")));
	}

	@Test
	void detalhesClinicosSaoReadOnly() throws Exception {
		mockMvc.perform(get("/admin/consultas/10").with(user(adminPrincipal())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Consulta anual")))
				.andExpect(content().string(containsString("Fluxo da consulta")))
				.andExpect(content().string(containsString("Apoio clínico por IA")))
				.andExpect(content().string(containsString("não substitui a avaliação do médico veterinário")))
				.andExpect(content().string(containsString("Suspeita de claudicação ortopédica")))
				.andExpect(content().string(containsString("Provisório")))
				.andExpect(content().string(containsString("Parecer veterinário")))
				.andExpect(content().string(containsString("Entorse leve confirmada")))
				.andExpect(content().string(containsString("Confirmado pelo veterinário")))
				.andExpect(content().string(containsString("Meloxicam")))
				.andExpect(content().string(containsString("Dose administrada")))
				.andExpect(content().string(not(containsString("Iniciar"))))
				.andExpect(content().string(not(containsString("Suporte clínico"))))
				.andExpect(content().string(not(containsString("Finalizar"))))
				.andExpect(content().string(not(containsString("Cancelar"))))
				.andExpect(content().string(not(containsString("Editar"))))
				.andExpect(content().string(not(containsString("Excluir"))));

		mockMvc.perform(get("/admin/prescricoes/20").with(user(adminPrincipal())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Meloxicam")))
				.andExpect(content().string(not(containsString("Cadastrar"))))
				.andExpect(content().string(not(containsString("Editar"))))
				.andExpect(content().string(not(containsString("Excluir"))));

		mockMvc.perform(get("/admin/adesoes/30").with(user(adminPrincipal())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Dose administrada")))
				.andExpect(content().string(not(containsString("Cadastrar"))))
				.andExpect(content().string(not(containsString("Editar"))))
				.andExpect(content().string(not(containsString("Excluir"))));
	}

	@Test
	void sysadminVisualizaDetalheClinicoGlobal() throws Exception {
		mockMvc.perform(get("/admin/consultas/10").with(user(sysadminPrincipal())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Apoio clínico por IA")))
				.andExpect(content().string(containsString("Parecer veterinário")))
				.andExpect(content().string(containsString("Entorse leve confirmada")));
	}

	@Test
	void sysadminSidebarIncluiAnimais() throws Exception {
		mockMvc.perform(get("/sysadmin/dashboard").with(user(sysadminPrincipal())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("/admin/animais")))
				.andExpect(content().string(containsString("Animais")));
	}

	@Test
	void listaAnimaisUsaFiltrosDoServicoAutorizado() throws Exception {
		ArgumentCaptor<Long> especieCaptor = ArgumentCaptor.forClass(Long.class);
		ArgumentCaptor<Long> racaCaptor = ArgumentCaptor.forClass(Long.class);
		ArgumentCaptor<Long> clinicaCaptor = ArgumentCaptor.forClass(Long.class);
		ArgumentCaptor<String> ativoCaptor = ArgumentCaptor.forClass(String.class);

		mockMvc.perform(get("/admin/animais")
						.param("nome", "Rex")
						.param("especieId", "1")
						.param("racaId", "2")
						.param("clinicaId", "1")
						.param("ativo", "S")
						.with(user(sysadminPrincipal())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Espécie")))
				.andExpect(content().string(containsString("Raça")))
				.andExpect(content().string(containsString("Clínica")));

		verify(animalService).listarAutorizado(eq("Rex"), especieCaptor.capture(), racaCaptor.capture(), clinicaCaptor.capture(), ativoCaptor.capture(), any(Pageable.class), any());
		assertEquals(1L, especieCaptor.getValue());
		assertEquals(2L, racaCaptor.getValue());
		assertEquals(1L, clinicaCaptor.getValue());
		assertEquals("S", ativoCaptor.getValue());
	}

	@Test
	void formularioNovoAnimalRespeitaClinicaPorPerfil() throws Exception {
		mockMvc.perform(get("/admin/animais/novo").with(user(adminPrincipal())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Novo animal")))
				.andExpect(content().string(containsString("Clínica Central")))
				.andExpect(content().string(not(containsString("name=\"clinicaId\""))))
				.andExpect(content().string(not(containsString("Veterinário"))));

		mockMvc.perform(get("/admin/animais/novo").with(user(sysadminPrincipal())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("name=\"clinicaId\"")))
				.andExpect(content().string(containsString("Sem clínica")));
	}

	@Test
	void detalheAnimalMostraResponsaveisEConsultas() throws Exception {
		mockMvc.perform(get("/admin/animais/1").with(user(adminPrincipal())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Responsável principal")))
				.andExpect(content().string(containsString("Rui Tutor")))
				.andExpect(content().string(containsString("CUIDADOR")))
				.andExpect(content().string(containsString("Últimos atendimentos")))
				.andExpect(content().string(containsString("Dra. Vera")))
				.andExpect(content().string(containsString("O histórico de consultas será preservado.")));
	}

	@Test
	void filtroDeConsultaPorStatusUsaServicoAutorizado() throws Exception {
		ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);

		mockMvc.perform(get("/admin/consultas")
						.param("status", "AP")
						.with(user(adminPrincipal())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Aguardando parecer")));

		verify(consultaService).listarAutorizado(any(), any(), any(), statusCaptor.capture(), any(), any(Pageable.class), any());
		assertEquals("AP", statusCaptor.getValue());
	}

	@Test
	void animaisUsamServicoPrincipalAwareParaEscrita() throws Exception {
		ArgumentCaptor<UsuarioPrincipal> principalCaptor = ArgumentCaptor.forClass(UsuarioPrincipal.class);

		mockMvc.perform(post("/admin/animais")
						.with(user(adminPrincipal()))
						.with(csrf())
						.param("nome", "Luna")
						.param("especieId", "1")
						.param("racaId", "2")
						.param("sexo", "F")
						.param("castrado", "S"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/animais"));

		verify(animalService).criar(any(), principalCaptor.capture());
		assertEquals(1L, principalCaptor.getValue().getClinicaId());
	}

	@Test
	void criacaoAnimalComErroDeValidacaoRetornaFormulario() throws Exception {
		mockMvc.perform(post("/admin/animais")
						.with(user(adminPrincipal()))
						.with(csrf())
						.param("nome", "")
						.param("castrado", "S")
						.param("ativo", "S"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Novo animal")))
				.andExpect(content().string(containsString("Dados do animal")));

		verify(animalService, never()).criar(any(), any());
	}

	@Test
	void edicaoAnimalAtualizaViaServicoAutorizado() throws Exception {
		mockMvc.perform(post("/admin/animais/1/editar")
						.with(user(adminPrincipal()))
						.with(csrf())
						.param("nome", "Rex Atualizado")
						.param("especieId", "1")
						.param("racaId", "2")
						.param("sexo", "M")
						.param("castrado", "S")
						.param("clinicaId", "1")
						.param("ativo", "S"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/animais/1"));

		verify(animalService).atualizar(eq(1L), any(), any());
	}

	@Test
	void desativacaoAnimalUsaSoftDeleteDoServico() throws Exception {
		mockMvc.perform(post("/admin/animais/1/desativar")
						.with(user(adminPrincipal()))
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/animais/1"));

		verify(animalService).excluir(eq(1L), any());
	}

	@Test
	void reativacaoAnimalUsaAtualizacaoNormal() throws Exception {
		when(animalService.buscarPorIdAutorizado(eq(1L), any())).thenReturn(new AnimalResponse(1L, "Rex", 1L, "Canino", 2L, "SRD", "M", "N", 1L, "Clínica Central", "N"));

		mockMvc.perform(post("/admin/animais/1/ativar")
						.with(user(adminPrincipal()))
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/animais/1"));

		verify(animalService).atualizar(eq(1L), any(), any());
	}

	@Test
	void consultasPrescricoesEAdesoesNaoTemRotasWebDePost() throws Exception {
		mockMvc.perform(post("/admin/consultas/10").with(user(adminPrincipal())).with(csrf()))
				.andExpect(status().is4xxClientError());
		mockMvc.perform(post("/admin/prescricoes/20").with(user(adminPrincipal())).with(csrf()))
				.andExpect(status().is4xxClientError());
		mockMvc.perform(post("/admin/adesoes/30").with(user(adminPrincipal())).with(csrf()))
				.andExpect(status().is4xxClientError());

		verify(consultaService, never()).atualizar(any(), any(), any());
		verify(prescricaoService, never()).atualizar(any(), any(), any());
		verify(adesaoPrescricaoService, never()).registrar(any(), any());
	}

	@Test
	@WithMockUser(roles = "VETERINARIO")
	void veterinarioNaoAcessaAdminClinica() throws Exception {
		mockMvc.perform(get("/admin/animais"))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/admin/animais")
						.with(csrf())
						.param("nome", "Luna")
						.param("especieId", "1")
						.param("castrado", "S")
						.param("ativo", "S"))
				.andExpect(status().isForbidden());
	}

	private UsuarioPrincipal adminPrincipal() {
		return new UsuarioPrincipal(
				5L,
				"Clara Admin",
				"clara@arkive.com",
				"$2a$10$hash",
				TipoUsuario.ADMIN_CLINICA,
				"S",
				false,
				null,
				null,
				1L
		);
	}

	private UsuarioPrincipal sysadminPrincipal() {
		return new UsuarioPrincipal(
				1L,
				"Ana Sys",
				"ana@arkive.com",
				"$2a$10$hash",
				TipoUsuario.SYSADMIN,
				"S",
				false,
				null,
				null,
				null
		);
	}

	private AnimalResponse animal() {
		return new AnimalResponse(1L, "Rex", 1L, "Canino", 2L, "SRD", "M", "N", 1L, "Clínica Central", "S");
	}

	private AnimalResponsavelResponse responsavelPrincipal() {
		return new AnimalResponsavelResponse(1L, "Rex", 3L, "Rui Tutor", "TUTOR_LEGAL", LocalDate.of(2026, 8, 1), null, "S", "S");
	}

	private AnimalResponsavelResponse responsavelSecundario() {
		return new AnimalResponsavelResponse(1L, "Rex", 4L, "Ana Cuidadora", "CUIDADOR", LocalDate.of(2026, 8, 2), null, "N", "S");
	}

	private ConsultaResponse consulta() {
		return new ConsultaResponse(
				10L,
				LocalDateTime.of(2026, 8, 30, 10, 0),
				"PRESENCIAL",
				"Consulta anual",
				"Apatia",
				"Sem alterações adicionais",
				new BigDecimal("8.5"),
				"Paciente com claudicação em membro posterior após atividade intensa.",
				StatusConsulta.FI.getCodigo(),
				StatusConsulta.FI.getDescricao(),
				1L,
				"Rex",
				2L,
				"Dra. Vera",
				1L,
				"Clínica Central"
		);
	}

	private DiagnosticoResponse apoioIa() {
		return new DiagnosticoResponse(
				11L,
				"Suspeita de claudicação ortopédica",
				"MODERADA",
				"N",
				"Correlacionar histórico de esforço, dor à palpação e resposta ao repouso antes de confirmar conduta.",
				new BigDecimal("82"),
				"N",
				10L,
				null
		);
	}

	private DiagnosticoResponse parecerVeterinario() {
		return new DiagnosticoResponse(
				12L,
				"Entorse leve confirmada",
				"LEVE",
				"S",
				null,
				null,
				"S",
				10L,
				99L
		);
	}

	private PrescricaoResponse prescricao() {
		return new PrescricaoResponse(
				20L,
				"Meloxicam",
				"1 comprimido",
				"A cada 24h",
				"ORAL",
				LocalDate.of(2026, 8, 30),
				LocalDate.of(2026, 9, 4),
				"Administrar após alimentação",
				10L
		);
	}

	private AdesaoPrescricaoResponse adesao() {
		return new AdesaoPrescricaoResponse(
				30L,
				20L,
				3L,
				"Rui Tutor",
				1L,
				"Rex",
				LocalDateTime.of(2026, 8, 30, 12, 0),
				"S",
				"Dose administrada"
		);
	}

	private EspecieResponse especie() {
		return new EspecieResponse(1L, "Canino", "S");
	}

	private RacaResponse raca() {
		return new RacaResponse(2L, "SRD", null, 1L, "Canino", "S");
	}

	private ClinicaResponse clinica() {
		return new ClinicaResponse(1L, "Clínica Central", "12345678000199", "Rua ArkIve, 100", "11999990000", "clinica@arkive.com", "S");
	}

}
