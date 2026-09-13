package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.config.BusinessTimeConfig;
import br.com.fiap.arkive.dto.request.AnimalRequest;
import br.com.fiap.arkive.dto.request.AnimalResponsavelRequest;
import br.com.fiap.arkive.dto.request.ConsultaRequest;
import br.com.fiap.arkive.dto.response.AnimalResponse;
import br.com.fiap.arkive.entity.*;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"})
@Import({AnimalService.class, ClinicalAccessService.class, VeterinarioService.class, ConsultaService.class,
		AnimalResponsavelService.class, AnimalHistoricoService.class, AnimalHistoricoPdfService.class,
		ConsultaResumoPdfService.class, BusinessTimeConfig.class})
class PatientIsolationTest {
	@Autowired TestEntityManager em;
	@Autowired AnimalService animais;
	@Autowired AnimalRepository animalRepository;
	@Autowired ConsultaRepository consultas;
	@Autowired ConsultaService consultaService;
	@Autowired ClinicalAccessService acesso;
	@Autowired AnimalResponsavelService tutores;
	@Autowired AnimalResponsavelRepository vinculos;
	@Autowired AnimalHistoricoService historico;
	@Autowired AnimalHistoricoPdfService historicoPdf;
	@Autowired ConsultaResumoPdfService resumoPdf;
	@MockitoBean EspecieService especies;
	@MockitoBean RacaService racas;
	@MockitoBean ClinicaService clinicas;
	@MockitoBean EventoJornadaService eventos;
	@MockitoBean AccountProvisioningService provisionamento;
	@MockitoBean(name = "businessClock") java.time.Clock clock;
	private Map<String, Veterinario> vets;
	private Map<String, Animal> pacientes;
	private Especie especie;
	private Clinica x;
	private Clinica y;

	@BeforeEach
	void preparar() {
		org.mockito.Mockito.when(clock.instant()).thenReturn(java.time.Instant.parse("2026-09-09T01:04:59Z"));
		org.mockito.Mockito.when(clock.getZone()).thenReturn(new BusinessTimeConfig().businessClock().getZone());
		x = clinica("X", "11111111111111"); y = clinica("Y", "22222222222222");
		vets = Map.of("A", vet("A", null), "B", vet("B", null), "C", vet("C", x),
				"D", vet("D", x), "E", vet("E", y));
		especie = new Especie(); especie.setNome("Cachorro"); em.persist(especie);
		pacientes = Map.of("P1", paciente("P1", "A", null), "P2", paciente("P2", "B", null),
				"P3", paciente("P3", null, x), "P4", paciente("P4", null, y));
		em.flush();
	}

	@ParameterizedTest
	@ValueSource(strings = {"A", "B", "C", "D", "E"})
	void listasELeituraDiretaConcordamNaMatrizDeIsolamento(String vet) {
		Set<String> esperado = switch (vet) {
			case "A" -> Set.of("P1"); case "B" -> Set.of("P2");
			case "C", "D" -> Set.of("P3"); default -> Set.of("P4");
		};
		var principal = principal(vet);
		assertEquals(esperado, nomes(animais.listarPacientesVeterinario(null, null, null, Pageable.unpaged(), principal).getContent()));
		assertEquals(esperado, nomes(animais.listarPacientesClinicaVeterinario(null, null, null, Pageable.unpaged(), principal).getContent()));
		assertEquals(esperado, nomes(animais.listarAutorizado(null, null, null, null, null, Pageable.unpaged(), principal).getContent()));
		for (var paciente : pacientes.values()) {
			if (esperado.contains(paciente.getNome())) assertDoesNotThrow(() -> animais.buscarPorIdAutorizado(paciente.getId(), principal));
			else assertThrows(AccessDeniedException.class, () -> animais.buscarPorIdAutorizado(paciente.getId(), principal));
		}
	}

	@Test
	void consultaLegitimaAcrescentaAcessoSemDuplicarPacienteNemContagem() {
		consulta("P2", "A", "FI"); consulta("P2", "A", "FI");
		em.flush();
		var pagina = animais.listarPacientesVeterinario(null, null, null, PageRequest.of(0, 1), principal("A"));
		assertEquals(2, pagina.getTotalElements());
		assertEquals(Set.of("P1", "P2"), nomes(animais.listarPacientesVeterinario(null, null, null, Pageable.unpaged(), principal("A")).getContent()));
		assertDoesNotThrow(() -> animais.buscarPorIdAutorizado(pacientes.get("P2").getId(), principal("A")));
	}

