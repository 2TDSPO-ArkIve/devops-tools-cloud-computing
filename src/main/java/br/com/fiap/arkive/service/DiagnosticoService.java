package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.DiagnosticoRequest;
import br.com.fiap.arkive.dto.response.DiagnosticoResponse;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Diagnostico;
import br.com.fiap.arkive.entity.Doenca;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.DiagnosticoRepository;
import br.com.fiap.arkive.repository.DoencaRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Service
@Profile("!local-nodb")
public class DiagnosticoService {

	private static final Set<String> SEVERIDADES = Set.of("LEVE", "MODERADA", "GRAVE");

	private final DiagnosticoRepository diagnosticoRepository;
	private final ConsultaService consultaService;
	private final DoencaRepository doencaRepository;
	private final ClinicalAccessService clinicalAccessService;

	public DiagnosticoService(
			DiagnosticoRepository diagnosticoRepository,
			ConsultaService consultaService,
			DoencaRepository doencaRepository,
			ClinicalAccessService clinicalAccessService
	) {
		this.diagnosticoRepository = diagnosticoRepository;
		this.consultaService = consultaService;
		this.doencaRepository = doencaRepository;
		this.clinicalAccessService = clinicalAccessService;
	}

	@Transactional
	public DiagnosticoResponse criar(DiagnosticoRequest request) {
		Diagnostico diagnostico = new Diagnostico();
		aplicarDados(diagnostico, request, true);
		return DiagnosticoResponse.fromEntity(diagnosticoRepository.save(diagnostico));
	}

	@Transactional
	public DiagnosticoResponse criar(DiagnosticoRequest request, UsuarioPrincipal principal) {
		Diagnostico diagnostico = new Diagnostico();
		aplicarDados(diagnostico, request, true);
		clinicalAccessService.exigirEscritaDiagnosticoVeterinario(principal, diagnostico);
		return DiagnosticoResponse.fromEntity(diagnosticoRepository.save(diagnostico));
	}

	public Diagnostico criarConfirmadoPeloVeterinario(
			Consulta consulta,
			String diagnosticoTexto,
			String severidade,
			Long doencaId
	) {
		if (diagnosticoTexto == null || diagnosticoTexto.isBlank()) {
			throw new BusinessException("Informe o diagnostico para finalizar a consulta.");
		}
		validarSeveridadeQuandoInformada(severidade);
		Doenca doenca = doencaId == null ? null : buscarDoenca(doencaId);
		Diagnostico diagnostico = new Diagnostico();
		diagnostico.setDiagnostico(diagnosticoTexto);
		diagnostico.setSeveridade(severidade);
		diagnostico.setConfirmado("S");
		diagnostico.setValidacaoVet("S");
		diagnostico.setConsulta(consulta);
		diagnostico.setDoenca(doenca);
		return diagnosticoRepository.save(diagnostico);
	}

	public Diagnostico criarSuporteClinicoIa(
			Consulta consulta,
			String diagnosticoTexto,
			String severidade,
			String insightIa,
			Integer confianca
	) {
		return criarSuporteClinicoIa(consulta, diagnosticoTexto, severidade, insightIa, confianca, null);
	}

	public Diagnostico criarSuporteClinicoIa(
			Consulta consulta,
			String diagnosticoTexto,
			String severidade,
			String insightIa,
			Integer confianca,
			String fontesIaJson
	) {
		if (diagnosticoTexto == null || diagnosticoTexto.isBlank()) {
			throw new BusinessException("Resposta invalida do motor clinico.", HttpStatus.BAD_GATEWAY);
		}
		validarSeveridadeQuandoInformada(severidade);
		if (insightIa == null || insightIa.isBlank()) {
			throw new BusinessException("Resposta invalida do motor clinico.", HttpStatus.BAD_GATEWAY);
		}
		if (confianca == null || confianca < 0 || confianca > 100) {
			throw new BusinessException("Resposta invalida do motor clinico.", HttpStatus.BAD_GATEWAY);
		}
		Diagnostico diagnostico = new Diagnostico();
		diagnostico.setDiagnostico(diagnosticoTexto);
		diagnostico.setSeveridade(severidade);
		diagnostico.setInsightIa(insightIa);
		diagnostico.setFontesIaJson(fontesIaJson);
		diagnostico.setConfianca(BigDecimal.valueOf(confianca));
		diagnostico.setConfirmado("N");
		diagnostico.setValidacaoVet("N");
		diagnostico.setConsulta(consulta);
		diagnostico.setDoenca(null);
		return diagnosticoRepository.save(diagnostico);
	}

