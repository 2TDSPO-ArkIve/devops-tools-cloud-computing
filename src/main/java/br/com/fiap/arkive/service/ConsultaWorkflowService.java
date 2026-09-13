package br.com.fiap.arkive.service;

import br.com.fiap.arkive.domain.consulta.ConsultaStatusTransitionPolicy;
import br.com.fiap.arkive.domain.consulta.StatusConsulta;
import br.com.fiap.arkive.dto.request.AtualizarNarrativaConsultaRequest;
import br.com.fiap.arkive.dto.request.CancelarConsultaRequest;
import br.com.fiap.arkive.dto.request.FinalizarConsultaRequest;
import br.com.fiap.arkive.dto.response.ConsultaWorkflowResponse;
import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Diagnostico;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.repository.ConsultaRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!local-nodb")
public class ConsultaWorkflowService {

	private final ConsultaService consultaService;
	private final ConsultaRepository consultaRepository;
	private final DiagnosticoService diagnosticoService;
	private final EventoJornadaService eventoJornadaService;
	private final ClinicalAccessService clinicalAccessService;

	public ConsultaWorkflowService(
			ConsultaService consultaService,
			ConsultaRepository consultaRepository,
			DiagnosticoService diagnosticoService,
			EventoJornadaService eventoJornadaService,
			ClinicalAccessService clinicalAccessService
	) {
		this.consultaService = consultaService;
		this.consultaRepository = consultaRepository;
		this.diagnosticoService = diagnosticoService;
		this.eventoJornadaService = eventoJornadaService;
		this.clinicalAccessService = clinicalAccessService;
	}

	@Transactional
	public ConsultaWorkflowResponse iniciar(Long id) {
		Consulta consulta = consultaService.buscarEntidade(id);
		return iniciar(consulta);
	}

	@Transactional
	public ConsultaWorkflowResponse iniciar(Long id, UsuarioPrincipal principal) {
		Consulta consulta = consultaService.buscarEntidade(id);
		clinicalAccessService.exigirEscritaClinicaVeterinario(principal, consulta);
		return iniciar(consulta);
	}

	@Transactional
	public ConsultaWorkflowResponse atualizarNarrativa(Long id, AtualizarNarrativaConsultaRequest request) {
		Consulta consulta = consultaService.buscarEntidade(id);
		return atualizarNarrativa(consulta, request);
	}

	@Transactional
	public ConsultaWorkflowResponse atualizarNarrativa(Long id, AtualizarNarrativaConsultaRequest request, UsuarioPrincipal principal) {
		Consulta consulta = consultaService.buscarEntidade(id);
		clinicalAccessService.exigirEscritaClinicaVeterinario(principal, consulta);
		return atualizarNarrativa(consulta, request);
	}

	private ConsultaWorkflowResponse atualizarNarrativa(Consulta consulta, AtualizarNarrativaConsultaRequest request) {
		StatusConsulta atual = statusAtual(consulta);
		if (atual != StatusConsulta.EP && atual != StatusConsulta.AP) {
			throw new BusinessException("Narrativa clinica so pode ser alterada em consultas em progresso ou aguardando parecer.");
		}
		if (request.narrativa() == null || request.narrativa().isBlank()) {
			throw new BusinessException("Informe a narrativa clinica.");
		}
		consulta.setTranscricao(request.narrativa());
		consultaRepository.save(consulta);
		return ConsultaWorkflowResponse.fromEntity(consulta);
	}

	@Transactional
	public ConsultaWorkflowResponse finalizar(Long id, FinalizarConsultaRequest request) {
		Consulta consulta = consultaService.buscarEntidade(id);
		return finalizar(consulta, request);
	}

	@Transactional
	public ConsultaWorkflowResponse finalizar(Long id, FinalizarConsultaRequest request, UsuarioPrincipal principal) {
		Consulta consulta = consultaService.buscarEntidade(id);
		clinicalAccessService.exigirEscritaClinicaVeterinario(principal, consulta);
		return finalizar(consulta, request);
	}

