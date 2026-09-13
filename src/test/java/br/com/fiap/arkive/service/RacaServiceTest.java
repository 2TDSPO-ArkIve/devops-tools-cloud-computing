package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.RacaRequest;
import br.com.fiap.arkive.entity.Especie;
import br.com.fiap.arkive.entity.Raca;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.RacaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class RacaServiceTest {
	private final RacaRepository repository = mock(RacaRepository.class);
	private final EspecieService especies = mock(EspecieService.class);
	private final RacaService service = new RacaService(repository, especies);
	private Especie especie;

	@BeforeEach void setup() {
		especie = new Especie(); especie.setId(1L); especie.setNome("Cachorro");
		when(especies.buscarEntidade(1L)).thenReturn(especie);
		when(repository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
	}

	@Test void normalizaNomeEPorteOpcional() {
		var response = service.criar(new RacaRequest("  Bulldog  ", 1L, " "));
		assertEquals("Bulldog", response.nome()); assertNull(response.porte()); assertEquals(1L, response.especieId());
	}
	@ParameterizedTest @NullAndEmptySource @ValueSource(strings = {" ", "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"})
	void rejeitaNomeInvalido(String nome) {
		assertThrows(BusinessException.class, () -> service.criar(new RacaRequest(nome, 1L, null)));
		verify(repository, never()).saveAndFlush(any());
	}
	@Test void rejeitaEspecieNulaInexistenteOuInativa() {
		assertThrows(BusinessException.class, () -> service.criar(new RacaRequest("Raca", null, null)));
		when(especies.buscarEntidade(2L)).thenThrow(new ResourceNotFoundException("Especie nao encontrada."));
		assertThrows(ResourceNotFoundException.class, () -> service.criar(new RacaRequest("Raca", 2L, null)));
		especie.setAtivo("N");
		assertThrows(BusinessException.class, () -> service.criar(new RacaRequest("Raca", 1L, null)));
	}
	@Test void duplicataNaMesmaEspecieRetornaConflito() {
		when(repository.existeOutraComNome(1L, "Bulldog", null)).thenReturn(true);
		assertEquals(409, assertThrows(BusinessException.class, () -> service.criar(new RacaRequest(" Bulldog ", 1L, null))).getStatus().value());
	}
	@Test void conflitoConcorrenteRetornaErroSeguro() {
		when(repository.saveAndFlush(any())).thenThrow(new org.springframework.dao.DataIntegrityViolationException("INTERNAL_SQL"));
		var ex = assertThrows(BusinessException.class, () -> service.criar(new RacaRequest("Bulldog", 1L, null)));
		assertEquals(409, ex.getStatus().value()); assertFalse(ex.getMessage().contains("INTERNAL_SQL"));
	}
	@Test void atualizarMesmoNomeNaoSeConsideraDuplicata() {
		Raca raca = new Raca(); raca.setId(2L); raca.setEspecie(especie);
		when(repository.findById(2L)).thenReturn(java.util.Optional.of(raca));
		service.atualizar(2L, new RacaRequest("Bulldog", 1L, "MEDIO"));
		verify(repository).existeOutraComNome(1L, "Bulldog", 2L);
	}
}