	@Transactional(readOnly = true)
	public Diagnostico buscarSuporteClinico(Long consultaId) {
		return diagnosticoRepository.buscarSuportesClinicos(consultaId, PageRequest.of(0, 1)).stream()
				.findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("Suporte clinico ainda nao gerado para esta consulta."));
	}

	@Transactional(readOnly = true)
	public Page<DiagnosticoResponse> listar(Long consultaId, Long doencaId, String severidade, String confirmado, Pageable pageable) {
		validarSeveridadeQuandoInformada(severidade);
		validarSNQuandoInformado(confirmado, "Confirmado");
		return diagnosticoRepository.buscar(consultaId, doencaId, vazioParaNulo(severidade), vazioParaNulo(confirmado), pageable)
				.map(DiagnosticoResponse::fromEntity);
	}

	@Transactional(readOnly = true)
	public Page<DiagnosticoResponse> listarAutorizado(
			Long consultaId,
			Long doencaId,
			String severidade,
			String confirmado,
			Pageable pageable,
			UsuarioPrincipal principal
	) {
		if (principal == null) {
			throw new AccessDeniedException("Usuario autenticado invalido.");
		}
		validarSeveridadeQuandoInformada(severidade);
		validarSNQuandoInformado(confirmado, "Confirmado");
		return switch (principal.getTipoUsuario()) {
			case SYSADMIN -> diagnosticoRepository.buscar(consultaId, doencaId, vazioParaNulo(severidade), vazioParaNulo(confirmado), pageable)
					.map(DiagnosticoResponse::fromEntity);
			case VETERINARIO -> principal.getVeterinarioId() == null ? Page.empty(pageable) : diagnosticoRepository.buscarParaVeterinario(
					principal.getVeterinarioId(),
					consultaId,
					doencaId,
					vazioParaNulo(severidade),
					vazioParaNulo(confirmado),
					pageable
			).map(DiagnosticoResponse::fromEntity);
			case RESPONSAVEL -> diagnosticoRepository.buscarParaResponsavel(
					principal.getResponsavelId(),
					LocalDate.now(),
					consultaId,
					doencaId,
					vazioParaNulo(severidade),
					vazioParaNulo(confirmado),
					pageable
			).map(DiagnosticoResponse::fromEntity);
			case ADMIN_CLINICA -> principal.getClinicaId() == null ? Page.empty(pageable) : diagnosticoRepository.buscarParaClinica(
					principal.getClinicaId(),
					consultaId,
					doencaId,
					vazioParaNulo(severidade),
					vazioParaNulo(confirmado),
					pageable
			).map(DiagnosticoResponse::fromEntity);
		};
	}

	@Transactional(readOnly = true)
	public DiagnosticoResponse buscarPorId(Long id) {
		return DiagnosticoResponse.fromEntity(buscarEntidade(id));
	}

	@Transactional(readOnly = true)
	public DiagnosticoResponse buscarPorIdAutorizado(Long id, UsuarioPrincipal principal) {
		Diagnostico diagnostico = buscarEntidade(id);
		clinicalAccessService.exigirLeituraDiagnostico(principal, diagnostico);
		return DiagnosticoResponse.fromEntity(diagnostico);
	}

	@Transactional
	public DiagnosticoResponse atualizar(Long id, DiagnosticoRequest request) {
		Diagnostico diagnostico = buscarEntidade(id);
		aplicarDados(diagnostico, request, false);
		return DiagnosticoResponse.fromEntity(diagnosticoRepository.save(diagnostico));
	}

	@Transactional
	public DiagnosticoResponse atualizar(Long id, DiagnosticoRequest request, UsuarioPrincipal principal) {
		Diagnostico diagnostico = buscarEntidade(id);
		aplicarDados(diagnostico, request, false);
		clinicalAccessService.exigirEscritaDiagnosticoVeterinario(principal, diagnostico);
		return DiagnosticoResponse.fromEntity(diagnosticoRepository.save(diagnostico));
	}

	@Transactional
	public void excluir(Long id) {
		Diagnostico diagnostico = buscarEntidade(id);
		try {
			diagnosticoRepository.delete(diagnostico);
			diagnosticoRepository.flush();
		} catch (DataIntegrityViolationException ex) {
			throw new BusinessException("Diagnostico nao pode ser excluido porque esta em uso.", HttpStatus.CONFLICT);
		}
	}

	@Transactional
	public void excluir(Long id, UsuarioPrincipal principal) {
		Diagnostico diagnostico = buscarEntidade(id);
		clinicalAccessService.exigirEscritaDiagnosticoVeterinario(principal, diagnostico);
		try {
			diagnosticoRepository.delete(diagnostico);
			diagnosticoRepository.flush();
		} catch (DataIntegrityViolationException ex) {
			throw new BusinessException("Diagnostico nao pode ser excluido porque esta em uso.", HttpStatus.CONFLICT);
		}
	}

	private Diagnostico buscarEntidade(Long id) {
		return diagnosticoRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Diagnostico nao encontrado."));
	}

	private void aplicarDados(Diagnostico diagnostico, DiagnosticoRequest request, boolean criando) {
		validarCamposControladosPeloServidor(request);
		validarSeveridadeQuandoInformada(request.severidade());
		Consulta consulta = consultaService.buscarEntidade(request.consultaId());
		Doenca doenca = request.doencaId() == null ? null : buscarDoenca(request.doencaId());
		diagnostico.setDiagnostico(request.diagnostico());
		diagnostico.setSeveridade(request.severidade());
		if (criando) {
			diagnostico.setConfirmado("N");
			diagnostico.setValidacaoVet("N");
		}
		diagnostico.setConsulta(consulta);
		diagnostico.setDoenca(doenca);
	}

	private void validarCamposControladosPeloServidor(DiagnosticoRequest request) {
		if (request.confirmado() != null || request.validacaoVet() != null
				|| request.insightIa() != null || request.confianca() != null) {
			throw new BusinessException("Campos de IA, confianca, confirmacao e validacao veterinaria sao controlados pelo servidor.");
		}
	}

	private Doenca buscarDoenca(Long id) {
		return doencaRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Doenca nao encontrada."));
	}

	private void validarSeveridadeQuandoInformada(String severidade) {
		if (severidade != null && !severidade.isBlank() && !SEVERIDADES.contains(severidade)) {
			throw new BusinessException("Severidade deve ser LEVE, MODERADA ou GRAVE.");
		}
	}

	private void validarSNQuandoInformado(String valor, String campo) {
		if (valor != null && !valor.isBlank() && !"S".equals(valor) && !"N".equals(valor)) {
			throw new BusinessException(campo + " deve ser S ou N.");
		}
	}

	private void validarConfianca(BigDecimal confianca) {
		if (confianca != null && (confianca.compareTo(BigDecimal.ZERO) < 0 || confianca.compareTo(BigDecimal.valueOf(100)) > 0)) {
			throw new BusinessException("Confianca deve estar entre 0 e 100.");
		}
	}

	private String vazioParaNulo(String valor) {
		return valor == null || valor.isBlank() ? null : valor;
	}

}
