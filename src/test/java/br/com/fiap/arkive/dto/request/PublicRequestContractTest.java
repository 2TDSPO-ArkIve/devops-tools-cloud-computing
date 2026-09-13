package br.com.fiap.arkive.dto.request;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicRequestContractTest {

	@Test
	void diagnosticoPublicoNaoExpoeCamposControladosPeloServidor() {
		Set<String> campos = campos(SalvarDiagnosticoRequest.class);

		assertTrue(campos.contains("diagnostico"));
		assertTrue(campos.contains("severidade"));
		assertTrue(campos.contains("consultaId"));
		assertTrue(campos.contains("doencaId"));
		assertFalse(campos.contains("insightIa"));
		assertFalse(campos.contains("confianca"));
		assertFalse(campos.contains("confirmado"));
		assertFalse(campos.contains("validacaoVet"));
	}

	@Test
	void adesaoPublicaNaoExpoeIdentidadeOuTimestampControladosPeloServidor() {
		Set<String> campos = campos(RegistrarAdesaoPrescricaoRequest.class);

		assertTrue(campos.contains("prescricaoId"));
		assertTrue(campos.contains("tomou"));
		assertTrue(campos.contains("observacao"));
		assertFalse(campos.contains("responsavelId"));
		assertFalse(campos.contains("animalId"));
		assertFalse(campos.contains("dataRegistro"));
	}

	private Set<String> campos(Class<? extends Record> tipo) {
		return Arrays.stream(tipo.getRecordComponents())
				.map(component -> component.getName())
				.collect(Collectors.toSet());
	}

}
