package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Diagnostico;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClinicalSupportResponseTest {

	@Test
	void leJsonPersistidoDeFontesComoLista() {
		Diagnostico diagnostico = diagnostico("[\"https://source-one.example\",\"https://source-two.example\"]");

		ClinicalSupportResponse response = ClinicalSupportResponse.fromEntities(consulta("AP"), diagnostico);

		assertEquals(List.of("https://source-one.example", "https://source-two.example"), response.fontesPesquisadas());
	}

	@Test
	void fontesNulasOuEmBrancoRetornamListaVazia() {
		assertEquals(List.of(), ClinicalSupportResponse.fromEntities(consulta("AP"), diagnostico(null)).fontesPesquisadas());
		assertEquals(List.of(), ClinicalSupportResponse.fromEntities(consulta("AP"), diagnostico("")).fontesPesquisadas());
		assertEquals(List.of(), ClinicalSupportResponse.fromEntities(consulta("AP"), diagnostico("   ")).fontesPesquisadas());
	}

	@Test
	void jsonPersistidoMalformadoRetornaListaVazia() {
		ClinicalSupportResponse response = ClinicalSupportResponse.fromEntities(consulta("AP"), diagnostico("{"));

		assertEquals(List.of(), response.fontesPesquisadas());
	}

	private Consulta consulta(String status) {
		Consulta consulta = new Consulta();
		consulta.setId(63L);
		consulta.setStatus(status);
		return consulta;
	}

	private Diagnostico diagnostico(String fontesIaJson) {
		Diagnostico diagnostico = new Diagnostico();
		diagnostico.setId(50L);
		diagnostico.setDiagnostico("Hipotese");
		diagnostico.setSeveridade("MODERADA");
		diagnostico.setInsightIa("Insight clinico");
		diagnostico.setConfianca(BigDecimal.valueOf(55));
		diagnostico.setFontesIaJson(fontesIaJson);
		return diagnostico;
	}
}
