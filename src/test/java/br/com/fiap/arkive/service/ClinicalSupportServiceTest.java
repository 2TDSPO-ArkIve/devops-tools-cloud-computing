package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.response.ClinicalSupportResponse;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Diagnostico;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.clinical.ClinicalSupportProvider;
import br.com.fiap.arkive.service.clinical.ClinicalSupportProviderResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClinicalSupportServiceTest {

	private ConsultaService consultaService;
	private DiagnosticoService diagnosticoService;
	private ClinicalAccessService clinicalAccessService;
	private ClinicalSupportProvider clinicalSupportProvider;
	private ClinicalSupportPersistenceService persistenceService;
	private ClinicalSupportService clinicalSupportService;

	@BeforeEach
	void setUp() {
		consultaService = mock(ConsultaService.class);
		diagnosticoService = mock(DiagnosticoService.class);
		clinicalAccessService = mock(ClinicalAccessService.class);
		clinicalSupportProvider = mock(ClinicalSupportProvider.class);
		persistenceService = mock(ClinicalSupportPersistenceService.class);
		clinicalSupportService = new ClinicalSupportService(
				consultaService,
				diagnosticoService,
				clinicalAccessService,
				clinicalSupportProvider,
				persistenceService
		);
	}

	@Test
	void consultaEpDoVeterinarioChamaProviderUmaVezEPersisteResposta() {
		Consulta consulta = consulta("EP", 10L);
		ClinicalSupportResponse esperado = response(consulta, diagnosticoIa(consulta));
		UsuarioPrincipal principal = veterinario(10L);
		when(consultaService.buscarEntidade(6L)).thenReturn(consulta);
		when(clinicalSupportProvider.gerarSuporte(6L)).thenReturn(result());
		when(persistenceService.persistirSuporte(6L, result(), principal)).thenReturn(esperado);

		ClinicalSupportResponse response = clinicalSupportService.gerarSuporte(6L, principal);

		verify(clinicalAccessService).exigirEscritaClinicaVeterinario(principal, consulta);
		verify(clinicalSupportProvider).gerarSuporte(6L);
		verify(persistenceService).persistirSuporte(6L, result(), principal);
		assertEquals(esperado, response);
	}

	@Test
	void suporteExistenteEmApRetornaPersistidoSemChamarProvider() {
		Consulta consulta = consulta("AP", 10L);
		Diagnostico suporte = diagnosticoIa(consulta);
		when(consultaService.buscarEntidade(6L)).thenReturn(consulta);
		when(diagnosticoService.buscarSuporteClinico(6L)).thenReturn(suporte);

		ClinicalSupportResponse response = clinicalSupportService.gerarSuporte(6L, veterinario(10L));

		assertEquals("AP", response.statusConsulta());
		verify(clinicalSupportProvider, never()).gerarSuporte(any());
		verify(persistenceService, never()).persistirSuporte(any(), any(), any());
	}

	@Test
	void outroVeterinarioNaoChamaProvider() {
		Consulta consulta = consulta("EP", 10L);
		UsuarioPrincipal principal = veterinario(22L);
		when(consultaService.buscarEntidade(6L)).thenReturn(consulta);
		doThrow(new AccessDeniedException("Veterinario nao autorizado para esta consulta."))
				.when(clinicalAccessService).exigirEscritaClinicaVeterinario(principal, consulta);

		assertThrows(AccessDeniedException.class, () -> clinicalSupportService.gerarSuporte(6L, principal));

		verify(clinicalSupportProvider, never()).gerarSuporte(any());
	}

	@Test
	void estadosInvalidosNaoChamamProvider() {
		assertEstadoInvalidoNaoChamaProvider("AG");
		assertEstadoInvalidoNaoChamaProvider("FI");
		assertEstadoInvalidoNaoChamaProvider("CA");
	}

	@Test
	void falhaDoProviderNaoPersiste() {
		Consulta consulta = consulta("EP", 10L);
		when(consultaService.buscarEntidade(6L)).thenReturn(consulta);
		when(clinicalSupportProvider.gerarSuporte(6L))
				.thenThrow(new BusinessException("Motor clinico temporariamente indisponivel.", HttpStatus.SERVICE_UNAVAILABLE));

		BusinessException exception = assertThrows(BusinessException.class, () -> clinicalSupportService.gerarSuporte(6L, veterinario(10L)));

		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
		verify(persistenceService, never()).persistirSuporte(any(), any(), any());
	}

	@Test
	void respostaMalformadaNaoPersiste() {
		Consulta consulta = consulta("EP", 10L);
		when(consultaService.buscarEntidade(6L)).thenReturn(consulta);
		when(clinicalSupportProvider.gerarSuporte(6L))
				.thenReturn(new ClinicalSupportProviderResult("", "MODERADA", "Insight", 65));

		BusinessException exception = assertThrows(BusinessException.class, () -> clinicalSupportService.gerarSuporte(6L, veterinario(10L)));

		assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
		verify(persistenceService, never()).persistirSuporte(any(), any(), any());
	}

	@Test
	void consultaEmProcessamentoNaoChamaProviderDuasVezes() throws Exception {
		Consulta consulta = consulta("EP", 10L);
		UsuarioPrincipal principal = veterinario(10L);
		CountDownLatch providerIniciado = new CountDownLatch(1);
		CountDownLatch liberarProvider = new CountDownLatch(1);
		when(consultaService.buscarEntidade(6L)).thenReturn(consulta);
		when(clinicalSupportProvider.gerarSuporte(6L)).thenAnswer(invocation -> {
			providerIniciado.countDown();
			assertTrue(liberarProvider.await(2, TimeUnit.SECONDS));
			return result();
		});
		when(persistenceService.persistirSuporte(6L, result(), principal)).thenReturn(response(consulta, diagnosticoIa(consulta)));

		var executor = Executors.newSingleThreadExecutor();
		try {
			var primeiraRequisicao = executor.submit(() -> clinicalSupportService.gerarSuporte(6L, principal));
			assertTrue(providerIniciado.await(2, TimeUnit.SECONDS));

			BusinessException exception = assertThrows(BusinessException.class, () -> clinicalSupportService.gerarSuporte(6L, principal));

			assertEquals(HttpStatus.CONFLICT, exception.getStatus());
			liberarProvider.countDown();
			primeiraRequisicao.get(2, TimeUnit.SECONDS);
		} finally {
			liberarProvider.countDown();
			executor.shutdownNow();
		}
		verify(clinicalSupportProvider, times(1)).gerarSuporte(6L);
	}

	@Test
	void getSuporteRetornaPersistidoSemProvider() {
		Consulta consulta = consulta("AP", 10L);
		Diagnostico suporte = diagnosticoIa(consulta);
		when(consultaService.buscarEntidade(6L)).thenReturn(consulta);
		when(diagnosticoService.buscarSuporteClinico(6L)).thenReturn(suporte);

		ClinicalSupportResponse response = clinicalSupportService.buscarSuporte(6L, veterinario(10L));

		assertEquals("Hipotese", response.hipoteseDiagnostica());
		verify(clinicalSupportProvider, never()).gerarSuporte(any());
	}

	@Test
	void getSuporteAusenteRetornaErroControlado() {
		Consulta consulta = consulta("AP", 10L);
		when(consultaService.buscarEntidade(6L)).thenReturn(consulta);
		when(diagnosticoService.buscarSuporteClinico(6L))
				.thenThrow(new ResourceNotFoundException("Suporte clinico ainda nao gerado para esta consulta."));

		assertThrows(ResourceNotFoundException.class, () -> clinicalSupportService.buscarSuporte(6L, veterinario(10L)));

		verify(clinicalSupportProvider, never()).gerarSuporte(any());
	}

	@Test
	void getSuporteDeOutroVeterinarioRetorna403SemProvider() {
		Consulta consulta = consulta("AP", 10L);
		UsuarioPrincipal principal = veterinario(22L);
		when(consultaService.buscarEntidade(6L)).thenReturn(consulta);
		doThrow(new AccessDeniedException("Veterinario nao autorizado para esta consulta."))
				.when(clinicalAccessService).exigirEscritaClinicaVeterinario(principal, consulta);

		assertThrows(AccessDeniedException.class, () -> clinicalSupportService.buscarSuporte(6L, principal));

		verify(clinicalSupportProvider, never()).gerarSuporte(any());
	}

	private void assertEstadoInvalidoNaoChamaProvider(String status) {
		Consulta consulta = consulta(status, 10L);
		when(consultaService.buscarEntidade(6L)).thenReturn(consulta);

		assertThrows(BusinessException.class, () -> clinicalSupportService.gerarSuporte(6L, veterinario(10L)));

		verify(clinicalSupportProvider, never()).gerarSuporte(any());
	}

	private ClinicalSupportProviderResult result() {
		return new ClinicalSupportProviderResult("Hipotese", "MODERADA", "Insight clinico", 65);
	}

	private ClinicalSupportResponse response(Consulta consulta, Diagnostico diagnostico) {
		Consulta consultaAguardandoParecer = consulta("AP", consulta.getVeterinario().getId());
		return ClinicalSupportResponse.fromEntities(consultaAguardandoParecer, diagnostico);
	}

	private Diagnostico diagnosticoIa(Consulta consulta) {
		Diagnostico diagnostico = new Diagnostico();
		diagnostico.setId(50L);
		diagnostico.setDiagnostico("Hipotese");
		diagnostico.setSeveridade("MODERADA");
		diagnostico.setInsightIa("Insight clinico");
		diagnostico.setConfianca(BigDecimal.valueOf(65));
		diagnostico.setConfirmado("N");
		diagnostico.setValidacaoVet("N");
		diagnostico.setConsulta(consulta);
		return diagnostico;
	}

	private Consulta consulta(String status, Long veterinarioId) {
		Animal animal = new Animal();
		animal.setId(99L);
		animal.setNome("Bilu");
		Veterinario veterinario = new Veterinario();
		veterinario.setId(veterinarioId);
		veterinario.setNome("Dra Vera");
		Consulta consulta = new Consulta();
		consulta.setId(6L);
		consulta.setStatus(status);
		consulta.setAnimal(animal);
		consulta.setVeterinario(veterinario);
		return consulta;
	}

	private UsuarioPrincipal veterinario(Long veterinarioId) {
		return new UsuarioPrincipal(1L, "Dra", "vet@arkive.com", "$2a$10$hash", TipoUsuario.VETERINARIO, "S", false, null, veterinarioId, null);
	}
}