	@Test
	void atribuicaoDeConsultaAgendadaMantemPoliticaExistenteSemCompartilharComOutros() {
		Consulta consulta = consulta("P2", "A", "AG"); consulta.setDataHora(LocalDateTime.of(2099, 1, 1, 10, 0)); em.flush();
		assertDoesNotThrow(() -> animais.buscarPorIdAutorizado(pacientes.get("P2").getId(), principal("A")));
		assertEquals(Set.of("P1", "P2"), nomes(animais.listarPacientesVeterinario(null, null, null, Pageable.unpaged(), principal("A")).getContent()));
		assertThrows(AccessDeniedException.class, () -> animais.buscarPorIdAutorizado(pacientes.get("P2").getId(), principal("E")));
	}

	@Test
	void pacientesInativosNaoVazamPelaListaGenericaOuPorFiltros() {
		Animal inativo = paciente("Inativo", "A", x); inativo.setAtivo("N"); em.flush();
		for (String vet : List.of("A", "C")) {
			for (String filtro : new String[]{null, "S", "N"}) {
				assertFalse(nomes(animais.listarAutorizado(null, null, null, null, filtro, Pageable.unpaged(), principal(vet)).getContent()).contains("Inativo"));
			}
			assertFalse(nomes(animais.listarPacientesVeterinario(null, null, null, Pageable.unpaged(), principal(vet)).getContent()).contains("Inativo"));
			assertThrows(AccessDeniedException.class, () -> animais.buscarPorIdAutorizado(inativo.getId(), principal(vet)));
		}
		assertTrue(animalRepository.buscarParaVeterinario(null, x.getId(), null, null, null, null, null, Pageable.unpaged()).isEmpty());
	}

	@Test
	void filtrosNaoAmpliamEscopoEClinicaVemDoCadastroAtualDoVeterinario() {
		assertTrue(animais.listarAutorizado(null, null, null, x.getId(), null, Pageable.unpaged(), principal("A")).isEmpty());
		assertTrue(animais.listarPacientesVeterinario("P2", especie.getId(), null, Pageable.unpaged(), principal("A")).isEmpty());
		assertEquals(Set.of("P1"), nomes(animais.listarPacientesVeterinario("P1", especie.getId(), null, Pageable.unpaged(), principal("A")).getContent()));
		vets.get("C").setClinica(y); em.flush();
		assertEquals(Set.of("P4"), nomes(animais.listarPacientesVeterinario(null, null, null, Pageable.unpaged(), principal("C")).getContent()));
	}

	@Test
	void idAdivinhadoNaoPermiteAtualizarAgendarVincularOuExportar() {
		Animal paciente = pacientes.get("P2"); var principal = principal("A");
		assertThrows(AccessDeniedException.class, () -> animais.atualizar(paciente.getId(),
				new AnimalRequest("Alterado", especie.getId(), null, "M", "N", null, "S"), principal));
		assertEquals("P2", paciente.getNome());
		long quantidade = consultas.count();
		assertThrows(AccessDeniedException.class, () -> consultaService.criar(new ConsultaRequest(
				LocalDateTime.of(2099, 1, 1, 10, 0), "REMOTA", "Retorno", null, null, null, null,
				"AG", paciente.getId(), null, null), principal));
		assertEquals(quantidade, consultas.count());
		assertThrows(AccessDeniedException.class, () -> tutores.listarAtivosPorAnimal(paciente.getId(), principal));
		assertThrows(AccessDeniedException.class, () -> tutores.criar(new AnimalResponsavelRequest(
				paciente.getId(), 999L, "TUTOR_LEGAL", null, null, "S", null), principal));
		assertThrows(AccessDeniedException.class, () -> historico.buscar(paciente.getId(), principal));
		assertThrows(AccessDeniedException.class, () -> historicoPdf.gerar(paciente.getId(), principal));
		Consulta consulta = consulta("P2", "B", "FI");
		assertThrows(AccessDeniedException.class, () -> resumoPdf.gerarResumo(consulta.getId(), principal));
	}

