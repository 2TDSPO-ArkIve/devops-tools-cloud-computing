package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.config.SecurityConfig;
import br.com.fiap.arkive.dto.request.AtualizarNarrativaConsultaRequest;
import br.com.fiap.arkive.dto.request.ConsultaRequest;
import br.com.fiap.arkive.dto.response.ClinicalSupportResponse;
import br.com.fiap.arkive.dto.response.ConsultaResponse;
import br.com.fiap.arkive.dto.response.ConsultaWorkflowResponse;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.GlobalExceptionHandler;
import br.com.fiap.arkive.security.ArkiveUserDetailsService;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.AdesaoPrescricaoService;
import br.com.fiap.arkive.service.ClinicalSupportService;
import br.com.fiap.arkive.service.ConsultaResumoPdfService;
import br.com.fiap.arkive.service.ConsultaService;
import br.com.fiap.arkive.service.ConsultaWorkflowService;
import br.com.fiap.arkive.service.PrescricaoService;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
		ConsultaController.class,
		ConsultaWorkflowController.class,
		PrescricaoController.class,
		AdesaoPrescricaoController.class
})
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class ClinicalWorkflowApiMvcTest {

	private final MockMvc mockMvc;

	@MockitoBean
	private ConsultaService consultaService;

	@MockitoBean
	private ConsultaWorkflowService consultaWorkflowService;

	@MockitoBean
	private ClinicalSupportService clinicalSupportService;

	@MockitoBean
	private ConsultaResumoPdfService consultaResumoPdfService;

	@MockitoBean
	private PrescricaoService prescricaoService;

	@MockitoBean
	private AdesaoPrescricaoService adesaoPrescricaoService;

	@MockitoBean
	private ArkiveUserDetailsService arkiveUserDetailsService;

	@Autowired
	ClinicalWorkflowApiMvcTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void apiProtegidaAnonimaRetorna401Json() throws Exception {
		mockMvc.perform(get("/api/consultas").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.message").value("Autenticacao obrigatoria."))
				.andExpect(jsonPath("$.path").value("/api/consultas"));
	}

	@Test
	void consultaListaAceitaPageSizeSemSortEOmiteOrdenacao() throws Exception {
		when(consultaService.listarAutorizado(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class), any(UsuarioPrincipal.class)))
				.thenReturn(new PageImpl<>(List.of()));
		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

		mockMvc.perform(get("/api/consultas")
						.with(user(veterinario()))
						.param("page", "0")
						.param("size", "20"))
				.andExpect(status().isOk());

		verify(consultaService).listarAutorizado(isNull(), isNull(), isNull(), isNull(), isNull(), pageableCaptor.capture(), any(UsuarioPrincipal.class));
		assertEquals(0, pageableCaptor.getValue().getPageNumber());
		assertEquals(20, pageableCaptor.getValue().getPageSize());
		assertFalse(pageableCaptor.getValue().getSort().isSorted());
	}

	@Test
	void consultaListaAceitaSortSpringDataPadrao() throws Exception {
		when(consultaService.listarAutorizado(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class), any(UsuarioPrincipal.class)))
				.thenReturn(new PageImpl<>(List.of()));
		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

		mockMvc.perform(get("/api/consultas")
						.with(user(veterinario()))
						.param("page", "0")
						.param("size", "20")
						.param("sort", "dataHora,desc"))
				.andExpect(status().isOk());

		verify(consultaService).listarAutorizado(isNull(), isNull(), isNull(), isNull(), isNull(), pageableCaptor.capture(), any(UsuarioPrincipal.class));
		assertEquals(0, pageableCaptor.getValue().getPageNumber());
		assertEquals(20, pageableCaptor.getValue().getPageSize());
		assertEquals("dataHora: DESC", pageableCaptor.getValue().getSort().toString());
	}

	@Test
	void veterinarioCriaConsultaParaSiERecebeStatusAg() throws Exception {
		when(consultaService.criar(any(ConsultaRequest.class), any(UsuarioPrincipal.class))).thenReturn(new ConsultaResponse(
				100L,
				LocalDateTime.parse("2026-09-01T10:00:00"),
				"PRESENCIAL",
				"Avaliacao",
				null,
				null,
				null,
				null,
				"AG",
				"Agendada",
				50L,
				"Bilu",
				10L,
				"Dra Vera",
				30L,
				"Clinica ArkIve"
		));

		mockMvc.perform(post("/api/consultas")
						.with(user(veterinario()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "dataHora": "2026-09-01T10:00:00",
								  "modalidade": "PRESENCIAL",
								  "motivo": "Avaliacao",
								  "status": "AG",
								  "animalId": 50,
								  "veterinarioId": 10,
								  "clinicaId": 30
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(100))
				.andExpect(jsonPath("$.status").value("AG"));

		verify(consultaService).criar(
				argThat(request -> Long.valueOf(10L).equals(request.veterinarioId())),
				argThat(principal -> Long.valueOf(10L).equals(principal.getVeterinarioId()))
		);
	}

	@Test
	void inicioDeConsultaDeOutroVeterinarioRetorna403Json() throws Exception {
		when(consultaWorkflowService.iniciar(eq(100L), any(UsuarioPrincipal.class)))
				.thenThrow(new AccessDeniedException("Veterinario nao autorizado para esta consulta."));

		mockMvc.perform(post("/api/consultas/100/iniciar").with(user(veterinario())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.message").value("Veterinario nao autorizado para esta consulta."))
				.andExpect(jsonPath("$.path").value("/api/consultas/100/iniciar"));
	}

	@Test
	void estadoInvalidoNoFluxoRetorna409Json() throws Exception {
		when(consultaWorkflowService.iniciar(eq(100L), any(UsuarioPrincipal.class)))
				.thenThrow(new BusinessException("Somente consultas agendadas podem ser iniciadas.", HttpStatus.CONFLICT));

		mockMvc.perform(post("/api/consultas/100/iniciar").with(user(veterinario())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.message").value("Somente consultas agendadas podem ser iniciadas."))
				.andExpect(jsonPath("$.path").value("/api/consultas/100/iniciar"));
	}

	@Test
	void narrativaDeOutroVeterinarioRetorna403Json() throws Exception {
		when(consultaWorkflowService.atualizarNarrativa(eq(100L), any(AtualizarNarrativaConsultaRequest.class), any(UsuarioPrincipal.class)))
				.thenThrow(new AccessDeniedException("Veterinario nao autorizado para esta consulta."));

		mockMvc.perform(patch("/api/consultas/100/narrativa")
						.with(user(veterinario()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"narrativa\":\"Texto clinico\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.path").value("/api/consultas/100/narrativa"));
	}

	@Test
	void suporteClinicoBemSucedidoRetornaApSemExporContratoBrutoDoProvider() throws Exception {
		when(clinicalSupportService.gerarSuporte(eq(100L), any(UsuarioPrincipal.class))).thenReturn(new ClinicalSupportResponse(
				100L,
				"AP",
				"Aguardando Parecer",
				"Entorse leve",
				"LEVE",
				"Avaliar apoio, dor e amplitude articular.",
				72,
				List.of("https://source-one.example", "https://source-two.example")
		));

		mockMvc.perform(post("/api/consultas/100/suporte-clinico").with(user(veterinario())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.statusConsulta").value("AP"))
				.andExpect(jsonPath("$.hipoteseDiagnostica").value("Entorse leve"))
				.andExpect(jsonPath("$.confianca").value(72))
				.andExpect(jsonPath("$.fontesPesquisadas[0]").value("https://source-one.example"))
				.andExpect(jsonPath("$.fontesPesquisadas[1]").value("https://source-two.example"));
	}

	@Test
	void buscaSuporteClinicoPersistidoExpoeFontesPesquisadas() throws Exception {
		when(clinicalSupportService.buscarSuporte(eq(100L), any(UsuarioPrincipal.class))).thenReturn(new ClinicalSupportResponse(
				100L,
				"AP",
				"Aguardando Parecer",
				"Entorse leve",
				"LEVE",
				"Avaliar apoio, dor e amplitude articular.",
				72,
				List.of("https://source-one.example", "https://source-two.example")
		));

		mockMvc.perform(get("/api/consultas/100/suporte-clinico").with(user(veterinario())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fontesPesquisadas[0]").value("https://source-one.example"))
				.andExpect(jsonPath("$.fontesPesquisadas[1]").value("https://source-two.example"));
	}

	@Test
	void resumoPdfDeConsultaFinalizadaRetornaPdfComHeadersPrivados() throws Exception {
		byte[] pdf = "%PDF-arkive-test".getBytes();
		when(consultaResumoPdfService.gerarResumo(eq(100L), any(UsuarioPrincipal.class)))
				.thenReturn(new ConsultaResumoPdfService.ConsultaResumoPdf(pdf, "arkive-consulta-bilu-100.pdf"));

		mockMvc.perform(get("/api/consultas/100/resumo-pdf").with(user(veterinario())))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
				.andExpect(content().bytes(pdf))
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"arkive-consulta-bilu-100.pdf\""))
				.andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));
	}

	@Test
	void resumoPdfRejeitaConsultaNaoFinalizada() throws Exception {
		when(consultaResumoPdfService.gerarResumo(eq(100L), any(UsuarioPrincipal.class)))
				.thenThrow(new BusinessException("Resumo em PDF disponivel apenas para consulta finalizada.", HttpStatus.CONFLICT));

		mockMvc.perform(get("/api/consultas/100/resumo-pdf").with(user(veterinario())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Resumo em PDF disponivel apenas para consulta finalizada."));
	}

	@Test
	void resumoPdfRejeitaVeterinarioNaoAutorizado() throws Exception {
		when(consultaResumoPdfService.gerarResumo(eq(100L), any(UsuarioPrincipal.class)))
				.thenThrow(new AccessDeniedException("Usuario nao autorizado para esta consulta."));

		mockMvc.perform(get("/api/consultas/100/resumo-pdf").with(user(veterinario())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Usuario nao autorizado para esta consulta."));
	}

	@Test
	void adesaoNaoSuportaPutOuDeletePublicosERetorna405Json() throws Exception {
		mockMvc.perform(put("/api/adesoes-prescricao/80")
						.with(user(veterinario()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status").value(405))
				.andExpect(jsonPath("$.message").value("Metodo HTTP nao suportado para este recurso."))
				.andExpect(jsonPath("$.path").value("/api/adesoes-prescricao/80"));

		mockMvc.perform(delete("/api/adesoes-prescricao/80").with(user(veterinario())))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status").value(405))
				.andExpect(jsonPath("$.message").value("Metodo HTTP nao suportado para este recurso."))
				.andExpect(jsonPath("$.path").value("/api/adesoes-prescricao/80"));
	}

	private UsuarioPrincipal veterinario() {
		return new UsuarioPrincipal(1L, "Dra Vera", "vera@arkive.com", "$2a$10$hash", TipoUsuario.VETERINARIO, "S", false, null, 10L, null);
	}
}
