package br.com.fiap.arkive.service.clinical;

import br.com.fiap.arkive.exception.BusinessException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class RenderClinicalSupportProvider implements ClinicalSupportProvider {

	private final RestClient restClient;

	public RenderClinicalSupportProvider(@Qualifier("clinicalEngineRestClient") RestClient restClient) {
		this.restClient = restClient;
	}

	@Override
	public ClinicalSupportProviderResult gerarSuporte(Long consultaId) {
		try {
			RenderClinicalEngineResponse response = restClient.get()
					.uri("/diagnostico/{consultaId}", consultaId)
					.retrieve()
					.onStatus(status -> status.value() == 429, (request, responseBody) -> {
						throw new BusinessException("Limite do motor clinico atingido. Tente novamente mais tarde.", HttpStatus.TOO_MANY_REQUESTS);
					})
					.onStatus(status -> status.is5xxServerError(), (request, responseBody) -> {
						throw new BusinessException("Motor clinico temporariamente indisponivel.", HttpStatus.SERVICE_UNAVAILABLE);
					})
					.body(RenderClinicalEngineResponse.class);
			if (response == null) {
				throw new BusinessException("Resposta invalida do motor clinico.", HttpStatus.BAD_GATEWAY);
			}
			validarResponse(response);
			return response.toProviderResult();
		} catch (ResourceAccessException ex) {
			throw new BusinessException("Motor clinico temporariamente indisponivel.", HttpStatus.SERVICE_UNAVAILABLE);
		} catch (RestClientResponseException ex) {
			if (ex.getStatusCode().value() == 429) {
				throw new BusinessException("Limite do motor clinico atingido. Tente novamente mais tarde.", HttpStatus.TOO_MANY_REQUESTS);
			}
			if (ex.getStatusCode().is5xxServerError()) {
				throw new BusinessException("Motor clinico temporariamente indisponivel.", HttpStatus.SERVICE_UNAVAILABLE);
			}
			throw new BusinessException("Falha ao consultar o motor clinico.", HttpStatus.BAD_GATEWAY);
		} catch (RestClientException ex) {
			throw new BusinessException("Resposta invalida do motor clinico.", HttpStatus.BAD_GATEWAY);
		}
	}

	private void validarResponse(RenderClinicalEngineResponse response) {
		if (response.diagnostico() == null || response.diagnostico().isBlank()
				|| response.severidade() == null || response.severidade().isBlank()
				|| response.insightIa() == null || response.insightIa().isBlank()
				|| response.confianca() == null
				|| response.confianca() < 0
				|| response.confianca() > 100) {
			throw new BusinessException("Resposta invalida do motor clinico.", HttpStatus.BAD_GATEWAY);
		}
	}
}