	@Test
	void papeisAdministrativosPreservamEscopoInclusiveInativos() {
		Animal paciente = pacientes.get("P3"); paciente.setAtivo("N"); em.flush();
		var sysadmin = usuario(TipoUsuario.SYSADMIN, null, null);
		var adminX = usuario(TipoUsuario.ADMIN_CLINICA, null, x.getId());
		assertEquals(4, animais.listarAutorizado(null, null, null, null, null, Pageable.unpaged(), sysadmin).getTotalElements());
		assertEquals(Set.of("P3"), nomes(animais.listarAutorizado(null, null, null, null, null, Pageable.unpaged(), adminX).getContent()));
		assertDoesNotThrow(() -> animais.buscarPorIdAutorizado(paciente.getId(), adminX));
		assertThrows(AccessDeniedException.class, () -> animais.buscarPorIdAutorizado(pacientes.get("P4").getId(), adminX));
		assertThrows(AccessDeniedException.class, () -> animais.listarPacientesVeterinario(null, null, null, Pageable.unpaged(), adminX));
	}

	@Test
	void vinculoFuturoDeTutorNaoConcedeAcessoAntesDaVigencia() {
		Responsavel tutor = new Responsavel(); tutor.setNome("Tutor"); tutor.setTipo("TUTOR"); tutor.setDocumento("123"); em.persist(tutor);
		LocalDate hoje = LocalDate.of(2026, 9, 9);
		AnimalResponsavelId id = new AnimalResponsavelId(); id.setAnimalId(pacientes.get("P1").getId());
		id.setResponsavelId(tutor.getId()); id.setDataInicio(hoje.plusDays(1));
		AnimalResponsavel vinculo = new AnimalResponsavel(); vinculo.setId(id); vinculo.setAnimal(pacientes.get("P1"));
		vinculo.setResponsavel(tutor); vinculo.setTipoVinculo("TUTOR_LEGAL"); em.persistAndFlush(vinculo);
		assertFalse(vinculos.existsVinculoAtivoVigente(id.getAnimalId(), tutor.getId(), hoje));
		assertTrue(animalRepository.buscarParaResponsavel(tutor.getId(), hoje, null, null, null, null, null, Pageable.unpaged()).isEmpty());
		assertTrue(vinculos.existsVinculoAtivoVigente(id.getAnimalId(), tutor.getId(), hoje.plusDays(1)));
	}

	private Set<String> nomes(List<AnimalResponse> animais) {
		return animais.stream().map(AnimalResponse::nome).collect(Collectors.toSet());
	}
	private UsuarioPrincipal principal(String vet) {
		return usuario(TipoUsuario.VETERINARIO, vets.get(vet).getId(), null);
	}
	private UsuarioPrincipal usuario(TipoUsuario tipo, Long vet, Long clinica) {
		return new UsuarioPrincipal(1L, "Usuario", "user@example.test", "hash", tipo, "S", false, null, vet, clinica);
	}
	private Clinica clinica(String nome, String cnpj) {
		Clinica clinica = new Clinica(); clinica.setNome(nome); clinica.setCnpj(cnpj); return em.persist(clinica);
	}
	private Veterinario vet(String nome, Clinica clinica) {
		Veterinario vet = new Veterinario(); vet.setNome(nome); vet.setCrmv("SP" + nome); vet.setClinica(clinica); return em.persist(vet);
	}
	private Animal paciente(String nome, String criador, Clinica clinica) {
		Animal animal = new Animal(); animal.setNome(nome); animal.setEspecie(especie); animal.setClinica(clinica);
		animal.setVeterinarioCadastro(criador == null ? null : vets.get(criador)); return em.persist(animal);
	}
	private Consulta consulta(String paciente, String vet, String status) {
		Consulta consulta = new Consulta(); consulta.setAnimal(pacientes.get(paciente)); consulta.setVeterinario(vets.get(vet));
		consulta.setStatus(status); consulta.setDataHora(LocalDateTime.of(2026, 9, 1, 10, 0));
		consulta.setModalidade("REMOTA"); consulta.setMotivo("Retorno"); return em.persistAndFlush(consulta);
	}
}
