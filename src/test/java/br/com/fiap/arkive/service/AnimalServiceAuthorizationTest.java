package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.AnimalRequest;
import br.com.fiap.arkive.dto.response.AnimalResponse;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.Especie;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.repository.AnimalRepository;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

class AnimalServiceAuthorizationTest {

	private AnimalRepository animalRepository;
	private EspecieService especieService;
	private RacaService racaService;
	private ClinicaService clinicaService;
	private EventoJornadaService eventoJornadaService;
	private ClinicalAccessService clinicalAccessService;
	private VeterinarioService veterinarioService;
	private AnimalService animalService;
	private final java.time.Clock clock = java.time.Clock.fixed(java.time.Instant.parse("2026-09-08T12:00:00Z"), java.time.ZoneId.of("America/Sao_Paulo"));

	@BeforeEach
	void setUp() {
		animalRepository = mock(AnimalRepository.class);
		especieService = mock(EspecieService.class);
		racaService = mock(RacaService.class);
		clinicaService = mock(ClinicaService.class);
		eventoJornadaService = mock(EventoJornadaService.class);
		clinicalAccessService = mock(ClinicalAccessService.class);
		veterinarioService = mock(VeterinarioService.class);
		animalService = new AnimalService(
				animalRepository,
				especieService,
				racaService,
				clinicaService,
				eventoJornadaService,
				clinicalAccessService,
				veterinarioService,
				clock
		);
		when(especieService.buscarEntidade(1L)).thenReturn(especie());
		when(clinicaService.buscarEntidade(30L)).thenReturn(clinica(30L));
		when(clinicaService.buscarEntidade(31L)).thenReturn(clinica(31L));
		when(animalRepository.save(any(Animal.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(eventoJornadaService.criarPayload(any(), any(), any())).thenReturn("{\"entity\":\"Animal\"}");
	}

	@Test
	void responsavelListaSomenteAnimaisVinculados() {
		UsuarioPrincipal principal = principal(TipoUsuario.RESPONSAVEL, 40L, null, null);
		when(animalRepository.buscarParaResponsavel(eq(40L), any(LocalDate.class), eq(null), eq(null), eq(null), eq(null), eq(null), any(Pageable.class)))
				.thenReturn(Page.empty());

		animalService.listarAutorizado(null, null, null, null, null, Pageable.unpaged(), principal);

		verify(animalRepository).buscarParaResponsavel(eq(40L), any(LocalDate.class), eq(null), eq(null), eq(null), eq(null), eq(null), any(Pageable.class));
	}

	@Test
	void veterinarioListaSomenteAnimaisComConsultasProprias() {
		UsuarioPrincipal principal = principal(TipoUsuario.VETERINARIO, null, 10L, null);
		when(veterinarioService.buscarClinicaId(10L)).thenReturn(null);
		when(animalRepository.buscarParaVeterinario(eq(10L), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), any(Pageable.class)))
				.thenReturn(Page.empty());

		animalService.listarAutorizado(null, null, null, null, null, Pageable.unpaged(), principal);

		verify(animalRepository).buscarParaVeterinario(eq(10L), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), any(Pageable.class));
	}

	@Test
	void veterinarioListaPacientesAtivosDaPropriaClinicaSemConsultaPrevia() {
		UsuarioPrincipal principal = principal(TipoUsuario.VETERINARIO, null, 10L, null);
		when(veterinarioService.buscarClinicaId(10L)).thenReturn(30L);
		when(animalRepository.buscarParaVeterinario(eq(10L), eq(30L), eq("Nina"), eq(1L), eq(null), eq(null), eq("S"), any(Pageable.class)))
				.thenReturn(Page.empty());

		animalService.listarPacientesClinicaVeterinario("Nina", 1L, null, Pageable.unpaged(), principal);

		verify(animalRepository).buscarParaVeterinario(eq(10L), eq(30L), eq("Nina"), eq(1L), eq(null), eq(null), eq("S"), any(Pageable.class));
	}

	@Test
	void veterinariosDaMesmaClinicaUsamOMesmoEscopoDePacientesDaClinica() {
		when(veterinarioService.buscarClinicaId(10L)).thenReturn(30L);
		when(veterinarioService.buscarClinicaId(22L)).thenReturn(30L);
		when(animalRepository.buscarParaVeterinario(any(), eq(30L), eq(null), eq(null), eq(null), eq(null), eq("S"), any(Pageable.class)))
				.thenReturn(Page.empty());

		animalService.listarPacientesClinicaVeterinario(null, null, null, Pageable.unpaged(), principal(TipoUsuario.VETERINARIO, null, 10L, null));
		animalService.listarPacientesClinicaVeterinario(null, null, null, Pageable.unpaged(), principal(TipoUsuario.VETERINARIO, null, 22L, null));

		verify(animalRepository, org.mockito.Mockito.times(2))
				.buscarParaVeterinario(any(), eq(30L), eq(null), eq(null), eq(null), eq(null), eq("S"), any(Pageable.class));
	}

	@Test
	void veterinarioDeOutraClinicaUsaSomenteOProprioEscopoDeClinica() {
		when(veterinarioService.buscarClinicaId(10L)).thenReturn(30L);
		when(veterinarioService.buscarClinicaId(22L)).thenReturn(31L);
		when(animalRepository.buscarParaVeterinario(eq(10L), eq(30L), eq(null), eq(null), eq(null), eq(null), eq("S"), any(Pageable.class)))
				.thenReturn(Page.empty());
		when(animalRepository.buscarParaVeterinario(eq(22L), eq(31L), eq(null), eq(null), eq(null), eq(null), eq("S"), any(Pageable.class)))
				.thenReturn(Page.empty());

		animalService.listarPacientesClinicaVeterinario(null, null, null, Pageable.unpaged(), principal(TipoUsuario.VETERINARIO, null, 10L, null));
		animalService.listarPacientesClinicaVeterinario(null, null, null, Pageable.unpaged(), principal(TipoUsuario.VETERINARIO, null, 22L, null));

		verify(animalRepository).buscarParaVeterinario(eq(10L), eq(30L), eq(null), eq(null), eq(null), eq(null), eq("S"), any(Pageable.class));
		verify(animalRepository).buscarParaVeterinario(eq(22L), eq(31L), eq(null), eq(null), eq(null), eq(null), eq("S"), any(Pageable.class));
	}

	@Test
	void adminClinicaComFiltroConflitanteRecebeListaVazia() {
		UsuarioPrincipal principal = principal(TipoUsuario.ADMIN_CLINICA, null, null, 30L);

		Page<AnimalResponse> resultado = animalService.listarAutorizado(null, null, null, 31L, null, Pageable.unpaged(), principal);

		assertEquals(0, resultado.getTotalElements());
	}

	@Test
	void buscaAnimalPorIdExigeAcessoAoRecurso() {
		Animal animal = animal();
		when(animalRepository.findById(50L)).thenReturn(Optional.of(animal));
		UsuarioPrincipal principal = principal(TipoUsuario.RESPONSAVEL, 40L, null, null);

		animalService.buscarPorIdAutorizado(50L, principal);

		verify(clinicalAccessService).exigirLeituraAnimal(principal, animal);
	}

	@Test
	void sysadminCriaAnimalGlobalmente() {
		animalService.criar(request(31L), principal(TipoUsuario.SYSADMIN, null, null, null));

		verify(animalRepository).save(argThat(animal -> animal.getClinica().getId().equals(31L)));
	}

	@Test
	void permiteAnimaisAtivosComMesmoNome() {
		animalService.criar(request("Luna", 31L), principal(TipoUsuario.SYSADMIN, null, null, null));
		animalService.criar(request("Luna", 31L), principal(TipoUsuario.SYSADMIN, null, null, null));

		verify(animalRepository, org.mockito.Mockito.times(2)).save(argThat(animal ->
				"Luna".equals(animal.getNome())
						&& "S".equals(animal.getAtivo())
		));
	}

	@Test
	void sysadminAtualizaAnimalGlobalmente() {
		when(animalRepository.findById(50L)).thenReturn(Optional.of(animal(31L)));

		animalService.atualizar(50L, request(30L), principal(TipoUsuario.SYSADMIN, null, null, null));

		verify(animalRepository).save(argThat(animal -> animal.getClinica().getId().equals(30L)));
	}

	@Test
	void sysadminExcluiAnimalGlobalmente() {
		when(animalRepository.findById(50L)).thenReturn(Optional.of(animal(31L)));

		animalService.excluir(50L, principal(TipoUsuario.SYSADMIN, null, null, null));

		verify(animalRepository).save(argThat(animal -> "N".equals(animal.getAtivo())));
	}

	@Test
	void adminClinicaCriaSomenteNaPropriaClinica() {
		UsuarioPrincipal admin = principal(TipoUsuario.ADMIN_CLINICA, null, null, 30L);

		animalService.criar(request(null), admin);
		animalService.criar(request(30L), admin);

		verify(animalRepository, org.mockito.Mockito.times(2)).save(argThat(animal -> animal.getClinica().getId().equals(30L)));
		assertThrows(AccessDeniedException.class, () -> animalService.criar(request(31L), admin));
	}

	@Test
	void adminClinicaAtualizaSomenteAnimalDaPropriaClinicaENaoMoveClinica() {
		UsuarioPrincipal admin = principal(TipoUsuario.ADMIN_CLINICA, null, null, 30L);
		when(animalRepository.findById(50L)).thenReturn(Optional.of(animal(30L)));

		animalService.atualizar(50L, request(null), admin);

		verify(animalRepository).save(argThat(animal -> animal.getClinica().getId().equals(30L)));
		BusinessException exception = assertThrows(BusinessException.class, () -> animalService.atualizar(50L, request(31L), admin));
		assertEquals(HttpStatus.CONFLICT, exception.getStatus());
	}

	@Test
	void adminClinicaNaoAtualizaOuExcluiAnimalDeOutraClinica() {
		UsuarioPrincipal admin = principal(TipoUsuario.ADMIN_CLINICA, null, null, 30L);
		when(animalRepository.findById(50L)).thenReturn(Optional.of(animal(31L)));

		assertThrows(AccessDeniedException.class, () -> animalService.atualizar(50L, request(31L), admin));
		assertThrows(AccessDeniedException.class, () -> animalService.excluir(50L, admin));

		verify(animalRepository, never()).save(any());
	}

	@Test
	void adminClinicaExcluiAnimalDaPropriaClinica() {
		UsuarioPrincipal admin = principal(TipoUsuario.ADMIN_CLINICA, null, null, 30L);
		when(animalRepository.findById(50L)).thenReturn(Optional.of(animal(30L)));

		animalService.excluir(50L, admin);

		verify(animalRepository).save(argThat(animal -> "N".equals(animal.getAtivo())));
	}

	@Test
	void veterinarioCriaAnimalNaPropriaClinicaSemClinicaNoRequest() {
		when(veterinarioService.buscarClinicaId(10L)).thenReturn(30L);
		when(veterinarioService.buscarEntidadeAtiva(10L)).thenReturn(veterinario(10L));

		animalService.criar(request(null), principal(TipoUsuario.VETERINARIO, null, 10L, null));

		verify(animalRepository).save(argThat(animal ->
				animal.getClinica().getId().equals(30L)
						&& "S".equals(animal.getAtivo())
						&& animal.getVeterinarioCadastro().getId().equals(10L)
		));
	}

	@Test
	void veterinarioSemClinicaCriaAnimalCliniclessComCadastroProprio() {
		when(veterinarioService.buscarClinicaId(10L)).thenReturn(null);
		when(veterinarioService.buscarEntidadeAtiva(10L)).thenReturn(veterinario(10L));

		animalService.criar(request(null), principal(TipoUsuario.VETERINARIO, null, 10L, null));

		verify(animalRepository).save(argThat(animal ->
				animal.getClinica() == null
						&& animal.getVeterinarioCadastro().getId().equals(10L)
						&& "S".equals(animal.getAtivo())
		));
	}

	@Test
	void veterinarioSemClinicaNaoInformaClinicaNoRequest() {
		when(veterinarioService.buscarClinicaId(10L)).thenReturn(null);

		assertThrows(AccessDeniedException.class,
				() -> animalService.criar(request(30L), principal(TipoUsuario.VETERINARIO, null, 10L, null)));

		verify(animalRepository, never()).save(any());
	}

	@Test
	void meusPacientesDoVeterinarioUsaEscopoAtivoSemExigirClinica() {
		when(veterinarioService.buscarClinicaId(10L)).thenReturn(null);
		when(animalRepository.buscarParaVeterinario(eq(10L), eq(null), eq("Luna"), eq(1L), eq(null), eq(null), eq("S"), any(Pageable.class)))
				.thenReturn(Page.empty());

		animalService.listarPacientesVeterinario("Luna", 1L, null, Pageable.unpaged(), principal(TipoUsuario.VETERINARIO, null, 10L, null));

		verify(animalRepository).buscarParaVeterinario(eq(10L), eq(null), eq("Luna"), eq(1L), eq(null), eq(null), eq("S"), any(Pageable.class));
	}

	@Test
	void veterinarioNaoForcaCriacaoEmOutraClinica() {
		when(veterinarioService.buscarClinicaId(10L)).thenReturn(30L);

		assertThrows(AccessDeniedException.class,
				() -> animalService.criar(request(31L), principal(TipoUsuario.VETERINARIO, null, 10L, null)));

		verify(animalRepository, never()).save(any());
	}

	@Test
	void veterinarioNaoCriaAnimalInativo() {
		when(veterinarioService.buscarClinicaId(10L)).thenReturn(30L);

		assertThrows(BusinessException.class,
				() -> animalService.criar(request(null, "N"), principal(TipoUsuario.VETERINARIO, null, 10L, null)));

		verify(animalRepository, never()).save(any());
	}

	@Test
	void veterinarioAtualizaDadosBasicosDoAnimalAtivoDaPropriaClinica() {
		when(animalRepository.findById(50L)).thenReturn(Optional.of(animal(30L)));

		animalService.atualizar(50L, request(null), principal(TipoUsuario.VETERINARIO, null, 10L, null));

		verify(animalRepository).save(argThat(animal ->
				animal.getClinica().getId().equals(30L)
						&& "S".equals(animal.getAtivo())
						&& "Nina".equals(animal.getNome())
		));
	}

	@Test
	void veterinarioNaoAtualizaAnimalDeOutraClinica() {
		when(animalRepository.findById(50L)).thenReturn(Optional.of(animal(31L)));
		doThrow(new AccessDeniedException("Veterinario nao autorizado para atualizar este animal."))
				.when(clinicalAccessService).exigirLeituraAnimal(any(), any());

		assertThrows(AccessDeniedException.class,
				() -> animalService.atualizar(50L, request(null), principal(TipoUsuario.VETERINARIO, null, 10L, null)));

		verify(animalRepository, never()).save(any());
	}

	@Test
	void veterinarioNaoMoveAnimalParaOutraClinicaNemAlteraStatus() {
		when(animalRepository.findById(50L)).thenReturn(Optional.of(animal(30L)));

		assertThrows(BusinessException.class,
				() -> animalService.atualizar(50L, request(31L), principal(TipoUsuario.VETERINARIO, null, 10L, null)));
		assertThrows(BusinessException.class,
				() -> animalService.atualizar(50L, request(null, "N"), principal(TipoUsuario.VETERINARIO, null, 10L, null)));

		verify(animalRepository, never()).save(any());
	}

	@Test
	void veterinarioNaoDesativaAnimal() {
		when(animalRepository.findById(50L)).thenReturn(Optional.of(animal(30L)));

		assertThrows(AccessDeniedException.class,
				() -> animalService.excluir(50L, principal(TipoUsuario.VETERINARIO, null, 10L, null)));

		verify(animalRepository, never()).save(any());
	}

	@Test
	void responsavelNaoExecutaMutacaoGenericaDeAnimal() {
		assertMutacoesBloqueadas(principal(TipoUsuario.RESPONSAVEL, 40L, null, null));
	}

	@Test
	void sobrecargasGenericasDeEscritaExigemPrincipal() {
		assertThrows(AccessDeniedException.class, () -> animalService.criar(request(30L)));
		assertThrows(AccessDeniedException.class, () -> animalService.atualizar(50L, request(30L)));
		assertThrows(AccessDeniedException.class, () -> animalService.excluir(50L));
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.NullSource
	@org.junit.jupiter.params.provider.ValueSource(strings = {"2021-04-17", "2026-09-08"})
	void nascimentoOpcionalValidoNaCriacaoEAtualizacao(String valor) {
		LocalDate data = valor == null ? null : LocalDate.parse(valor);
		var request = new AnimalRequest("Nina", 1L, null, "F", "N", null, "S", data);
		var vet = principal(TipoUsuario.VETERINARIO, null, 10L, null);
		when(veterinarioService.buscarEntidadeAtiva(10L)).thenReturn(veterinario(10L));
		assertEquals(data, animalService.criar(request, vet).dataNascimento());
		when(animalRepository.findById(50L)).thenReturn(Optional.of(animal(null)));
		assertEquals(data, animalService.atualizar(50L, request, vet).dataNascimento());
	}

	@Test
	void nascimentoFuturoRejeitadoNaCriacaoEAtualizacao() {
		var request = new AnimalRequest("Nina", 1L, null, "F", "N", null, "S", LocalDate.now(clock).plusDays(1));
		var admin = principal(TipoUsuario.SYSADMIN, null, null, null);
		when(animalRepository.findById(50L)).thenReturn(Optional.of(animal(null)));
		assertEquals(HttpStatus.BAD_REQUEST, assertThrows(BusinessException.class, () -> animalService.criar(request, admin)).getStatus());
		assertThrows(BusinessException.class, () -> animalService.atualizar(50L, request, admin));
		verify(animalRepository, never()).save(any());
	}

	@Test
	void racaDeOutraEspecieContinuaRejeitada() {
		var raca = new br.com.fiap.arkive.entity.Raca();
		var outraEspecie = especie();
		outraEspecie.setId(2L);
		raca.setEspecie(outraEspecie);
		when(racaService.buscarEntidade(2L)).thenReturn(raca);
		var request = new AnimalRequest("Nina", 1L, 2L, "F", "N", null, "S");
		var admin = principal(TipoUsuario.SYSADMIN, null, null, null);
		when(animalRepository.findById(50L)).thenReturn(Optional.of(animal(null)));
		assertThrows(BusinessException.class, () -> animalService.criar(request, admin));
		assertThrows(BusinessException.class, () -> animalService.atualizar(50L, request, admin));
	}

	private Animal animal() {
		return animal(30L);
	}

	private Animal animal(Long clinicaId) {
		Especie especie = new Especie();
		especie.setId(1L);
		especie.setNome("Canino");
		Animal animal = new Animal();
		animal.setId(50L);
		animal.setNome("Nina");
		animal.setEspecie(especie);
		if (clinicaId != null) {
			animal.setClinica(clinica(clinicaId));
		}
		animal.setAtivo("S");
		return animal;
	}

	private Veterinario veterinario(Long id) {
		Veterinario veterinario = new Veterinario();
		veterinario.setId(id);
		veterinario.setNome("Dra " + id);
		veterinario.setAtivo("S");
		return veterinario;
	}

	private Especie especie() {
		Especie especie = new Especie();
		especie.setId(1L);
		especie.setNome("Canino");
		return especie;
	}

	private Clinica clinica(Long id) {
		Clinica clinica = new Clinica();
		clinica.setId(id);
		clinica.setNome("Clinica " + id);
		return clinica;
	}

	private AnimalRequest request(Long clinicaId) {
		return request(clinicaId, "S");
	}

	private AnimalRequest request(Long clinicaId, String ativo) {
		return request("Nina", clinicaId, ativo);
	}

	private AnimalRequest request(String nome, Long clinicaId) {
		return request(nome, clinicaId, "S");
	}

	private AnimalRequest request(String nome, Long clinicaId, String ativo) {
		return new AnimalRequest(nome, 1L, null, "F", "N", clinicaId, ativo);
	}

	private void assertMutacoesBloqueadas(UsuarioPrincipal principal) {
		when(animalRepository.findById(50L)).thenReturn(Optional.of(animal(30L)));

		assertThrows(AccessDeniedException.class, () -> animalService.criar(request(30L), principal));
		assertThrows(AccessDeniedException.class, () -> animalService.atualizar(50L, request(30L), principal));
		assertThrows(AccessDeniedException.class, () -> animalService.excluir(50L, principal));
	}

	private UsuarioPrincipal principal(TipoUsuario tipo, Long responsavelId, Long veterinarioId, Long clinicaId) {
		return new UsuarioPrincipal(1L, "Usuario", "usuario@arkive.com", "$2a$10$hash", tipo, "S", false, responsavelId, veterinarioId, clinicaId);
	}

}
