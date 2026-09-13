package br.com.fiap.arkive.domain.consulta;

import br.com.fiap.arkive.exception.BusinessException;

import java.util.Map;
import java.util.Set;

public class ConsultaStatusTransitionPolicy {

	private static final Map<StatusConsulta, Set<StatusConsulta>> TRANSICOES = Map.of(
			StatusConsulta.AG, Set.of(StatusConsulta.EP, StatusConsulta.CA),
			StatusConsulta.EP, Set.of(StatusConsulta.AP, StatusConsulta.FI, StatusConsulta.CA),
			StatusConsulta.AP, Set.of(StatusConsulta.FI, StatusConsulta.CA),
			StatusConsulta.FI, Set.of(),
			StatusConsulta.CA, Set.of()
	);

	private ConsultaStatusTransitionPolicy() {
	}

	public static boolean permite(StatusConsulta atual, StatusConsulta destino) {
		return TRANSICOES.getOrDefault(atual, Set.of()).contains(destino);
	}

	public static void validar(StatusConsulta atual, StatusConsulta destino) {
		if (!permite(atual, destino)) {
			throw new BusinessException("Transicao de status invalida.");
		}
	}
}
