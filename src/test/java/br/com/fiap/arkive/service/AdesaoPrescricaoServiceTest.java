package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.AdesaoPrescricaoRequest;
import br.com.fiap.arkive.dto.request.RegistrarAdesaoPrescricaoRequest;
import br.com.fiap.arkive.entity.AdesaoPrescricao;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Prescricao;
import br.com.fiap.arkive.entity.Responsavel;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.repository.AdesaoPrescricaoRepository;
import br.com.fiap.arkive.repository.AnimalRepository;
import br.com.fiap.arkive.repository.ResponsavelRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdesaoPrescricaoServiceTest {

	private AdesaoPrescricaoRepository adesaoPrescricaoRepository;
	private PrescricaoService prescricaoService;
	private ResponsavelRepository responsavelRepository;
	private EventoJornadaService eventoJornadaService;
	private ClinicalAccessService clinicalAccessService;
	private AdesaoPrescricaoService adesaoPrescricaoService;

	@BeforeEach
	void setUp() {
		adesaoPrescricaoRepository = mock(AdesaoPrescricaoRepository.class);
		prescricaoService = mock(PrescricaoService.class);
		responsavelRepository = mock(ResponsavelRepository.class);
		eventoJornadaService = mock(EventoJornadaService.class);
		clinicalAccessService = mock(ClinicalAccessService.class);
		adesaoPrescricaoService = new AdesaoPrescricaoService(
				adesaoPrescricaoRepository,
				prescricaoService,
				mock(AnimalRepository.class),
				responsavelRepository,
				eventoJornadaService,
				clinicalAccessService
		);
		when(adesaoPrescricaoRepository.save(any(AdesaoPrescricao.class))).thenAnswer(invocation -> {
			AdesaoPrescricao adesao = invocation.getArgument(0);
			adesao.setId(80L);
			return adesao;
		});
		when(eventoJornadaService.criarPayload(any(), any(), any())).thenReturn("{\"entity\":\"AdesaoPrescricao\"}");
	}

	@Test
	void responsavelRegistraAdesaoSParaAnimalVinculado() {
		registrarAdesao("S");
	}

	@Test
	void responsavelRegistraAdesaoNComoValorValido() {
		registrarAdesao("N");
	}

	@Test
	void responsavelAnimalEDataRegistroSaoDerivadosDoServidor() {
		Prescricao prescricao = prescricao(periodo(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)));
		Responsavel responsavel = responsavelEntity(40L);
		when(prescricaoService.buscarEntidade(9L)).thenReturn(prescricao);
		when(responsavelRepository.findById(40L)).thenReturn(Optional.of(responsavel));

		adesaoPrescricaoService.registrar(new RegistrarAdesaoPrescricaoRequest(9L, "S", "ok"), responsavel(40L));

		verify(adesaoPrescricaoRepository).save(argThat(adesao ->
				adesao.getResponsavel().getId().equals(40L)
						&& adesao.getAnimal().getId().equals(50L)
						&& adesao.getDataRegistro() != null
						&& "S".equals(adesao.getTomou())
						&& "ok".equals(adesao.getObservacao())
		));
	}

	@Test
	void timestampDoClienteNaoExisteNoDtoPublicoENaoControlaPersistencia() {
		Prescricao prescricao = prescricao(periodo(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)));
		LocalDateTime instanteClienteLegado = LocalDateTime.now().minusYears(1);
		when(prescricaoService.buscarEntidade(9L)).thenReturn(prescricao);
		when(responsavelRepository.findById(40L)).thenReturn(Optional.of(responsavelEntity(40L)));

		adesaoPrescricaoService.registrar(new RegistrarAdesaoPrescricaoRequest(9L, "S", "ok"), responsavel(40L));

		verify(adesaoPrescricaoRepository).save(argThat(adesao -> {
			assertNotNull(adesao.getDataRegistro());
			assertNotEquals(instanteClienteLegado.toLocalDate(), adesao.getDataRegistro().toLocalDate());
			return true;
		}));
	}

	@Test
	void responsavelNaoRegistraParaAnimalNaoVinculado() {
		Prescricao prescricao = prescricao(periodo(LocalDate.now(), null));
		UsuarioPrincipal principal = responsavel(41L);
		when(prescricaoService.buscarEntidade(9L)).thenReturn(prescricao);
		doThrow(new AccessDeniedException("Responsavel nao autorizado para registrar adesao desta prescricao."))
				.when(clinicalAccessService).exigirRegistroAdesaoResponsavel(principal, prescricao);

		assertThrows(AccessDeniedException.class,
				() -> adesaoPrescricaoService.registrar(new RegistrarAdesaoPrescricaoRequest(9L, "S", null), principal));

		verify(adesaoPrescricaoRepository, never()).save(any());
	}

	@Test
	void somenteResponsavelRegistraAdesao() {
		assertPapelNaoRegistraAdesao(veterinario(10L));
		assertPapelNaoRegistraAdesao(adminClinica(30L));
		assertPapelNaoRegistraAdesao(sysadmin());
	}

	@Test
	void naoRegistraAntesDoInicioDaPrescricao() {
		Prescricao prescricao = prescricao(periodo(LocalDate.now().plusDays(1), null));
		when(prescricaoService.buscarEntidade(9L)).thenReturn(prescricao);

		BusinessException exception = assertThrows(BusinessException.class,
				() -> adesaoPrescricaoService.registrar(new RegistrarAdesaoPrescricaoRequest(9L, "S", null), responsavel(40L)));

		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
		verify(adesaoPrescricaoRepository, never()).save(any());
	}

	@Test
	void naoRegistraAposFimDaPrescricao() {
		Prescricao prescricao = prescricao(periodo(LocalDate.now().minusDays(2), LocalDate.now().minusDays(1)));
		when(prescricaoService.buscarEntidade(9L)).thenReturn(prescricao);

		BusinessException exception = assertThrows(BusinessException.class,
				() -> adesaoPrescricaoService.registrar(new RegistrarAdesaoPrescricaoRequest(9L, "S", null), responsavel(40L)));

		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
		verify(adesaoPrescricaoRepository, never()).save(any());
	}

	@Test
	void leituraPorIdValidaEscopoDaAdesao() {
		AdesaoPrescricao adesao = adesao(responsavelEntity(40L), prescricao(periodo(LocalDate.now(), null)));
		UsuarioPrincipal principal = responsavel(40L);
		when(adesaoPrescricaoRepository.findById(80L)).thenReturn(Optional.of(adesao));

		adesaoPrescricaoService.buscarPorIdAutorizado(80L, principal);

		verify(clinicalAccessService).exigirLeituraAdesaoPrescricao(principal, adesao);
	}

	@Test
	void listagemDeResponsavelMantemPrivacidadeDoResponsavelAutenticado() {
		when(adesaoPrescricaoRepository.buscarParaResponsavel(eq(40L), any(LocalDate.class), eq(9L), eq(null), eq(40L), eq("S"), any(Pageable.class)))
				.thenReturn(Page.empty());

		adesaoPrescricaoService.listarAutorizado(9L, null, 40L, "S", Pageable.unpaged(), responsavel(40L));

		verify(adesaoPrescricaoRepository).buscarParaResponsavel(eq(40L), any(LocalDate.class), eq(9L), eq(null), eq(40L), eq("S"), any(Pageable.class));
	}

	@Test
	void veterinarioAdminClinicaESysadminPossuemLeiturasEscopadas() {
		when(adesaoPrescricaoRepository.buscarParaVeterinario(eq(10L), eq(null), eq(null), eq(null), eq(null), any(Pageable.class)))
				.thenReturn(Page.empty());
		when(adesaoPrescricaoRepository.buscarParaClinica(eq(30L), eq(null), eq(null), eq(null), eq(null), any(Pageable.class)))
				.thenReturn(Page.empty());
		when(adesaoPrescricaoRepository.buscar(eq(null), eq(null), eq(null), eq(null), any(Pageable.class))).thenReturn(Page.empty());

		adesaoPrescricaoService.listarAutorizado(null, null, null, null, Pageable.unpaged(), veterinario(10L));
		adesaoPrescricaoService.listarAutorizado(null, null, null, null, Pageable.unpaged(), adminClinica(30L));
		adesaoPrescricaoService.listarAutorizado(null, null, null, null, Pageable.unpaged(), sysadmin());

		verify(adesaoPrescricaoRepository).buscarParaVeterinario(eq(10L), eq(null), eq(null), eq(null), eq(null), any(Pageable.class));
		verify(adesaoPrescricaoRepository).buscarParaClinica(eq(30L), eq(null), eq(null), eq(null), eq(null), any(Pageable.class));
		verify(adesaoPrescricaoRepository).buscar(eq(null), eq(null), eq(null), eq(null), any(Pageable.class));
	}

	@Test
	void atualizacaoEExclusaoPublicasSaoDesabilitadas() {
		BusinessException update = assertThrows(BusinessException.class,
				() -> adesaoPrescricaoService.atualizar(80L, new AdesaoPrescricaoRequest(9L, 40L, 50L, LocalDateTime.now(), "N", null)));
		BusinessException delete = assertThrows(BusinessException.class, () -> adesaoPrescricaoService.excluir(80L));

		assertEquals(HttpStatus.METHOD_NOT_ALLOWED, update.getStatus());
		assertEquals(HttpStatus.METHOD_NOT_ALLOWED, delete.getStatus());
	}

	@Test
	void sobrecargaGenericaDeCriacaoExigePrincipal() {
		assertThrows(AccessDeniedException.class,
				() -> adesaoPrescricaoService.criar(new AdesaoPrescricaoRequest(9L, 40L, 50L, LocalDateTime.now(), "S", null)));
	}

	private void registrarAdesao(String tomou) {
		Prescricao prescricao = prescricao(periodo(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)));
		UsuarioPrincipal principal = responsavel(40L);
		when(prescricaoService.buscarEntidade(9L)).thenReturn(prescricao);
		when(responsavelRepository.findById(40L)).thenReturn(Optional.of(responsavelEntity(40L)));

		adesaoPrescricaoService.registrar(new RegistrarAdesaoPrescricaoRequest(9L, tomou, null), principal);

		verify(clinicalAccessService).exigirRegistroAdesaoResponsavel(principal, prescricao);
		verify(adesaoPrescricaoRepository).save(argThat(adesao -> tomou.equals(adesao.getTomou())));
		verify(eventoJornadaService).registrarEvento(
				eq("ADESAO_REGISTRADA"),
				eq("RESPONSAVEL"),
				eq(40L),
				eq(null),
				eq(50L),
				eq(null),
				eq("Adesao de prescricao registrada."),
				eq("{\"entity\":\"AdesaoPrescricao\"}")
		);
	}

	private void assertPapelNaoRegistraAdesao(UsuarioPrincipal principal) {
		Prescricao prescricao = prescricao(periodo(LocalDate.now(), null));
		when(prescricaoService.buscarEntidade(9L)).thenReturn(prescricao);
		doThrow(new AccessDeniedException("Registro de adesao permitido apenas ao responsavel vinculado ao animal."))
				.when(clinicalAccessService).exigirRegistroAdesaoResponsavel(principal, prescricao);

		assertThrows(AccessDeniedException.class,
				() -> adesaoPrescricaoService.registrar(new RegistrarAdesaoPrescricaoRequest(9L, "S", null), principal));

		verify(adesaoPrescricaoRepository, never()).save(any());
	}

	private Periodo periodo(LocalDate inicio, LocalDate fim) {
		return new Periodo(inicio, fim);
	}

	private Prescricao prescricao(Periodo periodo) {
		Prescricao prescricao = new Prescricao();
		prescricao.setId(9L);
		prescricao.setMedicamento("Medicamento");
		prescricao.setDosagem("1 comprimido");
		prescricao.setDataInicio(periodo.inicio());
		prescricao.setDataFim(periodo.fim());
		prescricao.setConsulta(consulta(10L, 30L));
		return prescricao;
	}

	private AdesaoPrescricao adesao(Responsavel responsavel, Prescricao prescricao) {
		AdesaoPrescricao adesao = new AdesaoPrescricao();
		adesao.setId(80L);
		adesao.setPrescricao(prescricao);
		adesao.setAnimal(prescricao.getConsulta().getAnimal());
		adesao.setResponsavel(responsavel);
		adesao.setDataRegistro(LocalDateTime.now());
		adesao.setTomou("S");
		return adesao;
	}

	private Consulta consulta(Long veterinarioId, Long clinicaId) {
		Clinica clinica = new Clinica();
		clinica.setId(clinicaId);
		Animal animal = new Animal();
		animal.setId(50L);
		animal.setNome("Bilu");
		animal.setClinica(clinica);
		Veterinario veterinario = new Veterinario();
		veterinario.setId(veterinarioId);
		Consulta consulta = new Consulta();
		consulta.setId(1L);
		consulta.setAnimal(animal);
		consulta.setVeterinario(veterinario);
		consulta.setClinica(clinica);
		return consulta;
	}

	private Responsavel responsavelEntity(Long id) {
		Responsavel responsavel = new Responsavel();
		responsavel.setId(id);
		responsavel.setNome("Tutor");
		return responsavel;
	}

	private UsuarioPrincipal responsavel(Long responsavelId) {
		return new UsuarioPrincipal(2L, "Tutor", "tutor@arkive.com", "$2a$10$hash", TipoUsuario.RESPONSAVEL, "S", false, responsavelId, null, null);
	}

	private UsuarioPrincipal veterinario(Long veterinarioId) {
		return new UsuarioPrincipal(1L, "Dra", "vet@arkive.com", "$2a$10$hash", TipoUsuario.VETERINARIO, "S", false, null, veterinarioId, null);
	}

	private UsuarioPrincipal adminClinica(Long clinicaId) {
		return new UsuarioPrincipal(3L, "Admin", "admin@arkive.com", "$2a$10$hash", TipoUsuario.ADMIN_CLINICA, "S", false, null, null, clinicaId);
	}

	private UsuarioPrincipal sysadmin() {
		return new UsuarioPrincipal(4L, "Sys", "sys@arkive.com", "$2a$10$hash", TipoUsuario.SYSADMIN, "S", false, null, null, null);
	}

	private record Periodo(LocalDate inicio, LocalDate fim) {
	}

}
