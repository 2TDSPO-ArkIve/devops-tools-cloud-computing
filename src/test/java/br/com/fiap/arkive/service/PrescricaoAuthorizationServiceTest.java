package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.PrescricaoRequest;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Prescricao;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.repository.AdesaoPrescricaoRepository;
import br.com.fiap.arkive.repository.PrescricaoRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrescricaoAuthorizationServiceTest {

	private PrescricaoRepository prescricaoRepository;
	private AdesaoPrescricaoRepository adesaoPrescricaoRepository;
	private ConsultaService consultaService;
	private EventoJornadaService eventoJornadaService;
	private ClinicalAccessService clinicalAccessService;
	private PrescricaoService prescricaoService;

	@BeforeEach
	void setUp() {
		prescricaoRepository = mock(PrescricaoRepository.class);
		adesaoPrescricaoRepository = mock(AdesaoPrescricaoRepository.class);
		consultaService = mock(ConsultaService.class);
		eventoJornadaService = mock(EventoJornadaService.class);
		clinicalAccessService = mock(ClinicalAccessService.class);
		prescricaoService = new PrescricaoService(
				prescricaoRepository,
				adesaoPrescricaoRepository,
				consultaService,
				eventoJornadaService,
				clinicalAccessService
		);
		when(prescricaoRepository.save(any(Prescricao.class))).thenAnswer(invocation -> {
			Prescricao prescricao = invocation.getArgument(0);
			prescricao.setId(9L);
			return prescricao;
		});
		when(eventoJornadaService.criarPayload(any(), any(), any())).thenReturn("{\"entity\":\"Prescricao\"}");
	}

	@Test
	void veterinarioCriaPrescricaoParaConsultaFinalizadaPropria() {
		Consulta consulta = consulta("FI", 10L, 30L);
		UsuarioPrincipal principal = veterinario(10L);
		when(consultaService.buscarEntidade(1L)).thenReturn(consulta);

		prescricaoService.criar(request(1L), principal);

		verify(clinicalAccessService).exigirEscritaPrescricaoVeterinario(eq(principal), any(Prescricao.class));
		verify(prescricaoRepository).save(any(Prescricao.class));
		verify(eventoJornadaService).registrarEvento(
				eq("PRESCRICAO_CRIADA"),
				eq("VETERINARIO"),
				eq(null),
				eq(10L),
				eq(50L),
				eq(30L),
				eq("Prescricao criada."),
				eq("{\"entity\":\"Prescricao\"}")
		);
	}

	@Test
	void outroVeterinarioNaoCriaPrescricao() {
		Consulta consulta = consulta("FI", 10L, 30L);
		UsuarioPrincipal principal = veterinario(22L);
		when(consultaService.buscarEntidade(1L)).thenReturn(consulta);
		doThrow(new AccessDeniedException("Veterinario nao autorizado para esta consulta."))
				.when(clinicalAccessService).exigirEscritaPrescricaoVeterinario(eq(principal), any(Prescricao.class));

		assertThrows(AccessDeniedException.class, () -> prescricaoService.criar(request(1L), principal));

		verify(prescricaoRepository, never()).save(any());
	}

	@Test
	void somenteVeterinarioCriaPrescricao() {
		assertPapelNaoPrescreve(responsavel(40L));
		assertPapelNaoPrescreve(adminClinica(30L));
		assertPapelNaoPrescreve(sysadmin());
	}

	@Test
	void criaPrescricaoSomenteParaConsultaFinalizada() {
		assertStatusNaoPermitePrescricao("AG");
		assertStatusNaoPermitePrescricao("EP");
		assertStatusNaoPermitePrescricao("AP");
		assertStatusNaoPermitePrescricao("CA");
	}

	@Test
	void leituraPorIdValidaEscopoDaPrescricao() {
		Prescricao prescricao = prescricao(consulta("FI", 10L, 30L));
		UsuarioPrincipal principal = veterinario(10L);
		when(prescricaoRepository.findById(9L)).thenReturn(Optional.of(prescricao));

		prescricaoService.buscarPorIdAutorizado(9L, principal);

		verify(clinicalAccessService).exigirLeituraPrescricao(principal, prescricao);
	}

	@Test
	void listagemDeVeterinarioUsaVeterinarioAutenticado() {
		when(prescricaoRepository.buscarParaVeterinario(eq(10L), eq(1L), eq("med"), any(Pageable.class)))
				.thenReturn(Page.empty());

		prescricaoService.listarAutorizado(1L, "med", Pageable.unpaged(), veterinario(10L));

		verify(prescricaoRepository).buscarParaVeterinario(eq(10L), eq(1L), eq("med"), any(Pageable.class));
		verify(prescricaoRepository, never()).buscar(any(), any(), any());
	}

	@Test
	void listagemDeResponsavelUsaResponsavelAutenticado() {
		when(prescricaoRepository.buscarParaResponsavel(eq(40L), any(LocalDate.class), eq(null), eq(null), any(Pageable.class)))
				.thenReturn(Page.empty());

		prescricaoService.listarAutorizado(null, null, Pageable.unpaged(), responsavel(40L));

		verify(prescricaoRepository).buscarParaResponsavel(eq(40L), any(LocalDate.class), eq(null), eq(null), any(Pageable.class));
	}

	@Test
	void adminClinicaLeSomenteEscopoDaClinicaESysadminLeGlobal() {
		when(prescricaoRepository.buscarParaClinica(eq(30L), eq(null), eq(null), any(Pageable.class))).thenReturn(Page.empty());
		when(prescricaoRepository.buscar(eq(null), eq(null), any(Pageable.class))).thenReturn(Page.empty());

		prescricaoService.listarAutorizado(null, null, Pageable.unpaged(), adminClinica(30L));
		prescricaoService.listarAutorizado(null, null, Pageable.unpaged(), sysadmin());

		verify(prescricaoRepository).buscarParaClinica(eq(30L), eq(null), eq(null), any(Pageable.class));
		verify(prescricaoRepository).buscar(eq(null), eq(null), any(Pageable.class));
	}

	@Test
	void atualizacaoNaoMovePrescricaoParaOutraConsulta() {
		Prescricao prescricao = prescricao(consulta("FI", 10L, 30L));
		when(prescricaoRepository.findById(9L)).thenReturn(Optional.of(prescricao));

		BusinessException exception = assertThrows(BusinessException.class,
				() -> prescricaoService.atualizar(9L, request(2L), veterinario(10L)));

		assertEquals(HttpStatus.CONFLICT, exception.getStatus());
		verify(prescricaoRepository, never()).save(any());
	}

	@Test
	void atualizacaoBloqueadaQuandoExisteAdesao() {
		Prescricao prescricao = prescricao(consulta("FI", 10L, 30L));
		when(prescricaoRepository.findById(9L)).thenReturn(Optional.of(prescricao));
		when(adesaoPrescricaoRepository.existsByPrescricaoId(9L)).thenReturn(true);

		BusinessException exception = assertThrows(BusinessException.class,
				() -> prescricaoService.atualizar(9L, request(1L), veterinario(10L)));

		assertEquals(HttpStatus.CONFLICT, exception.getStatus());
		verify(prescricaoRepository, never()).save(any());
	}

	@Test
	void exclusaoBloqueadaQuandoExisteAdesao() {
		Prescricao prescricao = prescricao(consulta("FI", 10L, 30L));
		when(prescricaoRepository.findById(9L)).thenReturn(Optional.of(prescricao));
		when(adesaoPrescricaoRepository.existsByPrescricaoId(9L)).thenReturn(true);

		BusinessException exception = assertThrows(BusinessException.class,
				() -> prescricaoService.excluir(9L, veterinario(10L)));

		assertEquals(HttpStatus.CONFLICT, exception.getStatus());
		verify(prescricaoRepository, never()).delete(any());
	}

	@Test
	void sobrecargasGenericasDeEscritaExigemPrincipal() {
		assertThrows(AccessDeniedException.class, () -> prescricaoService.criar(request(1L)));
		assertThrows(AccessDeniedException.class, () -> prescricaoService.atualizar(9L, request(1L)));
		assertThrows(AccessDeniedException.class, () -> prescricaoService.excluir(9L));
	}

	private void assertPapelNaoPrescreve(UsuarioPrincipal principal) {
		Consulta consulta = consulta("FI", 10L, 30L);
		when(consultaService.buscarEntidade(1L)).thenReturn(consulta);
		doThrow(new AccessDeniedException("Operacao clinica permitida apenas ao veterinario responsavel."))
				.when(clinicalAccessService).exigirEscritaPrescricaoVeterinario(eq(principal), any(Prescricao.class));

		assertThrows(AccessDeniedException.class, () -> prescricaoService.criar(request(1L), principal));

		verify(prescricaoRepository, never()).save(any());
	}

	private void assertStatusNaoPermitePrescricao(String status) {
		Consulta consulta = consulta(status, 10L, 30L);
		when(consultaService.buscarEntidade(1L)).thenReturn(consulta);

		BusinessException exception = assertThrows(BusinessException.class,
				() -> prescricaoService.criar(request(1L), veterinario(10L)));

		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
		verify(prescricaoRepository, never()).save(any());
	}

	private PrescricaoRequest request(Long consultaId) {
		return new PrescricaoRequest("Medicamento", "1 comprimido", "12/12h", "ORAL", LocalDate.now(), null, "Com alimento", consultaId);
	}

	private Prescricao prescricao(Consulta consulta) {
		Prescricao prescricao = new Prescricao();
		prescricao.setId(9L);
		prescricao.setMedicamento("Medicamento");
		prescricao.setDosagem("1 comprimido");
		prescricao.setDataInicio(LocalDate.now());
		prescricao.setConsulta(consulta);
		return prescricao;
	}

	private Consulta consulta(String status, Long veterinarioId, Long clinicaId) {
		Animal animal = new Animal();
		animal.setId(50L);
		animal.setNome("Bilu");
		Veterinario veterinario = new Veterinario();
		veterinario.setId(veterinarioId);
		Consulta consulta = new Consulta();
		consulta.setId(1L);
		consulta.setStatus(status);
		consulta.setAnimal(animal);
		consulta.setVeterinario(veterinario);
		if (clinicaId != null) {
			Clinica clinica = new Clinica();
			clinica.setId(clinicaId);
			consulta.setClinica(clinica);
		}
		return consulta;
	}

	private UsuarioPrincipal veterinario(Long veterinarioId) {
		return new UsuarioPrincipal(1L, "Dra", "vet@arkive.com", "$2a$10$hash", TipoUsuario.VETERINARIO, "S", false, null, veterinarioId, null);
	}

	private UsuarioPrincipal responsavel(Long responsavelId) {
		return new UsuarioPrincipal(2L, "Tutor", "tutor@arkive.com", "$2a$10$hash", TipoUsuario.RESPONSAVEL, "S", false, responsavelId, null, null);
	}

	private UsuarioPrincipal adminClinica(Long clinicaId) {
		return new UsuarioPrincipal(3L, "Admin", "admin@arkive.com", "$2a$10$hash", TipoUsuario.ADMIN_CLINICA, "S", false, null, null, clinicaId);
	}

	private UsuarioPrincipal sysadmin() {
		return new UsuarioPrincipal(4L, "Sys", "sys@arkive.com", "$2a$10$hash", TipoUsuario.SYSADMIN, "S", false, null, null, null);
	}

}
