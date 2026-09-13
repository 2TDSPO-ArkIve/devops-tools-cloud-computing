package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.AdesaoPrescricaoRequest;
import br.com.fiap.arkive.dto.request.RegistrarAdesaoPrescricaoRequest;
import br.com.fiap.arkive.dto.response.AdesaoPrescricaoResponse;
import br.com.fiap.arkive.entity.AdesaoPrescricao;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Prescricao;
import br.com.fiap.arkive.entity.Responsavel;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.AdesaoPrescricaoRepository;
import br.com.fiap.arkive.repository.AnimalRepository;
import br.com.fiap.arkive.repository.ResponsavelRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Profile("!local-nodb")
public class AdesaoPrescricaoService {

	private final AdesaoPrescricaoRepository adesaoPrescricaoRepository;
	private final PrescricaoService prescricaoService;
	private final AnimalRepository animalRepository;
	private final ResponsavelRepository responsavelRepository;
	private final EventoJornadaService eventoJornadaService;
	private final ClinicalAccessService clinicalAccessService;

	public AdesaoPrescricaoService(
			AdesaoPrescricaoRepository adesaoPrescricaoRepository,
			PrescricaoService prescricaoService,
			AnimalRepository animalRepository,
			ResponsavelRepository responsavelRepository,
			EventoJornadaService eventoJornadaService,
			ClinicalAccessService clinicalAccessService
	) {
		this.adesaoPrescricaoRepository = adesaoPrescricaoRepository;
		this.prescricaoService = prescricaoService;
		this.animalRepository = animalRepository;
		this.responsavelRepository = responsavelRepository;
		this.eventoJornadaService = eventoJornadaService;
		this.clinicalAccessService = clinicalAccessService;
	}

	@Transactional
	public AdesaoPrescricaoResponse criar(AdesaoPrescricaoRequest request) {
		throw new AccessDeniedException("Registro de adesao exige responsavel autenticado.");
	}

	@Transactional
	public AdesaoPrescricaoResponse registrar(RegistrarAdesaoPrescricaoRequest request, UsuarioPrincipal principal) {
		validarTomouObrigatorio(request.tomou());
		Prescricao prescricao = prescricaoService.buscarEntidade(request.prescricaoId());
		clinicalAccessService.exigirRegistroAdesaoResponsavel(principal, prescricao);
		LocalDateTime dataRegistro = LocalDateTime.now();
		validarPeriodoPrescricao(prescricao, dataRegistro.toLocalDate());
		Responsavel responsavel = buscarResponsavel(principal.getResponsavelId());
		Animal animal = prescricao.getConsulta().getAnimal();

		AdesaoPrescricao adesao = new AdesaoPrescricao();
		adesao.setPrescricao(prescricao);
		adesao.setAnimal(animal);
		adesao.setResponsavel(responsavel);
		adesao.setDataRegistro(dataRegistro);
		adesao.setTomou(request.tomou());
		adesao.setObservacao(request.observacao());
		AdesaoPrescricao salva = adesaoPrescricaoRepository.save(adesao);
		eventoJornadaService.registrarEvento(
				"ADESAO_REGISTRADA",
				"RESPONSAVEL",
				responsavel.getId(),
				null,
				animal.getId(),
				null,
				"Adesao de prescricao registrada.",
				eventoJornadaService.criarPayload("AdesaoPrescricao", salva.getId(), "ADESAO_REGISTRADA")
		);
		return AdesaoPrescricaoResponse.fromEntity(salva);
	}

	@Transactional(readOnly = true)
	public Page<AdesaoPrescricaoResponse> listar(Long prescricaoId, Long animalId, Long responsavelId, String tomou, Pageable pageable) {
		validarTomouQuandoInformado(tomou);
		return adesaoPrescricaoRepository.buscar(prescricaoId, animalId, responsavelId, vazioParaNulo(tomou), pageable)
				.map(AdesaoPrescricaoResponse::fromEntity);
	}

