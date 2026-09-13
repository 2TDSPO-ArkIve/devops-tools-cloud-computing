package br.com.fiap.arkive.repository;

import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Diagnostico;
import br.com.fiap.arkive.entity.Especie;
import br.com.fiap.arkive.entity.Veterinario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"})
class AnimalHistoricoRepositoryTest {
	@Autowired TestEntityManager em;
	@Autowired ConsultaRepository consultas;
	@Autowired DiagnosticoRepository diagnosticos;
	private int crmv;

	@Test
	void historicoRestringeAnimalOrdenaDataEIdDescEMantemConsultasSemClinica() {
		Animal animal = animal("Paciente");
		Consulta antiga = consulta(animal, "2025-01-01T09:00:00", "FI");
		Consulta recente = consulta(animal, "2026-01-01T09:00:00", "FI");
		Consulta empate = consulta(animal, "2026-01-01T09:00:00", "CA");
		Consulta futura = consulta(animal, "2027-01-01T09:00:00", "AG");
		consulta(animal("Outro paciente"), "2028-01-01T09:00:00", "FI");
		em.flush(); em.clear();
		var resultado = consultas.buscarHistoricoPorAnimal(animal.getId());
		assertEquals(List.of(futura.getId(), empate.getId(), recente.getId(), antiga.getId()),
				resultado.stream().map(Consulta::getId).toList());
		assertTrue(resultado.stream().allMatch(c -> c.getClinica() == null));
		assertEquals("Vet", resultado.get(0).getVeterinario().getNome());
		assertTrue(consultas.buscarHistoricoPorAnimal(-1L).isEmpty());
	}

	@Test
	void diagnosticoExigeAmbasConfirmacoesENuncaEscolheHipoteseIaMaisRecente() {
		Consulta consulta = consulta(animal("Paciente"), "2026-01-01T09:00:00", "FI");
		diagnostico(consulta, "Anterior aprovado", "S", "S");
		Diagnostico aprovado = diagnostico(consulta, "Diagnostico final", "S", "S");
		diagnostico(consulta, "SECRET_AI", "N", "N");
		diagnostico(consulta, "SECRET_SEM_VALIDACAO", "S", "N");
		diagnostico(consulta, "SECRET_SEM_CONFIRMACAO", "N", "S");
		Consulta outra = consulta(animal("Outro"), "2026-01-01T09:00:00", "FI");
		diagnostico(outra, "SECRET_OUTRO_PACIENTE", "S", "S");
		em.flush(); em.clear();
		var resultado = diagnosticos.buscarDiagnosticosConfirmadosVeterinario(consulta.getId(), PageRequest.of(0, 1));
		assertEquals(1, resultado.size());
		assertEquals(aprovado.getId(), resultado.get(0).getId());
		assertEquals("Diagnostico final", resultado.get(0).getDiagnostico());
	}

	private Animal animal(String nome) {
		Especie especie = new Especie(); especie.setNome(nome + " especie"); em.persist(especie);
		Animal animal = new Animal(); animal.setNome(nome); animal.setEspecie(especie);
		return em.persist(animal);
	}

	private Consulta consulta(Animal animal, String data, String status) {
		Veterinario vet = new Veterinario(); vet.setNome("Vet"); vet.setCrmv("SP" + ++crmv); em.persist(vet);
		Consulta consulta = new Consulta(); consulta.setAnimal(animal); consulta.setVeterinario(vet);
		consulta.setDataHora(LocalDateTime.parse(data)); consulta.setModalidade("REMOTA");
		consulta.setMotivo("Retorno"); consulta.setStatus(status);
		return em.persist(consulta);
	}

	private Diagnostico diagnostico(Consulta consulta, String texto, String confirmado, String validacao) {
		Diagnostico diagnostico = new Diagnostico(); diagnostico.setConsulta(consulta); diagnostico.setDiagnostico(texto);
		diagnostico.setConfirmado(confirmado); diagnostico.setValidacaoVet(validacao);
		return em.persist(diagnostico);
	}
}