	private ConsultaWorkflowResponse finalizar(Consulta consulta, FinalizarConsultaRequest request) {
		StatusConsulta atual = statusAtual(consulta);
		if (atual == StatusConsulta.AG) {
			throw new BusinessException("Consulta agendada ainda nao pode ser finalizada.");
		}
		if (atual == StatusConsulta.FI) {
			throw new BusinessException("Consulta finalizada nao pode ser alterada.");
		}
		if (atual == StatusConsulta.CA) {
			throw new BusinessException("Nao e possivel finalizar uma consulta cancelada.");
		}
		ConsultaStatusTransitionPolicy.validar(atual, StatusConsulta.FI);
		validarRecursosAtivos(consulta);
		if (request.diagnostico() == null || request.diagnostico().isBlank()) {
			throw new BusinessException("Informe o diagnostico para finalizar a consulta.");
		}
		Diagnostico diagnostico = diagnosticoService.criarConfirmadoPeloVeterinario(
				consulta,
				request.diagnostico(),
				request.severidade(),
				request.doencaId()
		);
		consulta.setObservacao(request.conclusao());
		consulta.setStatus(StatusConsulta.FI.getCodigo());
		consultaRepository.save(consulta);
		registrarEvento(consulta, "CONSULTA_FINALIZADA", "Consulta finalizada com diagnostico confirmado pelo veterinario.");
		return ConsultaWorkflowResponse.fromEntity(consulta, diagnostico.getId());
	}

	@Transactional
	public ConsultaWorkflowResponse cancelar(Long id, CancelarConsultaRequest request) {
		Consulta consulta = consultaService.buscarEntidade(id);
		return cancelar(consulta, request);
	}

	@Transactional
	public ConsultaWorkflowResponse cancelar(Long id, CancelarConsultaRequest request, UsuarioPrincipal principal) {
		Consulta consulta = consultaService.buscarEntidade(id);
		clinicalAccessService.exigirEscritaClinicaVeterinario(principal, consulta);
		return cancelar(consulta, request);
	}

	private ConsultaWorkflowResponse iniciar(Consulta consulta) {
		StatusConsulta atual = statusAtual(consulta);
		if (atual == StatusConsulta.EP) {
			throw new BusinessException("Consulta ja foi iniciada.");
		}
		ConsultaStatusTransitionPolicy.validar(atual, StatusConsulta.EP);
		validarRecursosAtivos(consulta);
		consulta.setStatus(StatusConsulta.EP.getCodigo());
		consultaRepository.save(consulta);
		registrarEvento(consulta, "CONSULTA_INICIADA", "Consulta iniciada.");
		return ConsultaWorkflowResponse.fromEntity(consulta);
	}

	private ConsultaWorkflowResponse cancelar(Consulta consulta, CancelarConsultaRequest request) {
		StatusConsulta atual = statusAtual(consulta);
		if (atual == StatusConsulta.FI) {
			throw new BusinessException("Consulta finalizada nao pode ser cancelada.");
		}
		if (atual == StatusConsulta.CA) {
			throw new BusinessException("Consulta ja esta cancelada.");
		}
		if (request.motivo() == null || request.motivo().isBlank()) {
			throw new BusinessException("Informe o motivo do cancelamento.");
		}
		ConsultaStatusTransitionPolicy.validar(atual, StatusConsulta.CA);
		validarRecursosAtivos(consulta);
		consulta.setObservacao(formatarObservacaoCancelamento(consulta.getObservacao(), request.motivo()));
		consulta.setStatus(StatusConsulta.CA.getCodigo());
		consultaRepository.save(consulta);
		registrarEvento(consulta, "CONSULTA_CANCELADA", "Consulta cancelada.");
		return ConsultaWorkflowResponse.fromEntity(consulta);
	}

	private StatusConsulta statusAtual(Consulta consulta) {
		return StatusConsulta.fromCodigo(consulta.getStatus());
	}

	private void validarRecursosAtivos(Consulta consulta) {
		if (!"S".equals(consulta.getAnimal().getAtivo())) {
			throw new BusinessException("Animal da consulta deve estar ativo.");
		}
		if (!"S".equals(consulta.getVeterinario().getAtivo())) {
			throw new BusinessException("Veterinario da consulta deve estar ativo.");
		}
		Clinica clinica = consulta.getClinica();
		if (clinica != null && !"S".equals(clinica.getAtivo())) {
			throw new BusinessException("Clinica da consulta deve estar ativa.");
		}
	}

	private void registrarEvento(Consulta consulta, String tipoEvento, String contexto) {
		Long clinicaId = consulta.getClinica() == null ? null : consulta.getClinica().getId();
		eventoJornadaService.registrarEvento(
				tipoEvento,
				"VETERINARIO",
				null,
				consulta.getVeterinario().getId(),
				consulta.getAnimal().getId(),
				clinicaId,
				contexto,
				eventoJornadaService.criarPayload("Consulta", consulta.getId(), tipoEvento)
		);
	}

	private String formatarObservacaoCancelamento(String observacaoAtual, String motivo) {
		String cancelamento = "Cancelamento: " + motivo;
		if (observacaoAtual == null || observacaoAtual.isBlank()) {
			return cancelamento;
		}
		return observacaoAtual + "\n" + cancelamento;
	}
}
