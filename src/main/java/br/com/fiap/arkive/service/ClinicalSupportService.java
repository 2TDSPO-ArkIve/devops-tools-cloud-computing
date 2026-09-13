package br.com.fiap.arkive.service;

import br.com.fiap.arkive.domain.consulta.ConsultaStatusTransitionPolicy;
import br.com.fiap.arkive.domain.consulta.StatusConsulta;
import br.com.fiap.arkive.dto.response.ClinicalSupportResponse;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Diagnostico;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.clinical.ClinicalSupportProvider;
import br.com.fiap.arkive.service.clinical.ClinicalSupportProviderResult;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Profile("!local-nodb")
public class ClinicalSupportService {

	private final ConsultaService consultaService;
	private final DiagnosticoService diagnosticoService;
	private final ClinicalAccessService clinicalAccessService;
	private final ClinicalSupportProvider clinicalSupportProvider;
	private final ClinicalSupportPersistenceService persistenceService;
	private final Set<Long> consultasEmProcessamento = ConcurrentHashMap.newKeySet();

	public ClinicalSupportService(
			ConsultaService consultaService,
			DiagnosticoService diagnosticoService,
			ClinicalAccessService clinicalAccessService,
			ClinicalSupportProvider clinicalSupportProvider,
			ClinicalSupportPersistenceService persistenceService
	) {
		this.consultaService = consultaService;
		this.diagnosticoService = diagnosticoService;
		this.clinicalAccessService = clinicalAccessService;
		this.clinicalSupportProvider = clinicalSupportProvider;
		this.persistenceService = persistenceService;
	}

	public ClinicalSupportResponse gerarSuporte(Long consultaId, UsuarioPrincipal principal) {
		Consulta consulta = consultaService.buscarEntidade(consultaId);
		clinicalAccessService.exigirEscritaClinicaVeterinario(principal, consulta);
		StatusConsulta atual = StatusConsulta.fromCodigo(consulta.getStatus());
		if (atual == StatusConsulta.AP) {
			Diagnostico existente = diagnosticoService.buscarSuporteClinico(consultaId);
			return ClinicalSupportResponse.fromEntities(consulta, existente);
		}
		if (atual != StatusConsulta.EP) {
			throw new BusinessException("Suporte clinico so pode ser gerado para consulta em progresso.");
		}
		ConsultaStatusTransitionPolicy.validar(atual, StatusConsulta.AP);
		if (!consultasEmProcessamento.add(consultaId)) {
			throw new BusinessException("Suporte clinico ja esta sendo processado para esta consulta.", HttpStatus.CONFLICT);
		}
		try {
			ClinicalSupportProviderResult result = clinicalSupportProvider.gerarSuporte(consultaId);
			validarResultado(result);
			return persistenceService.persistirSuporte(consultaId, result, principal);
		} finally {
			consultasEmProcessamento.remove(consultaId);
		}
	}

	public ClinicalSupportResponse buscarSuporte(Long consultaId, UsuarioPrincipal principal) {
		Consulta consulta = consultaService.buscarEntidade(consultaId);
		clinicalAccessService.exigirEscritaClinicaVeterinario(principal, consulta);
		Diagnostico diagnostico = diagnosticoService.buscarSuporteClinico(consultaId);
		return ClinicalSupportResponse.fromEntities(consulta, diagnostico);
	}

	private void validarResultado(ClinicalSupportProviderResult result) {
		if (result == null
				|| result.diagnostico() == null || result.diagnostico().isBlank()
				|| result.severidade() == null || result.severidade().isBlank()
				|| result.insightIa() == null || result.insightIa().isBlank()
				|| result.confianca() == null
				|| result.confianca() < 0
				|| result.confianca() > 100) {
			throw new BusinessException("Resposta invalida do motor clinico.", HttpStatus.BAD_GATEWAY);
		}
	}
}
