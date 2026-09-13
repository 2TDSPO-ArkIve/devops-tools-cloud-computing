package br.com.fiap.arkive.service.clinical;

import br.com.fiap.arkive.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RenderClinicalSupportProviderTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void mapeiaContratoSnakeCaseDoRenderComFontesGenericas() throws Exception {
		RenderClinicalEngineResponse response = objectMapper.readValue(fixture(), RenderClinicalEngineResponse.class);

		assertEquals("Suspeita de Displasia Coxofemoral em Golden Retriever", response.diagnostico());
		assertEquals("MODERADA", response.severidade());
		assertEquals(65, response.confianca());
		assertEquals(List.of("https://source-one.example", "https://source-two.example"), response.fontesPesquisadas());
	}

	@Test
	void toProviderResultPreservaFontesPesquisadas() throws Exception {
		RenderClinicalEngineResponse response = objectMapper.readValue(fixture(), RenderClinicalEngineResponse.class);

		ClinicalSupportProviderResult result = response.toProviderResult();

		assertEquals(List.of("https://source-one.example", "https://source-two.example"), result.fontesPesquisadas());
	}

	@Test
	void fontesPesquisadasVaziasMapeiamComoListaVazia() throws Exception {
		RenderClinicalEngineResponse response = objectMapper.readValue("""
				{
				  "ds_diagnostico": "Hipotese",
				  "tp_severidade": "MODERADA",
				  "ds_insight_ia": "Insight",
				  "pc_confianca": 65,
				  "fontes_pesquisadas": []
				}
				""", RenderClinicalEngineResponse.class);

		assertEquals(List.of(), response.fontesPesquisadas());
		assertEquals(List.of(), response.toProviderResult().fontesPesquisadas());
	}

	@Test
	void fontesPesquisadasAusentesOuNulasMapeiamComoListaVazia() throws Exception {
		RenderClinicalEngineResponse semCampo = objectMapper.readValue("""
				{
				  "ds_diagnostico": "Hipotese",
				  "tp_severidade": "MODERADA",
				  "ds_insight_ia": "Insight",
				  "pc_confianca": 65
				}
				""", RenderClinicalEngineResponse.class);
		RenderClinicalEngineResponse campoNulo = objectMapper.readValue("""
				{
				  "ds_diagnostico": "Hipotese",
				  "tp_severidade": "MODERADA",
				  "ds_insight_ia": "Insight",
				  "pc_confianca": 65,
				  "fontes_pesquisadas": null
				}
				""", RenderClinicalEngineResponse.class);

		assertEquals(List.of(), semCampo.fontesPesquisadas());
		assertEquals(List.of(), campoNulo.fontesPesquisadas());
	}

	@Test
	void chamaEndpointConfiguradoEUsoContratoValido() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://engine.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RenderClinicalSupportProvider provider = new RenderClinicalSupportProvider(builder.build());
		server.expect(requestTo("https://engine.test/diagnostico/6"))
				.andRespond(withSuccess(fixture(), MediaType.APPLICATION_JSON));

		ClinicalSupportProviderResult result = provider.gerarSuporte(6L);

		assertEquals("Suspeita de Displasia Coxofemoral em Golden Retriever", result.diagnostico());
		assertEquals("MODERADA", result.severidade());
		assertEquals(65, result.confianca());
		assertEquals(List.of("https://source-one.example", "https://source-two.example"), result.fontesPesquisadas());
		server.verify();
	}

	@Test
	void rejeitaPayloadMalformado() {
		RenderClinicalSupportProvider provider = providerRespondendo("""
				{
				  "ds_diagnostico": "",
				  "tp_severidade": "MODERADA",
				  "ds_insight_ia": "Texto",
				  "pc_confianca": 65,
				  "fontes_pesquisadas": []
				}
				""");

		BusinessException exception = assertThrows(BusinessException.class, () -> provider.gerarSuporte(6L));

		assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
	}

	@Test
	void traduzJsonInvalidoComoRespostaInvalida() {
		RenderClinicalSupportProvider provider = providerRespondendo("{");

		BusinessException exception = assertThrows(BusinessException.class, () -> provider.gerarSuporte(6L));

		assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
	}

	@Test
	void traduzLimiteDeQuota() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://engine.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RenderClinicalSupportProvider provider = new RenderClinicalSupportProvider(builder.build());
		server.expect(requestTo("https://engine.test/diagnostico/6"))
				.andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

		BusinessException exception = assertThrows(BusinessException.class, () -> provider.gerarSuporte(6L));

		assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatus());
	}

	@Test
	void traduzErroServidorExterno() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://engine.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RenderClinicalSupportProvider provider = new RenderClinicalSupportProvider(builder.build());
		server.expect(requestTo("https://engine.test/diagnostico/6"))
				.andRespond(withServerError());

		BusinessException exception = assertThrows(BusinessException.class, () -> provider.gerarSuporte(6L));

		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
	}

	private RenderClinicalSupportProvider providerRespondendo(String body) {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://engine.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://engine.test/diagnostico/6"))
				.andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
		return new RenderClinicalSupportProvider(builder.build());
	}

	private String fixture() {
		try (var inputStream = getClass().getResourceAsStream("/fixtures/clinical-engine/diagnostico-success.json")) {
			return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		} catch (Exception ex) {
			throw new IllegalStateException("Fixture de suporte clinico indisponivel.", ex);
		}
	}
}
