package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.ConsultaRequest;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.repository.AnimalRepository;
import br.com.fiap.arkive.repository.ClinicaRepository;
import br.com.fiap.arkive.repository.ConsultaRepository;
import br.com.fiap.arkive.repository.VeterinarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

class ConsultaServiceTest {

	private ConsultaRepository consultaRepository;
	private AnimalRepository animalRepository;
	private VeterinarioRepository veterinarioRepository;
	private ClinicaRepository clinicaRepository;
	private EventoJornadaService eventoJornadaService;
	private ClinicalAccessService clinicalAccessService;
	private ConsultaService consultaService;
	private final java.time.Clock clock = java.time.Clock.fixed(java.time.Instant.parse("2026-09-08T12:00:00Z"), java.time.ZoneId.of("America/Sao_Paulo"));

	@BeforeEach
	void setUp() {
		consultaRepository = mock(ConsultaRepository.class);
		animalRepository = mock(AnimalRepository.class);
		veterinarioRepository = mock(VeterinarioRepository.class);
		clinicaRepository = mock(ClinicaRepository.class);
		eventoJornadaService = mock(EventoJornadaService.class);
		clinicalAccessService = mock(ClinicalAccessService.class);
		consultaService = new ConsultaService(
				consultaRepository,
				animalRepository,
				veterinarioRepository,
				clinicaRepository,
				eventoJornadaService,
				clinicalAccessService,
				clock
		);
		when(consultaRepository.save(any(Consulta.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(eventoJornadaService.criarPayload(any(), any(), any())).thenReturn("{\"entity\":\"Consulta\"}");
		when(animalRepository.findById(10L)).thenReturn(Optional.of(animal()));
		when(veterinarioRepository.findById(20L)).thenReturn(Optional.of(veterinario()));
		when(clinicaRepository.findById(30L)).thenReturn(Optional.of(clinica()));
	}

	@Test
	void criaConsultaSempreAgendadaQuandoStatusOmitidoOuAg() {
		consultaService.criar(request(null), veterinarioPrincipal(20L));
		consultaService.criar(request("AG"), veterinarioPrincipal(20L));

		verify(consultaRepository, times(2)).save(any(Consulta.class));
	}

	@Test
	void criaPrimeiraConsultaParaAnimalRecemCadastradoSemConsultaPrevia() {
		consultaService.criar(request("AG"), veterinarioPrincipal(20L));

		verify(consultaRepository).save(any(Consulta.class));
		verify(clinicalAccessService).exigirLeituraAnimal(any(), any());
	}

	@Test
	void criaConsultaSemVeterinarioNoRequestUsaVeterinarioAutenticado() {
		consultaService.criar(requestSemVeterinario("AG"), veterinarioPrincipal(20L));

		verify(consultaRepository).save(argThat(consulta ->
				consulta.getVeterinario().getId().equals(20L)
		));
	}

	@Test
	void rejeitaCriacaoComStatusDiferenteDeAg() {
		assertThrows(BusinessException.class, () -> consultaService.criar(request("EP"), veterinarioPrincipal(20L)));
		assertThrows(BusinessException.class, () -> consultaService.criar(request("FI"), veterinarioPrincipal(20L)));
		verify(consultaRepository, never()).save(any());
	}

	@Test
	void somenteVeterinarioAutenticadoCriaConsulta() {
		assertThrows(AccessDeniedException.class, () -> consultaService.criar(request("AG"), principal(TipoUsuario.RESPONSAVEL, 40L, null, null)));
		assertThrows(AccessDeniedException.class, () -> consultaService.criar(request("AG"), principal(TipoUsuario.ADMIN_CLINICA, null, null, 30L)));
		assertThrows(AccessDeniedException.class, () -> consultaService.criar(request("AG"), principal(TipoUsuario.SYSADMIN, null, null, null)));

		verify(consultaRepository, never()).save(any());
	}

	@Test
	void veterinarioNaoCriaConsultaComoOutroVeterinario() {
		BusinessException exception = assertThrows(BusinessException.class,
				() -> consultaService.criar(request("AG"), veterinarioPrincipal(21L)));

		assertEquals(HttpStatus.CONFLICT, exception.getStatus());
		verify(consultaRepository, never()).save(any());
	}

	@Test
	void veterinarioSemClinicaCriaConsultaSemClinica() {
		when(veterinarioRepository.findById(20L)).thenReturn(Optional.of(veterinarioSemClinica()));

		consultaService.criar(requestSemVeterinarioEClinica("AG"), veterinarioPrincipal(20L));

		verify(consultaRepository).save(argThat(consulta ->
				consulta.getVeterinario().getId().equals(20L)
						&& consulta.getClinica() == null
		));
	}

	@Test
	void veterinarioSemClinicaNaoCriaConsultaInformandoClinica() {
		when(veterinarioRepository.findById(20L)).thenReturn(Optional.of(veterinarioSemClinica()));

		assertThrows(AccessDeniedException.class,
				() -> consultaService.criar(request("AG"), veterinarioPrincipal(20L)));

		verify(consultaRepository, never()).save(any());
	}

	@Test
	void putNaoAlteraStatusDaConsulta() {
		Consulta consulta = consulta("EP");
		when(consultaRepository.findById(1L)).thenReturn(Optional.of(consulta));

		assertThrows(BusinessException.class, () -> consultaService.atualizar(1L, request("FI"), veterinarioPrincipal(20L)));

		assertEquals("EP", consulta.getStatus());
		verify(consultaRepository, never()).save(any());
	}

	@Test
	void putMantemEdicaoDeCamposQuandoStatusNaoMuda() {
		Consulta consulta = consulta("EP");
		when(consultaRepository.findById(1L)).thenReturn(Optional.of(consulta));
		ConsultaRequest request = new ConsultaRequest(
				LocalDateTime.now().plusDays(1),
				"REMOTA",
				"Retorno",
				"Sintomas atualizados",
				"Observacao atualizada",
				null,
				"Narrativa atualizada",
				"EP",
				10L,
				20L,
				30L
		);
		var principal = veterinarioPrincipal(20L);

		consultaService.atualizar(1L, request, principal);

		assertEquals("EP", consulta.getStatus());
		assertEquals("REMOTA", consulta.getModalidade());
		assertEquals("Narrativa atualizada", consulta.getTranscricao());
		verify(clinicalAccessService).exigirEscritaClinicaVeterinario(principal, consulta);
		verify(consultaRepository).save(consulta);
	}

	@Test
	void putExigeVeterinarioDonoDaConsulta() {
		Consulta consulta = consulta("EP");
		var principal = veterinarioPrincipal(21L);
		when(consultaRepository.findById(1L)).thenReturn(Optional.of(consulta));
		doThrow(new AccessDeniedException("Veterinario nao autorizado para esta consulta."))
				.when(clinicalAccessService).exigirEscritaClinicaVeterinario(principal, consulta);

		assertThrows(AccessDeniedException.class, () -> consultaService.atualizar(1L, request("EP"), principal));

		verify(consultaRepository, never()).save(any());
	}

	@Test
	void putNaoReassociaAnimalVeterinarioOuClinica() {
		Consulta consulta = consulta("EP");
		when(consultaRepository.findById(1L)).thenReturn(Optional.of(consulta));

		assertReassociacaoBloqueada(new ConsultaRequest(LocalDateTime.now(), "PRESENCIAL", "Check-up", null, null, null, null, "EP", 11L, 20L, 30L));
		assertReassociacaoBloqueada(new ConsultaRequest(LocalDateTime.now(), "PRESENCIAL", "Check-up", null, null, null, null, "EP", 10L, 21L, 30L));
		assertReassociacaoBloqueada(new ConsultaRequest(LocalDateTime.now(), "PRESENCIAL", "Check-up", null, null, null, null, "EP", 10L, 20L, 31L));

		verify(consultaRepository, never()).save(any());
	}

	@Test
	void deleteExigeVeterinarioDonoDaConsulta() {
		Consulta consulta = consulta("AG");
		var principal = veterinarioPrincipal(21L);
		when(consultaRepository.findById(1L)).thenReturn(Optional.of(consulta));
		doThrow(new AccessDeniedException("Veterinario nao autorizado para esta consulta."))
				.when(clinicalAccessService).exigirEscritaClinicaVeterinario(principal, consulta);

		assertThrows(AccessDeniedException.class, () -> consultaService.excluir(1L, principal));

		verify(consultaRepository, never()).delete(any());
	}

	@Test
	void deletePermiteSomenteConsultaAgendadaPropria() {
		Consulta agendada = consulta("AG");
		when(consultaRepository.findById(1L)).thenReturn(Optional.of(agendada));

		consultaService.excluir(1L, veterinarioPrincipal(20L));

		verify(consultaRepository).delete(agendada);
	}

	@Test
	void deleteBloqueiaConsultaQueProgrediu() {
		assertDeletePorStatusBloqueado("EP");
		assertDeletePorStatusBloqueado("AP");
		assertDeletePorStatusBloqueado("FI");
		assertDeletePorStatusBloqueado("CA");
	}

	@Test
	void sobrecargasGenericasDeEscritaExigemPrincipal() {
		assertThrows(AccessDeniedException.class, () -> consultaService.criar(request("AG")));
		assertThrows(AccessDeniedException.class, () -> consultaService.atualizar(1L, request("AG")));
		assertThrows(AccessDeniedException.class, () -> consultaService.excluir(1L));
	}

	@Test
	void listagemAutorizadaDeVeterinarioSempreUsaVeterinarioAutenticado() {
		var principal = new br.com.fiap.arkive.security.UsuarioPrincipal(
				1L, "Dra Vera", "vera@arkive.com", "$2a$10$hash",
				br.com.fiap.arkive.entity.TipoUsuario.VETERINARIO, "S", false, null, 20L, null
		);
		when(consultaRepository.buscar(null, 20L, null, null, null, org.springframework.data.domain.Pageable.unpaged()))
				.thenReturn(org.springframework.data.domain.Page.empty());

		consultaService.listarAutorizado(null, null, null, null, null, org.springframework.data.domain.Pageable.unpaged(), principal);

		verify(consultaRepository).buscar(null, 20L, null, null, null, org.springframework.data.domain.Pageable.unpaged());
	}

	@Test
	void listagemAutorizadaDeVeterinarioComFiltroConflitanteRetornaVazia() {
		var principal = new br.com.fiap.arkive.security.UsuarioPrincipal(
				1L, "Dra Vera", "vera@arkive.com", "$2a$10$hash",
				br.com.fiap.arkive.entity.TipoUsuario.VETERINARIO, "S", false, null, 20L, null
		);

		var resultado = consultaService.listarAutorizado(null, 22L, null, null, null, org.springframework.data.domain.Pageable.unpaged(), principal);

		assertEquals(0, resultado.getTotalElements());
		verify(consultaRepository, never()).buscar(null, 22L, null, null, null, org.springframework.data.domain.Pageable.unpaged());
	}

	@Test
	void buscaAutorizadaValidaAcessoAoRecurso() {
		Consulta consulta = consulta("EP");
		when(consultaRepository.findById(1L)).thenReturn(Optional.of(consulta));
		var principal = new br.com.fiap.arkive.security.UsuarioPrincipal(
				1L, "Dra Vera", "vera@arkive.com", "$2a$10$hash",
				br.com.fiap.arkive.entity.TipoUsuario.VETERINARIO, "S", false, null, 20L, null
		);

		consultaService.buscarPorIdAutorizado(1L, principal);

		verify(clinicalAccessService).exigirLeituraConsulta(principal, consulta);
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(longs = {0, 1, 86400})
	void aceitaAgoraOuFuturo(long segundos) {
		var data = LocalDateTime.now(clock).plusSeconds(segundos);
		assertEquals(data, consultaService.criar(comDataEndereco(data, "REMOTA", null), veterinarioPrincipal(20L)).dataHora());
	}

	@Test
	void rejeitaCriacaoNoPassado() {
		BusinessException ex = assertThrows(BusinessException.class, () -> consultaService.criar(
				comDataEndereco(LocalDateTime.now(clock).minusNanos(1), "PRESENCIAL", null), veterinarioPrincipal(20L)));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
		verify(consultaRepository, never()).save(any());
	}

	@Test
	void rejeitaReagendamentoNoPassado() {
		Consulta consulta = consulta("AG");
		consulta.setDataHora(LocalDateTime.now(clock).plusDays(1));
		when(consultaRepository.findById(1L)).thenReturn(Optional.of(consulta));
		assertThrows(BusinessException.class, () -> consultaService.atualizar(1L,
				comDataEndereco(LocalDateTime.now(clock).minusDays(1), "PRESENCIAL", null), veterinarioPrincipal(20L)));
		verify(consultaRepository, never()).save(any());
	}

	@Test
	void permiteEdicaoHistoricaSemMudarData() {
		Consulta consulta = consulta("EP");
		consulta.setDataHora(LocalDateTime.now(clock).minusDays(2));
		when(consultaRepository.findById(1L)).thenReturn(Optional.of(consulta));
		var request = new ConsultaRequest(consulta.getDataHora(), "PRESENCIAL", "Retorno corrigido", null,
				null, null, null, "EP", 10L, 20L, 30L, "Rua original");
		assertEquals("Retorno corrigido", consultaService.atualizar(1L, request, veterinarioPrincipal(20L)).motivo());
	}

	@Test
	void enderecoDaClinicaEhSnapshotInclusiveSemClinicaNoRequest() {
		Veterinario vet = veterinario();
		vet.getClinica().setEndereco("Rua da Clinica, 10");
		when(veterinarioRepository.findById(20L)).thenReturn(Optional.of(vet));
		var response = consultaService.criar(requestSemVeterinarioEClinica("AG"), veterinarioPrincipal(20L));
		var captor = org.mockito.ArgumentCaptor.forClass(Consulta.class);
		verify(consultaRepository).save(captor.capture());
		vet.getClinica().setEndereco("Novo endereco");
		assertEquals("Rua da Clinica, 10", response.endereco());
		assertEquals("Rua da Clinica, 10", captor.getValue().getEndereco());
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.NullAndEmptySource
	@org.junit.jupiter.params.provider.ValueSource(strings = {"   "})
	void enderecoVazioUsaClinica(String endereco) {
		Veterinario vet = veterinario();
		vet.getClinica().setEndereco("Rua da Clinica");
		when(veterinarioRepository.findById(20L)).thenReturn(Optional.of(vet));
		assertEquals("Rua da Clinica", consultaService.criar(comDataEndereco(LocalDateTime.now(clock), "PRESENCIAL", endereco), veterinarioPrincipal(20L)).endereco());
	}

	@Test
	void enderecoPersonalizadoVenceClinica() {
		assertEquals("Rua escolhida", consultaService.criar(comDataEndereco(LocalDateTime.now(clock), "PRESENCIAL", "Rua escolhida"), veterinarioPrincipal(20L)).endereco());
	}

	@Test
	void presencialSemClinicaAceitaEnderecoNulo() {
		when(veterinarioRepository.findById(20L)).thenReturn(Optional.of(veterinarioSemClinica()));
		org.junit.jupiter.api.Assertions.assertNull(consultaService.criar(requestSemVeterinarioEClinica("AG"), veterinarioPrincipal(20L)).endereco());
	}

	@Test
	void remotaNaoUsaClinicaMasPreservaEnderecoExplicito() {
		Veterinario vet = veterinario();
		vet.getClinica().setEndereco("Rua da Clinica");
		when(veterinarioRepository.findById(20L)).thenReturn(Optional.of(vet));
		org.junit.jupiter.api.Assertions.assertNull(consultaService.criar(comDataEndereco(LocalDateTime.now(clock), "REMOTA", " "), veterinarioPrincipal(20L)).endereco());
		assertEquals("Referencia", consultaService.criar(comDataEndereco(LocalDateTime.now(clock), "REMOTA", "Referencia"), veterinarioPrincipal(20L)).endereco());
	}

	@Test
	void atualizacaoNaoReaplicaEnderecoDaClinica() {
		Consulta consulta = consulta("AG");
		consulta.setDataHora(LocalDateTime.now(clock));
		consulta.getVeterinario().getClinica().setEndereco("Novo endereco");
		when(consultaRepository.findById(1L)).thenReturn(Optional.of(consulta));
		org.junit.jupiter.api.Assertions.assertNull(consultaService.atualizar(1L, comDataEndereco(consulta.getDataHora(), "PRESENCIAL", null), veterinarioPrincipal(20L)).endereco());
	}

	private ConsultaRequest comDataEndereco(LocalDateTime data, String modalidade, String endereco) {
		return new ConsultaRequest(data, modalidade, "Check-up", null, null, null, null, "AG", 10L, null, 30L, endereco);
	}

	private ConsultaRequest request(String status) {
		return new ConsultaRequest(
				LocalDateTime.now(clock),
				"PRESENCIAL",
				"Check-up",
				null,
				null,
				null,
				null,
				status,
				10L,
				20L,
				30L
		);
	}

	private ConsultaRequest requestSemVeterinario(String status) {
		return new ConsultaRequest(
				LocalDateTime.now(clock),
				"PRESENCIAL",
				"Check-up",
				null,
				null,
				null,
				null,
				status,
				10L,
				null,
				30L
		);
	}

	private ConsultaRequest requestSemVeterinarioEClinica(String status) {
		return new ConsultaRequest(
				LocalDateTime.now(clock),
				"PRESENCIAL",
				"Check-up",
				null,
				null,
				null,
				null,
				status,
				10L,
				null,
				null
		);
	}

	private void assertReassociacaoBloqueada(ConsultaRequest request) {
		BusinessException exception = assertThrows(BusinessException.class,
				() -> consultaService.atualizar(1L, request, veterinarioPrincipal(20L)));
		assertEquals(HttpStatus.CONFLICT, exception.getStatus());
	}

	private void assertDeletePorStatusBloqueado(String status) {
		Consulta consulta = consulta(status);
		when(consultaRepository.findById(1L)).thenReturn(Optional.of(consulta));

		BusinessException exception = assertThrows(BusinessException.class,
				() -> consultaService.excluir(1L, veterinarioPrincipal(20L)));

		assertEquals(HttpStatus.CONFLICT, exception.getStatus());
		verify(consultaRepository, never()).delete(consulta);
	}

	private Consulta consulta(String status) {
		Consulta consulta = new Consulta();
		consulta.setStatus(status);
		consulta.setAnimal(animal());
		consulta.setVeterinario(veterinario());
		consulta.setClinica(clinica());
		return consulta;
	}

	private Animal animal() {
		Animal animal = new Animal();
		animal.setId(10L);
		animal.setNome("Nina");
		animal.setAtivo("S");
		return animal;
	}

	private Veterinario veterinario() {
		Veterinario veterinario = new Veterinario();
		veterinario.setId(20L);
		veterinario.setNome("Dra Vera");
		veterinario.setAtivo("S");
		veterinario.setClinica(clinica());
		return veterinario;
	}

	private Veterinario veterinarioSemClinica() {
		Veterinario veterinario = new Veterinario();
		veterinario.setId(20L);
		veterinario.setNome("Dra Vera");
		veterinario.setAtivo("S");
		return veterinario;
	}

	private Clinica clinica() {
		Clinica clinica = new Clinica();
		clinica.setId(30L);
		clinica.setNome("Clinica Arkive");
		clinica.setAtivo("S");
		return clinica;
	}

	private br.com.fiap.arkive.security.UsuarioPrincipal veterinarioPrincipal(Long veterinarioId) {
		return principal(TipoUsuario.VETERINARIO, null, veterinarioId, null);
	}

	private br.com.fiap.arkive.security.UsuarioPrincipal principal(TipoUsuario tipo, Long responsavelId, Long veterinarioId, Long clinicaId) {
		return new br.com.fiap.arkive.security.UsuarioPrincipal(
				1L,
				"Usuario",
				"usuario@arkive.com",
				"$2a$10$hash",
				tipo,
				"S",
				false,
				responsavelId,
				veterinarioId,
				clinicaId
		);
	}
}
