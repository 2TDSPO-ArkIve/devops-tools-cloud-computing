package br.com.fiap.arkive.domain.consulta;

import br.com.fiap.arkive.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsultaStatusTransitionPolicyTest {

	@Test
	void permiteTransicoesDoFluxoClinico() {
		assertDoesNotThrow(() -> ConsultaStatusTransitionPolicy.validar(StatusConsulta.AG, StatusConsulta.EP));
		assertDoesNotThrow(() -> ConsultaStatusTransitionPolicy.validar(StatusConsulta.AG, StatusConsulta.CA));
		assertDoesNotThrow(() -> ConsultaStatusTransitionPolicy.validar(StatusConsulta.EP, StatusConsulta.AP));
		assertDoesNotThrow(() -> ConsultaStatusTransitionPolicy.validar(StatusConsulta.EP, StatusConsulta.FI));
		assertDoesNotThrow(() -> ConsultaStatusTransitionPolicy.validar(StatusConsulta.EP, StatusConsulta.CA));
		assertDoesNotThrow(() -> ConsultaStatusTransitionPolicy.validar(StatusConsulta.AP, StatusConsulta.FI));
		assertDoesNotThrow(() -> ConsultaStatusTransitionPolicy.validar(StatusConsulta.AP, StatusConsulta.CA));
	}

	@Test
	void rejeitaSaltosInvalidos() {
		assertThrows(BusinessException.class, () -> ConsultaStatusTransitionPolicy.validar(StatusConsulta.AG, StatusConsulta.FI));
		assertThrows(BusinessException.class, () -> ConsultaStatusTransitionPolicy.validar(StatusConsulta.AG, StatusConsulta.AP));
		assertThrows(BusinessException.class, () -> ConsultaStatusTransitionPolicy.validar(StatusConsulta.EP, StatusConsulta.AG));
		assertThrows(BusinessException.class, () -> ConsultaStatusTransitionPolicy.validar(StatusConsulta.AP, StatusConsulta.AG));
	}

	@Test
	void rejeitaQualquerTransicaoAPartirDeTerminais() {
		for (StatusConsulta destino : StatusConsulta.values()) {
			assertThrows(BusinessException.class, () -> ConsultaStatusTransitionPolicy.validar(StatusConsulta.FI, destino));
			assertThrows(BusinessException.class, () -> ConsultaStatusTransitionPolicy.validar(StatusConsulta.CA, destino));
		}
	}
}
