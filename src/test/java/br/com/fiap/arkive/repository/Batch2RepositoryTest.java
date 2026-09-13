package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Pageable;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"})
class Batch2RepositoryTest {
	@Autowired TestEntityManager em;
	@Autowired RacaRepository racas;
	@Autowired ResponsavelRepository responsaveis;
	@Autowired AnimalRepository animais;
	@Autowired AnimalResponsavelRepository vinculos;
	@Autowired ConsultaRepository consultas;
	@Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;

	@Test
	void nascimentoEEnderecoPersistemComoCamposOpcionais() {
		Animal animal = new Animal(); animal.setNome("Paciente"); animal.setEspecie(especie("Especie persistencia"));
		em.persistAndFlush(animal); em.clear();
		assertNull(animais.findById(animal.getId()).orElseThrow().getDataNascimento());
		animal = animais.findById(animal.getId()).orElseThrow();
		animal.setDataNascimento(java.time.LocalDate.parse("2021-04-17"));
		Veterinario vet = new Veterinario(); vet.setNome("Vet"); vet.setCrmv("SP123"); em.persist(vet);
		Consulta consulta = new Consulta(); consulta.setAnimal(animal); consulta.setVeterinario(vet);
		consulta.setDataHora(java.time.LocalDateTime.now()); consulta.setModalidade("PRESENCIAL"); consulta.setMotivo("Retorno");
		em.persistAndFlush(consulta); em.clear();
		assertNull(consultas.findById(consulta.getId()).orElseThrow().getEndereco());
		consulta = consultas.findById(consulta.getId()).orElseThrow(); consulta.setEndereco("Rua original");
		em.flush(); em.clear();
		assertEquals("Rua original", consultas.findById(consulta.getId()).orElseThrow().getEndereco());
		assertEquals(java.time.LocalDate.parse("2021-04-17"), animais.findById(animal.getId()).orElseThrow().getDataNascimento());
	}

	@Test
	void vinculoFuturoNaoConcedeAcessoAoTutor() {
		Animal animal = new Animal(); animal.setNome("Paciente futuro"); animal.setEspecie(especie("Especie futura")); em.persist(animal);
		Responsavel tutor = responsavel("Tutor futuro", "futuro@example.test", "4", "S");
		AnimalResponsavelId id = new AnimalResponsavelId(); id.setAnimalId(animal.getId()); id.setResponsavelId(tutor.getId()); id.setDataInicio(java.time.LocalDate.now().plusDays(1));
		AnimalResponsavel ar = new AnimalResponsavel(); ar.setId(id); ar.setAnimal(animal); ar.setResponsavel(tutor); ar.setTipoVinculo("TUTOR_LEGAL"); ar.setPrincipal("S"); em.persistAndFlush(ar);
		assertFalse(vinculos.existsVinculoAtivoVigente(animal.getId(), tutor.getId(), java.time.LocalDate.now()));
		assertTrue(animais.buscarParaResponsavel(tutor.getId(), java.time.LocalDate.now(), null, null, null, null, null, Pageable.unpaged()).isEmpty());
	}

	@Test
	@org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
	void criacoesConcorrentesMantemUmUnicoPrincipal() throws Exception {
		var tx = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
		Long[] ids = tx.execute(status -> {
			Animal animal = new Animal(); animal.setNome("Paciente concorrente"); animal.setEspecie(especie("Especie concorrente")); em.persist(animal);
			return new Long[] {animal.getId(), responsavel("Tutor concorrente A", null, "conc-a", "S").getId(), responsavel("Tutor concorrente B", null, "conc-b", "S").getId()};
		});
		var acesso = new br.com.fiap.arkive.service.ClinicalAccessService(vinculos, consultas, org.mockito.Mockito.mock(br.com.fiap.arkive.service.VeterinarioService.class));
		var service = new br.com.fiap.arkive.service.AnimalResponsavelService(vinculos, animais, responsaveis, org.mockito.Mockito.mock(br.com.fiap.arkive.service.EventoJornadaService.class), acesso);
		var principal = new br.com.fiap.arkive.security.UsuarioPrincipal(1L, "Admin", "admin@example.test", "hash", TipoUsuario.SYSADMIN, "S", false, null, null, null);
		var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
		var inicio = new java.util.concurrent.CountDownLatch(1);
		try {
			java.util.List<java.util.concurrent.Future<?>> resultados = new java.util.ArrayList<>();
			for (int i = 1; i <= 2; i++) {
				Long tutorId = ids[i];
				resultados.add(executor.submit(() -> {
					try { inicio.await(); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException(ex); }
					tx.executeWithoutResult(status -> service.criar(new br.com.fiap.arkive.dto.request.AnimalResponsavelRequest(ids[0], tutorId, "TUTOR_LEGAL", null, null, "S", null), principal));
				}));
			}
			inicio.countDown();
			for (var resultado : resultados) resultado.get(10, java.util.concurrent.TimeUnit.SECONDS);
			assertEquals(1, tx.execute(status -> vinculos.listarPrincipaisAtivos(ids[0], "S", "S").size()).intValue());
		} finally { executor.shutdownNow(); }
	}

	@Test
	void consultaRealNuncaRetornaRacaDeOutraEspecie() {
		Especie cao = especie("Cachorro");
		Especie macaco = especie("Macaco");
		Raca bulldog = raca(cao, "Bulldog");
		Raca outra = raca(macaco, "Outra raca");
		em.flush();
		em.clear();
		assertEquals(java.util.List.of(bulldog.getId()), racas.buscar(null, cao.getId(), Pageable.unpaged()).map(Raca::getId).getContent());
		assertEquals(java.util.List.of(outra.getId()), racas.buscar(null, macaco.getId(), Pageable.unpaged()).map(Raca::getId).getContent());
		assertTrue(racas.buscar("Bulldog", macaco.getId(), Pageable.unpaged()).isEmpty());
		assertTrue(racas.existeOutraComNome(cao.getId(), "Bulldog", null));
		assertFalse(racas.existeOutraComNome(macaco.getId(), "Bulldog", null));
	}

	@Test
	void buscaTutorPorNomeOuEmailExatoSomenteAtivos() {
		Responsavel ana = responsavel("Ana Silva", "ana@example.test", "1", "S");
		responsavel("Ana Inativa", "inativa@example.test", "2", "N");
		responsavel("Outro Tutor", "outro@example.test", "3", "S");
		em.flush();
		assertEquals(java.util.List.of(ana.getId()), responsaveis.buscarParaVinculo("ana", Pageable.unpaged()).map(Responsavel::getId).getContent());
		assertEquals(1, responsaveis.buscarParaVinculo("ANA@example.test", Pageable.unpaged()).getTotalElements());
		assertTrue(responsaveis.buscarParaVinculo("example.test", Pageable.unpaged()).isEmpty());
		assertTrue(responsaveis.buscarParaVinculo("%", Pageable.unpaged()).isEmpty());
	}

	private Especie especie(String nome) {
		Especie especie = new Especie(); especie.setNome(nome); return em.persist(especie);
	}
	private Raca raca(Especie especie, String nome) {
		Raca raca = new Raca(); raca.setNome(nome); raca.setEspecie(especie); return em.persist(raca);
	}
	private Responsavel responsavel(String nome, String email, String documento, String ativo) {
		Responsavel r = new Responsavel(); r.setNome(nome); r.setEmail(email); r.setDocumento(documento);
		r.setTipo("TUTOR"); r.setAtivo(ativo); return em.persist(r);
	}
}
