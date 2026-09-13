package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.AtualizarNarrativaConsultaRequest;
import br.com.fiap.arkive.dto.request.CancelarConsultaRequest;
import br.com.fiap.arkive.dto.request.FinalizarConsultaRequest;
import br.com.fiap.arkive.dto.response.ConsultaWorkflowResponse;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Diagnostico;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.ConsultaRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.entity.TipoUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultaWorkflowServiceTest {

	private ConsultaService consultaService;
	private ConsultaRepository consultaRepository;
	private DiagnosticoService diagnosticoService;
	private EventoJornadaService eventoJornadaService;
	private ClinicalAccessService clinicalAccessService;
	private ConsultaWorkflowService workflowService;

	@BeforeEach
	void setUp() {
		consultaService = mock(ConsultaService.class);
		consultaRepository = mock(ConsultaRepository.class);
		diagnosticoService = mock(DiagnosticoService.class);
		eventoJornadaService = mock(EventoJornadaService.class);
		clinicalAccessService = mock(ClinicalAccessService.class);
		workflowService = new ConsultaWorkflowService(
				consultaService,
				consultaRepository,
				diagnosticoService,
				eventoJornadaService,
				clinicalAccessService
		);
		when(consultaRepository.save(any(Consulta.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(eventoJornadaService.criarPayload(any(), any(), any())).thenReturn("{\"entity\":\"Consulta\"}");
	}

	@Test
	void iniciaConsultaAgendada() {
		Consulta consulta = consulta("AG");
		when(consultaService.buscarEntidade(1L)).thenReturn(consulta);

		ConsultaWorkflowResponse response = workflowService.iniciar(1L);

		assertEquals("EP", consulta.getStatus());
		assertEquals("EP", response.status());
		assertEquals("Em Progresso", response.statusDescricao());
		verify(consultaRepository).save(consulta);
		verify(eventoJornadaService).registrarEvento(
				eq("CONSULTA_INICIADA"),
				eq("VETERINARIO"),
				eq(null),
				eq(20L),
				eq(10L),
				eq(30L),
				eq("Consulta iniciada."),
				any()
		);
	}

	@Test
	void rejeitaIniciarConsultaJaIniciadaOuTerminal() {
		assertInicioRejeitado("EP");
		assertInicioRejeitado("FI");
		assertInicioRejeitado("CA");
	}

	@Test
	void rejeitaInicioComRecursosInativos() {
		Consulta animalInativo = consulta("AG");
		animalInativo.getAnimal().setAtivo("N");
		assertInicioComConsultaRejeitado(animalInativo);

		Consulta veterinarioInativo = consulta("AG");
		veterinarioInativo.getVeterinario().setAtivo("N");
		assertInicioComConsultaRejeitado(veterinarioInativo);

		Consulta clinicaInativa = consulta("AG");
		clinicaInativa.getClinica().setAtivo("N");
		assertInicioComConsultaRejeitado(clinicaInativa);
	}

	@Test
	void atualizaNarrativaEmEpEAp() {
		Consulta emProgresso = consulta("EP");
		when(consultaService.buscarEntidade(1L)).thenReturn(emProgresso);

		workflowService.atualizarNarrativa(1L, new AtualizarNarrativaConsultaRequest("Relato clinico"));

		assertEquals("Relato clinico", emProgresso.getTranscricao());
		verify(consultaRepository).save(emProgresso);
		verifyNoEventoOuDiagnostico();

		Consulta aguardandoParecer = consulta("AP");
		when(consultaService.buscarEntidade(2L)).thenReturn(aguardandoParecer);

		workflowService.atualizarNarrativa(2L, new AtualizarNarrativaConsultaRequest("Evolucao do caso"));

		assertEquals("Evolucao do caso", aguardandoParecer.getTranscricao());
	}

	@Test
	void rejeitaNarrativaBlankAgFiECa() {
		Consulta ep = consulta("EP");
		when(consultaService.buscarEntidade(1L)).thenReturn(ep);
		assertThrows(BusinessException.class, () -> workflowService.atualizarNarrativa(1L, new AtualizarNarrativaConsultaRequest(" ")));

		assertNarrativaRejeitada("AG");
		assertNarrativaRejeitada("FI");
		assertNarrativaRejeitada("CA");
	}

	@Test
	void finalizaConsultaEmProgressoComDiagnosticoConfirmado() {
		Consulta consulta = consulta("EP");
		Diagnostico diagnostico = diagnostico(100L);
		when(consultaService.buscarEntidade(1L)).thenReturn(consulta);
		when(diagnosticoService.criarConfirmadoPeloVeterinario(consulta, "Gastrite", "LEVE", 5L)).thenReturn(diagnostico);

		ConsultaWorkflowResponse response = workflowService.finalizar(1L, new FinalizarConsultaRequest("Gastrite", "LEVE", 5L, "Alta com acompanhamento"));

		assertEquals("FI", consulta.getStatus());
		assertEquals("Finalizada", response.statusDescricao());
		assertEquals(100L, response.diagnosticoId());
		assertEquals("Alta com acompanhamento", consulta.getObservacao());
		verify(diagnosticoService).criarConfirmadoPeloVeterinario(consulta, "Gastrite", "LEVE", 5L);
		verify(eventoJornadaService).registrarEvento(
				eq("CONSULTA_FINALIZADA"),
				eq("VETERINARIO"),
				eq(null),
				eq(20L),
				eq(10L),
				eq(30L),
				eq("Consulta finalizada com diagnostico confirmado pelo veterinario."),
				any()
		);
	}

	@Test
	void finalizaConsultaAguardandoParecer() {
		Consulta consulta = consulta("AP");
		Diagnostico diagnostico = diagnostico(101L);
		when(consultaService.buscarEntidade(1L)).thenReturn(consulta);
		when(diagnosticoService.criarConfirmadoPeloVeterinario(consulta, "Dermatite", null, null)).thenReturn(diagnostico);

		ConsultaWorkflowResponse response = workflowService.finalizar(1L, new FinalizarConsultaRequest("Dermatite", null, null, "Retorno em 7 dias"));

		assertEquals("FI", consulta.getStatus());
		assertEquals(101L, response.diagnosticoId());
	}

	@Test
	void rejeitaFinalizacaoEmEstadosInvalidos() {
		assertFinalizacaoRejeitada("AG");
		assertFinalizacaoRejeitada("FI");
		assertFinalizacaoRejeitada("CA");
	}

	@Test
	void rejeitaFinalizacaoComDiagnosticoBlankOuDoencaInvalida() {
		Consulta consulta = consulta("EP");
		when(consultaService.buscarEntidade(1L)).thenReturn(consulta);

		assertThrows(BusinessException.class, () -> workflowService.finalizar(1L, new FinalizarConsultaRequest(" ", "LEVE", null, null)));
		verify(diagnosticoService, never()).criarConfirmadoPeloVeterinario(any(), any(), any(), any());

		Consulta outraConsulta = consulta("EP");
		when(consultaService.buscarEntidade(2L)).thenReturn(outraConsulta);
		when(diagnosticoService.criarConfirmadoPeloVeterinario(outraConsulta, "Diagnostico", null, 99L))
				.thenThrow(new ResourceNotFoundException("Doenca nao encontrada."));

		assertThrows(ResourceNotFoundException.class, () -> workflowService.finalizar(2L, new FinalizarConsultaRequest("Diagnostico", null, 99L, null)));
		assertEquals("EP", outraConsulta.getStatus());
		verify(eventoJornadaService, never()).registrarEvento(eq("CONSULTA_FINALIZADA"), any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void rejeitaFinalizacaoComRecursosInativos() {
		Consulta animalInativo = consulta("EP");
		animalInativo.getAnimal().setAtivo("N");
		assertFinalizacaoComConsultaRejeitada(animalInativo);

		Consulta veterinarioInativo = consulta("EP");
		veterinarioInativo.getVeterinario().setAtivo("N");
		assertFinalizacaoComConsultaRejeitada(veterinarioInativo);

		Consulta clinicaInativa = consulta("EP");
		clinicaInativa.getClinica().setAtivo("N");
		assertFinalizacaoComConsultaRejeitada(clinicaInativa);
	}

	@Test
	void propagaErroDeEventoParaRollbackTransacionalDaFinalizacao() {
		Consulta consulta = consulta("EP");
		Diagnostico diagnostico = diagnostico(100L);
		when(consultaService.buscarEntidade(1L)).thenReturn(consulta);
		when(diagnosticoService.criarConfirmadoPeloVeterinario(consulta, "Gastrite", null, null)).thenReturn(diagnostico);
		doThrow(new BusinessException("Evento invalido.")).when(eventoJornadaService)
				.registrarEvento(eq("CONSULTA_FINALIZADA"), any(), any(), any(), any(), any(), any(), any());

		assertThrows(BusinessException.class, () -> workflowService.finalizar(1L, new FinalizarConsultaRequest("Gastrite", null, null, null)));

		verify(consultaRepository).save(consulta);
		verify(diagnosticoService).criarConfirmadoPeloVeterinario(consulta, "Gastrite", null, null);
	}

	@Test
	void cancelaConsultaAgEpEAp() {
		assertCancelamento("AG");
		assertCancelamento("EP");
		assertCancelamento("AP");
	}

	@Test
	void veterinarioPodeExecutarFluxoDaPropriaConsulta() {
		Consulta consulta = consulta("AG");
		UsuarioPrincipal principal = veterinarioPrincipal(20L);
		when(consultaService.buscarEntidade(1L)).thenReturn(consulta);

		workflowService.iniciar(1L, principal);

		verify(clinicalAccessService).exigirEscritaClinicaVeterinario(principal, consulta);
		assertEquals("EP", consulta.getStatus());
	}

	@Test
	void veterinarioPodeAtualizarNarrativaDaPropriaConsulta() {
		Consulta consulta = consulta("EP");
		UsuarioPrincipal principal = veterinarioPrincipal(20L);
		when(consultaService.buscarEntidade(1L)).thenReturn(consulta);

		workflowService.atualizarNarrativa(1L, new AtualizarNarrativaConsultaRequest("Relato"), principal);

		verify(clinicalAccessService).exigirEscritaClinicaVeterinario(principal, consulta);
		assertEquals("Relato", consulta.getTranscricao());
	}

	@Test
	void veterinarioPodeFinalizarPropriaConsulta() {
		Consulta consulta = consulta("EP");
		Diagnostico diagnostico = diagnostico(100L);
		UsuarioPrincipal principal = veterinarioPrincipal(20L);
		when(consultaService.buscarEntidade(1L)).thenReturn(consulta);
		when(diagnosticoService.criarConfirmadoPeloVeterinario(consulta, "Gastrite", null, null)).thenReturn(diagnostico);

		workflowService.finalizar(1L, new FinalizarConsultaRequest("Gastrite", null, null, "Alta"), principal);

		verify(clinicalAccessService).exigirEscritaClinicaVeterinario(principal, consulta);
		assertEquals("FI", consulta.getStatus());
	}

	@Test
	void veterinarioPodeCancelarPropriaConsulta() {
		Consulta consulta = consulta("AG");
		UsuarioPrincipal principal = veterinarioPrincipal(20L);
		when(consultaService.buscarEntidade(1L)).thenReturn(consulta);

		workflowService.cancelar(1L, new CancelarConsultaRequest("Tutor solicitou"), principal);

		verify(clinicalAccessService).exigirEscritaClinicaVeterinario(principal, consulta);
		assertEquals("CA", consulta.getStatus());
	}

	@Test
	void veterinarioNaoExecutaFluxoDeConsultaDeOutroVeterinario() {
		Consulta consulta = consulta("AG");
		UsuarioPrincipal principal = veterinarioPrincipal(22L);
		when(consultaService.buscarEntidade(1L)).thenReturn(consulta);
		doThrow(new AccessDeniedException("Veterinario nao autorizado para esta consulta."))
				.when(clinicalAccessService).exigirEscritaClinicaVeterinario(principal, consulta);

		assertThrows(AccessDeniedException.class, () -> workflowService.iniciar(1L, principal));
		assertThrows(AccessDeniedException.class, () -> workflowService.atualizarNarrativa(1L, new AtualizarNarrativaConsultaRequest("Texto"), principal));
		assertThrows(AccessDeniedException.class, () -> workflowService.finalizar(1L, new FinalizarConsultaRequest("Diagnostico", null, null, null), principal));
		assertThrows(AccessDeniedException.class, () -> workflowService.cancelar(1L, new CancelarConsultaRequest("Motivo"), principal));

		verify(consultaRepository, never()).save(consulta);
	}

	@Test
	void rejeitaCancelamentoSemMotivoOuTerminal() {
		Consulta consulta = consulta("AG");
		when(consultaService.buscarEntidade(1L)).thenReturn(consulta);
		assertThrows(BusinessException.class, () -> workflowService.cancelar(1L, new CancelarConsultaRequest(" ")));

		assertCancelamentoRejeitado("FI");
		assertCancelamentoRejeitado("CA");
	}

	private void assertInicioRejeitado(String status) {
		Consulta consulta = consulta(status);
		when(consultaService.buscarEntidade(1L)).thenReturn(consulta);

		assertThrows(BusinessException.class, () -> workflowService.iniciar(1L));
	}

	private void assertInicioComConsultaRejeitado(Consulta consulta) {
		when(consultaService.buscarEntidade(1L)).thenReturn(consulta);

		assertThrows(BusinessException.class, () -> workflowService.iniciar(1L));
		verify(consultaRepository, never()).save(consulta);
	}

	private void assertNarrativaRejeitada(String status) {
		Consulta consulta = consulta(status);
		when(consultaService.buscarEntidade(1L)).thenReturn(consulta);

		assertThrows(BusinessException.class, () -> workflowService.atualizarNarrativa(1L, new AtualizarNarrativaConsultaRequest("Texto")));
	}

	private void assertFinalizacaoRejeitada(String status) {
		Consulta consulta = consulta(status);
		when(consultaService.buscarEntidade(1L)).thenReturn(consulta);

		assertThrows(BusinessException.class, () -> workflowService.finalizar(1L, new FinalizarConsultaRequest("Diagnostico", null, null, null)));
	}

	private void assertFinalizacaoComConsultaRejeitada(Consulta consulta) {
		when(consultaService.buscarEntidade(1L)).thenReturn(consulta);

		assertThrows(BusinessException.class, () -> workflowService.finalizar(1L, new FinalizarConsultaRequest("Diagnostico", null, null, null)));
		verify(diagnosticoService, never()).criarConfirmadoPeloVeterinario(eq(consulta), any(), any(), any());
	}

	private void assertCancelamento(String status) {
		Consulta consulta = consulta(status);
		when(consultaService.buscarEntidade(1L)).thenReturn(consulta);

		ConsultaWorkflowResponse response = workflowService.cancelar(1L, new CancelarConsultaRequest("Tutor solicitou remarcacao"));

		assertEquals("CA", consulta.getStatus());
		assertEquals("Cancelada", response.statusDescricao());
		assertEquals("Cancelamento: Tutor solicitou remarcacao", consulta.getObservacao());
		verify(eventoJornadaService, atLeastOnce()).registrarEvento(
				eq("CONSULTA_CANCELADA"),
				eq("VETERINARIO"),
				eq(null),
				eq(20L),
				eq(10L),
				eq(30L),
				eq("Consulta cancelada."),
				any()
		);
	}

	private void assertCancelamentoRejeitado(String status) {
		Consulta consulta = consulta(status);
		when(consultaService.buscarEntidade(1L)).thenReturn(consulta);

		assertThrows(BusinessException.class, () -> workflowService.cancelar(1L, new CancelarConsultaRequest("Tutor solicitou")));
	}

	private void verifyNoEventoOuDiagnostico() {
		verify(diagnosticoService, never()).criarConfirmadoPeloVeterinario(any(), any(), any(), any());
		verify(eventoJornadaService, never()).registrarEvento(any(), any(), any(), any(), any(), any(), any(), any());
	}

	private Consulta consulta(String status) {
		Animal animal = new Animal();
		animal.setId(10L);
		animal.setNome("Nina");
		animal.setAtivo("S");

		Veterinario veterinario = new Veterinario();
		veterinario.setId(20L);
		veterinario.setNome("Dra Vera");
		veterinario.setAtivo("S");

		Clinica clinica = new Clinica();
		clinica.setId(30L);
		clinica.setNome("Clinica Arkive");
		clinica.setAtivo("S");

		Consulta consulta = new Consulta();
		consulta.setId(1L);
		consulta.setStatus(status);
		consulta.setAnimal(animal);
		consulta.setVeterinario(veterinario);
		consulta.setClinica(clinica);
		consulta.setModalidade("PRESENCIAL");
		consulta.setMotivo("Check-up");
		return consulta;
	}

	private Diagnostico diagnostico(Long id) {
		Diagnostico diagnostico = new Diagnostico();
		diagnostico.setId(id);
		return diagnostico;
	}

	private UsuarioPrincipal veterinarioPrincipal(Long veterinarioId) {
		return new UsuarioPrincipal(1L, "Dra Vera", "vera@arkive.com", "$2a$10$hash", TipoUsuario.VETERINARIO, "S", false, null, veterinarioId, null);
	}
}
