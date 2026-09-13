package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.PrescricaoRequest;
import br.com.fiap.arkive.dto.response.PrescricaoResponse;
import br.com.fiap.arkive.domain.consulta.StatusConsulta;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Prescricao;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.AdesaoPrescricaoRepository;
import br.com.fiap.arkive.repository.PrescricaoRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

@Service
@Profile("!local-nodb")
public class PrescricaoService {

	private static final Set<String> VIAS_ADMINISTRACAO = Set.of("ORAL", "INJETAVEL", "TOPICO", "OCULAR", "OTOLOGICO", "OUTRO");

	private final PrescricaoRepository prescricaoRepository;
	private final AdesaoPrescricaoRepository adesaoPrescricaoRepository;
	private final ConsultaService consultaService;
	private final EventoJornadaService eventoJornadaService;
	private final ClinicalAccessService clinicalAccessService;

	public PrescricaoService(
			PrescricaoRepository prescricaoRepository,
			AdesaoPrescricaoRepository adesaoPrescricaoRepository,
			ConsultaService consultaService,
			EventoJornadaService eventoJornadaService,
			ClinicalAccessService clinicalAccessService
	) {
		this.prescricaoRepository = prescricaoRepository;
		this.adesaoPrescricaoRepository = adesaoPrescricaoRepository;
		this.consultaService = consultaService;
		this.eventoJornadaService = eventoJornadaService;
		this.clinicalAccessService = clinicalAccessService;
	}

	@Transactional
	public PrescricaoResponse criar(PrescricaoRequest request) {
		throw new AccessDeniedException("Criacao de prescricao exige veterinario autenticado.");
	}

	@Transactional
	public PrescricaoResponse criar(PrescricaoRequest request, UsuarioPrincipal principal) {
		Prescricao prescricao = new Prescricao();
		aplicarDados(prescricao, request);
		clinicalAccessService.exigirEscritaPrescricaoVeterinario(principal, prescricao);
		exigirConsultaFinalizada(prescricao.getConsulta());
		Prescricao salva = prescricaoRepository.save(prescricao);
		Consulta consulta = salva.getConsulta();
		Long clinicaId = consulta.getClinica() == null ? null : consulta.getClinica().getId();
		eventoJornadaService.registrarEvento(
				"PRESCRICAO_CRIADA",
				"VETERINARIO",
				null,
				consulta.getVeterinario().getId(),
				consulta.getAnimal().getId(),
				clinicaId,
				"Prescricao criada.",
				eventoJornadaService.criarPayload("Prescricao", salva.getId(), "PRESCRICAO_CRIADA")
		);
		return PrescricaoResponse.fromEntity(salva);
	}

	@Transactional(readOnly = true)
	public Page<PrescricaoResponse> listar(Long consultaId, String medicamento, Pageable pageable) {
		return prescricaoRepository.buscar(consultaId, vazioParaNulo(medicamento), pageable)
				.map(PrescricaoResponse::fromEntity);
	}

	@Transactional(readOnly = true)
	public Page<PrescricaoResponse> listarAutorizado(Long consultaId, String medicamento, Pageable pageable, UsuarioPrincipal principal) {
		if (principal == null) {
			throw new AccessDeniedException("Usuario autenticado invalido.");
		}
		String medicamentoFiltro = vazioParaNulo(medicamento);
		return switch (principal.getTipoUsuario()) {
			case SYSADMIN -> prescricaoRepository.buscar(consultaId, medicamentoFiltro, pageable)
					.map(PrescricaoResponse::fromEntity);
			case VETERINARIO -> principal.getVeterinarioId() == null ? Page.empty(pageable) : prescricaoRepository.buscarParaVeterinario(
					principal.getVeterinarioId(),
					consultaId,
					medicamentoFiltro,
					pageable
			).map(PrescricaoResponse::fromEntity);
			case RESPONSAVEL -> principal.getResponsavelId() == null ? Page.empty(pageable) : prescricaoRepository.buscarParaResponsavel(
					principal.getResponsavelId(),
					LocalDate.now(),
					consultaId,
					medicamentoFiltro,
					pageable
			).map(PrescricaoResponse::fromEntity);
			case ADMIN_CLINICA -> principal.getClinicaId() == null ? Page.empty(pageable) : prescricaoRepository.buscarParaClinica(
					principal.getClinicaId(),
					consultaId,
					medicamentoFiltro,
					pageable
			).map(PrescricaoResponse::fromEntity);
		};
	}

	@Transactional(readOnly = true)
	public PrescricaoResponse buscarPorId(Long id) {
		return PrescricaoResponse.fromEntity(buscarEntidade(id));
	}