	@Transactional(readOnly = true)
	public Page<AdesaoPrescricaoResponse> listarAutorizado(
			Long prescricaoId,
			Long animalId,
			Long responsavelId,
			String tomou,
			Pageable pageable,
			UsuarioPrincipal principal
	) {
		if (principal == null) {
			throw new AccessDeniedException("Usuario autenticado invalido.");
		}
		validarTomouQuandoInformado(tomou);
		String tomouFiltro = vazioParaNulo(tomou);
		return switch (principal.getTipoUsuario()) {
			case SYSADMIN -> adesaoPrescricaoRepository.buscar(prescricaoId, animalId, responsavelId, tomouFiltro, pageable)
					.map(AdesaoPrescricaoResponse::fromEntity);
			case VETERINARIO -> principal.getVeterinarioId() == null ? Page.empty(pageable) : adesaoPrescricaoRepository.buscarParaVeterinario(
					principal.getVeterinarioId(),
					prescricaoId,
					animalId,
					responsavelId,
					tomouFiltro,
					pageable
			).map(AdesaoPrescricaoResponse::fromEntity);
			case RESPONSAVEL -> principal.getResponsavelId() == null ? Page.empty(pageable) : adesaoPrescricaoRepository.buscarParaResponsavel(
					principal.getResponsavelId(),
					LocalDate.now(),
					prescricaoId,
					animalId,
					responsavelId,
					tomouFiltro,
					pageable
			).map(AdesaoPrescricaoResponse::fromEntity);
			case ADMIN_CLINICA -> principal.getClinicaId() == null ? Page.empty(pageable) : adesaoPrescricaoRepository.buscarParaClinica(
					principal.getClinicaId(),
					prescricaoId,
					animalId,
					responsavelId,
					tomouFiltro,
					pageable
			).map(AdesaoPrescricaoResponse::fromEntity);
		};
	}

	@Transactional(readOnly = true)
	public AdesaoPrescricaoResponse buscarPorId(Long id) {
		return AdesaoPrescricaoResponse.fromEntity(buscarEntidade(id));
	}

	@Transactional(readOnly = true)
	public AdesaoPrescricaoResponse buscarPorIdAutorizado(Long id, UsuarioPrincipal principal) {
		AdesaoPrescricao adesao = buscarEntidade(id);
		clinicalAccessService.exigirLeituraAdesaoPrescricao(principal, adesao);
		return AdesaoPrescricaoResponse.fromEntity(adesao);
	}

	@Transactional
	public AdesaoPrescricaoResponse atualizar(Long id, AdesaoPrescricaoRequest request) {
		throw new BusinessException("Historico de adesao de prescricao nao pode ser alterado.", HttpStatus.METHOD_NOT_ALLOWED);
	}

	@Transactional
	public void excluir(Long id) {
		throw new BusinessException("Historico de adesao de prescricao nao pode ser excluido.", HttpStatus.METHOD_NOT_ALLOWED);
	}

	private AdesaoPrescricao buscarEntidade(Long id) {
		return adesaoPrescricaoRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Adesao de prescricao nao encontrada."));
	}

	private void aplicarDados(AdesaoPrescricao adesao, AdesaoPrescricaoRequest request, boolean criando) {
		LocalDateTime dataRegistro = criando && request.dataRegistro() == null ? LocalDateTime.now() : request.dataRegistro();
		validarTomouObrigatorio(request.tomou());
		Prescricao prescricao = prescricaoService.buscarEntidade(request.prescricaoId());
		Animal animal = buscarAnimal(request.animalId());
		Responsavel responsavel = request.responsavelId() == null ? null : buscarResponsavel(request.responsavelId());
		if (!prescricao.getConsulta().getAnimal().getId().equals(animal.getId())) {
			throw new BusinessException("Animal da adesao deve ser o mesmo animal da consulta da prescricao.");
		}
		adesao.setPrescricao(prescricao);
		adesao.setAnimal(animal);
		adesao.setResponsavel(responsavel);
		adesao.setDataRegistro(dataRegistro == null ? adesao.getDataRegistro() : dataRegistro);
		adesao.setTomou(request.tomou());
		adesao.setObservacao(request.observacao());
	}

	private void validarPeriodoPrescricao(Prescricao prescricao, LocalDate dataRegistro) {
		if (dataRegistro.isBefore(prescricao.getDataInicio())) {
			throw new BusinessException("Adesao nao pode ser registrada antes do inicio da prescricao.");
		}
		if (prescricao.getDataFim() != null && dataRegistro.isAfter(prescricao.getDataFim())) {
			throw new BusinessException("Adesao nao pode ser registrada apos o fim da prescricao.");
		}
	}

	private Animal buscarAnimal(Long id) {
		return animalRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Animal nao encontrado."));
	}

	private Responsavel buscarResponsavel(Long id) {
		return responsavelRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Responsavel nao encontrado."));
	}

	private void validarTomouObrigatorio(String tomou) {
		if (!"S".equals(tomou) && !"N".equals(tomou)) {
			throw new BusinessException("Tomou deve ser S ou N.");
		}
	}

	private void validarTomouQuandoInformado(String tomou) {
		if (tomou != null && !tomou.isBlank() && !"S".equals(tomou) && !"N".equals(tomou)) {
			throw new BusinessException("Tomou deve ser S ou N.");
		}
	}

	private String vazioParaNulo(String valor) {
		return valor == null || valor.isBlank() ? null : valor;
	}

}