	@Transactional(readOnly = true)
	public PrescricaoResponse buscarPorIdAutorizado(Long id, UsuarioPrincipal principal) {
		Prescricao prescricao = buscarEntidade(id);
		clinicalAccessService.exigirLeituraPrescricao(principal, prescricao);
		return PrescricaoResponse.fromEntity(prescricao);
	}

	@Transactional
	public PrescricaoResponse atualizar(Long id, PrescricaoRequest request) {
		throw new AccessDeniedException("Atualizacao de prescricao exige veterinario autenticado.");
	}

	@Transactional
	public PrescricaoResponse atualizar(Long id, PrescricaoRequest request, UsuarioPrincipal principal) {
		Prescricao prescricao = buscarEntidade(id);
		clinicalAccessService.exigirEscritaPrescricaoVeterinario(principal, prescricao);
		exigirSemAdesaoRegistrada(prescricao);
		exigirMesmaConsulta(prescricao, request.consultaId());
		aplicarTratamento(prescricao, request);
		return PrescricaoResponse.fromEntity(prescricaoRepository.save(prescricao));
	}

	@Transactional
	public void excluir(Long id) {
		throw new AccessDeniedException("Exclusao de prescricao exige veterinario autenticado.");
	}

	@Transactional
	public void excluir(Long id, UsuarioPrincipal principal) {
		Prescricao prescricao = buscarEntidade(id);
		clinicalAccessService.exigirEscritaPrescricaoVeterinario(principal, prescricao);
		exigirSemAdesaoRegistrada(prescricao);
		try {
			prescricaoRepository.delete(prescricao);
			prescricaoRepository.flush();
		} catch (DataIntegrityViolationException ex) {
			throw new BusinessException("Prescricao nao pode ser excluida porque esta em uso.", HttpStatus.CONFLICT);
		}
	}

	@Transactional(readOnly = true)
	public Prescricao buscarEntidade(Long id) {
		return prescricaoRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Prescricao nao encontrada."));
	}

	private void aplicarDados(Prescricao prescricao, PrescricaoRequest request) {
		validarViaQuandoInformada(request.viaAdministracao());
		validarDataFim(request.dataInicio(), request.dataFim());
		Consulta consulta = consultaService.buscarEntidade(request.consultaId());
		prescricao.setMedicamento(request.medicamento());
		prescricao.setDosagem(request.dosagem());
		prescricao.setFrequencia(request.frequencia());
		prescricao.setViaAdministracao(request.viaAdministracao());
		prescricao.setDataInicio(request.dataInicio());
		prescricao.setDataFim(request.dataFim());
		prescricao.setInstrucoes(request.instrucoes());
		prescricao.setConsulta(consulta);
	}

	private void aplicarTratamento(Prescricao prescricao, PrescricaoRequest request) {
		validarViaQuandoInformada(request.viaAdministracao());
		validarDataFim(request.dataInicio(), request.dataFim());
		prescricao.setMedicamento(request.medicamento());
		prescricao.setDosagem(request.dosagem());
		prescricao.setFrequencia(request.frequencia());
		prescricao.setViaAdministracao(request.viaAdministracao());
		prescricao.setDataInicio(request.dataInicio());
		prescricao.setDataFim(request.dataFim());
		prescricao.setInstrucoes(request.instrucoes());
	}

	private void exigirConsultaFinalizada(Consulta consulta) {
		if (consulta == null || !StatusConsulta.FI.getCodigo().equals(consulta.getStatus())) {
			throw new BusinessException("Prescricao so pode ser criada para consulta finalizada.");
		}
	}

	private void exigirMesmaConsulta(Prescricao prescricao, Long consultaIdRequest) {
		if (consultaIdRequest == null || !prescricao.getConsulta().getId().equals(consultaIdRequest)) {
			throw new BusinessException("Consulta da prescricao nao pode ser alterada.", HttpStatus.CONFLICT);
		}
	}

	private void exigirSemAdesaoRegistrada(Prescricao prescricao) {
		if (adesaoPrescricaoRepository.existsByPrescricaoId(prescricao.getId())) {
			throw new BusinessException("Prescricao nao pode ser alterada ou excluida porque ja possui adesao registrada.", HttpStatus.CONFLICT);
		}
	}

	private void validarViaQuandoInformada(String viaAdministracao) {
		if (viaAdministracao != null && !viaAdministracao.isBlank() && !VIAS_ADMINISTRACAO.contains(viaAdministracao)) {
			throw new BusinessException("Via de administracao invalida.");
		}
	}

	private void validarDataFim(LocalDate dataInicio, LocalDate dataFim) {
		if (dataFim != null && dataFim.isBefore(dataInicio)) {
			throw new BusinessException("Data fim nao pode ser anterior a data inicio.");
		}
	}

	private String vazioParaNulo(String valor) {
		return valor == null || valor.isBlank() ? null : valor;
	}

}
