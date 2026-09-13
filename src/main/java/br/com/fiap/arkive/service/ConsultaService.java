package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.ConsultaRequest;
import br.com.fiap.arkive.dto.response.ConsultaResponse;
import br.com.fiap.arkive.domain.consulta.StatusConsulta;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.AnimalRepository;
import br.com.fiap.arkive.repository.ClinicaRepository;
import br.com.fiap.arkive.repository.ConsultaRepository;
import br.com.fiap.arkive.repository.VeterinarioRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Set;

@Service
@Profile("!local-nodb")
public class ConsultaService {

	private static final Set<String> MODALIDADES = Set.of("PRESENCIAL", "REMOTA");

	private final ConsultaRepository consultaRepository;
	private final AnimalRepository animalRepository;
	private final VeterinarioRepository veterinarioRepository;
	private final ClinicaRepository clinicaRepository;
	private final EventoJornadaService eventoJornadaService;
	private final ClinicalAccessService clinicalAccessService;
	private final Clock clock;

	public ConsultaService(
			ConsultaRepository consultaRepository,
			AnimalRepository animalRepository,
			VeterinarioRepository veterinarioRepository,
			ClinicaRepository clinicaRepository,
			EventoJornadaService eventoJornadaService,
			ClinicalAccessService clinicalAccessService,
			@Qualifier("businessClock") Clock clock
	) {
		this.clock = clock;
		this.consultaRepository = consultaRepository;
		this.animalRepository = animalRepository;
		this.veterinarioRepository = veterinarioRepository;
		this.clinicaRepository = clinicaRepository;
		this.eventoJornadaService = eventoJornadaService;
		this.clinicalAccessService = clinicalAccessService;
	}

	@Transactional
	public ConsultaResponse criar(ConsultaRequest request) {
		throw new AccessDeniedException("Criacao de consulta exige veterinario autenticado.");
	}

	@Transactional
	public ConsultaResponse criar(ConsultaRequest request, UsuarioPrincipal principal) {
		exigirVeterinarioAutenticado(principal);
		if (request.veterinarioId() != null && !Objects.equals(principal.getVeterinarioId(), request.veterinarioId())) {
			throw new BusinessException("Consulta deve ser criada para o veterinario autenticado.", HttpStatus.CONFLICT);
		}
		Consulta consulta = new Consulta();
		aplicarDados(consulta, comVeterinarioAutenticado(request, principal.getVeterinarioId()), true);
		clinicalAccessService.exigirLeituraAnimal(principal, consulta.getAnimal());
		validarClinicaDaConsultaParaVeterinario(consulta);
		Consulta salva = consultaRepository.save(consulta);
		Long clinicaId = salva.getClinica() == null ? null : salva.getClinica().getId();
		eventoJornadaService.registrarEvento(
				"CONSULTA_CRIADA",
				"VETERINARIO",
				null,
				salva.getVeterinario().getId(),
				salva.getAnimal().getId(),
				clinicaId,
				"Consulta criada.",
				eventoJornadaService.criarPayload("Consulta", salva.getId(), "CONSULTA_CRIADA")
		);
		return ConsultaResponse.fromEntity(salva);
	}

	@Transactional(readOnly = true)
	public Page<ConsultaResponse> listar(Long animalId, Long veterinarioId, Long clinicaId, String status, String modalidade, Pageable pageable) {
		validarStatusQuandoInformado(status);
		validarModalidadeQuandoInformada(modalidade);
		return consultaRepository.buscar(animalId, veterinarioId, clinicaId, vazioParaNulo(status), vazioParaNulo(modalidade), pageable)
				.map(ConsultaResponse::fromEntity);
	}

	@Transactional(readOnly = true)
	public Page<ConsultaResponse> listarAutorizado(
			Long animalId,
			Long veterinarioId,
			Long clinicaId,
			String status,
			String modalidade,
			Pageable pageable,
			UsuarioPrincipal principal
	) {
		if (principal == null) {
			throw new AccessDeniedException("Usuario autenticado invalido.");
		}
		validarStatusQuandoInformado(status);
		validarModalidadeQuandoInformada(modalidade);
		return switch (principal.getTipoUsuario()) {
			case SYSADMIN -> consultaRepository.buscar(animalId, veterinarioId, clinicaId, vazioParaNulo(status), vazioParaNulo(modalidade), pageable)
					.map(ConsultaResponse::fromEntity);
			case VETERINARIO -> listarParaVeterinario(animalId, veterinarioId, clinicaId, status, modalidade, pageable, principal.getVeterinarioId());
			case RESPONSAVEL -> consultaRepository.buscarParaResponsavel(
					principal.getResponsavelId(),
					LocalDate.now(),
					animalId,
					veterinarioId,
					clinicaId,
					vazioParaNulo(status),
					vazioParaNulo(modalidade),
					pageable
			).map(ConsultaResponse::fromEntity);
			case ADMIN_CLINICA -> listarParaClinica(animalId, veterinarioId, clinicaId, status, modalidade, pageable, principal.getClinicaId());
		};
	}

	@Transactional(readOnly = true)
	public ConsultaResponse buscarPorId(Long id) {
		return ConsultaResponse.fromEntity(buscarEntidade(id));
	}

	@Transactional(readOnly = true)
	public ConsultaResponse buscarPorIdAutorizado(Long id, UsuarioPrincipal principal) {
		Consulta consulta = buscarEntidade(id);
		clinicalAccessService.exigirLeituraConsulta(principal, consulta);
		return ConsultaResponse.fromEntity(consulta);
	}

	@Transactional
	public ConsultaResponse atualizar(Long id, ConsultaRequest request) {
		throw new AccessDeniedException("Atualizacao de consulta exige veterinario autenticado.");
	}

	@Transactional
	public ConsultaResponse atualizar(Long id, ConsultaRequest request, UsuarioPrincipal principal) {
		Consulta consulta = buscarEntidade(id);
		clinicalAccessService.exigirEscritaClinicaVeterinario(principal, consulta);
		ConsultaRequest requestAutorizado = comVeterinarioAtualQuandoOmitido(consulta, request);
		exigirAssociacoesImutaveis(consulta, requestAutorizado);
		aplicarDados(consulta, requestAutorizado, false);
		return ConsultaResponse.fromEntity(consultaRepository.save(consulta));
	}

	@Transactional
	public void excluir(Long id) {
		throw new AccessDeniedException("Exclusao de consulta exige veterinario autenticado.");
	}

	@Transactional
	public void excluir(Long id, UsuarioPrincipal principal) {
		Consulta consulta = buscarEntidade(id);
		clinicalAccessService.exigirEscritaClinicaVeterinario(principal, consulta);
		if (!StatusConsulta.AG.getCodigo().equals(consulta.getStatus())) {
			throw new BusinessException("Somente consultas agendadas podem ser excluidas.", HttpStatus.CONFLICT);
		}
		try {
			consultaRepository.delete(consulta);
			consultaRepository.flush();
		} catch (DataIntegrityViolationException ex) {
			throw new BusinessException("Consulta nao pode ser excluida porque esta em uso.", HttpStatus.CONFLICT);
		}
	}

	@Transactional(readOnly = true)
	public Consulta buscarEntidade(Long id) {
		return consultaRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Consulta nao encontrada."));
	}

	private Page<ConsultaResponse> listarParaVeterinario(
			Long animalId,
			Long veterinarioId,
			Long clinicaId,
			String status,
			String modalidade,
			Pageable pageable,
			Long veterinarioAutenticadoId
	) {
		if (veterinarioAutenticadoId == null || (veterinarioId != null && !veterinarioId.equals(veterinarioAutenticadoId))) {
			return Page.empty(pageable);
		}
		return consultaRepository.buscar(animalId, veterinarioAutenticadoId, clinicaId, vazioParaNulo(status), vazioParaNulo(modalidade), pageable)
				.map(ConsultaResponse::fromEntity);
	}

	private Page<ConsultaResponse> listarParaClinica(
			Long animalId,
			Long veterinarioId,
			Long clinicaId,
			String status,
			String modalidade,
			Pageable pageable,
			Long clinicaAutenticadaId
	) {
		if (clinicaAutenticadaId == null || (clinicaId != null && !clinicaId.equals(clinicaAutenticadaId))) {
			return Page.empty(pageable);
		}
		return consultaRepository.buscar(animalId, veterinarioId, clinicaAutenticadaId, vazioParaNulo(status), vazioParaNulo(modalidade), pageable)
				.map(ConsultaResponse::fromEntity);
	}

	private void aplicarDados(Consulta consulta, ConsultaRequest request, boolean criando) {
		String status = criando && request.status() == null ? StatusConsulta.AG.getCodigo() : request.status();
		validarModalidadeObrigatoria(request.modalidade());
		validarStatusCriacaoOuAtualizacao(consulta, status, criando);
		if (request.dataHora() == null) {
			throw new BusinessException("Data e hora da consulta devem ser informadas.");
		}
		if ((criando || !Objects.equals(consulta.getDataHora(), request.dataHora()))
				&& request.dataHora().truncatedTo(ChronoUnit.MINUTES)
						.isBefore(LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES))) {
			throw new BusinessException("Data e hora da consulta nao podem estar no passado.");
		}
		Animal animal = buscarAnimal(request.animalId());
		Veterinario veterinario = buscarVeterinario(request.veterinarioId());
		Clinica clinica = request.clinicaId() == null ? null : buscarClinica(request.clinicaId());
		String endereco = vazioParaNulo(request.endereco());
		if (criando && endereco == null && "PRESENCIAL".equals(request.modalidade()) && veterinario.getClinica() != null) {
			endereco = vazioParaNulo(veterinario.getClinica().getEndereco());
		}
		if (endereco != null && endereco.length() > 255) {
			throw new BusinessException("Endereco da consulta deve ter no maximo 255 caracteres.");
		}
		consulta.setEndereco(endereco);
		consulta.setDataHora(request.dataHora());
		consulta.setModalidade(request.modalidade());
		consulta.setMotivo(request.motivo());
		consulta.setSintomas(request.sintomas());
		consulta.setObservacao(request.observacao());
		consulta.setPeso(request.peso());
		consulta.setTranscricao(request.transcricao());
		consulta.setStatus(status == null ? consulta.getStatus() : status);
		consulta.setAnimal(animal);
		consulta.setVeterinario(veterinario);
		consulta.setClinica(clinica);
	}

	private ConsultaRequest comVeterinarioAutenticado(ConsultaRequest request, Long veterinarioId) {
		return new ConsultaRequest(
				request.dataHora(),
				request.modalidade(),
				request.motivo(),
				request.sintomas(),
				request.observacao(),
				request.peso(),
				request.transcricao(),
				request.status(),
				request.animalId(),
				veterinarioId,
				request.clinicaId(),
				request.endereco()
		);
	}

	private ConsultaRequest comVeterinarioAtualQuandoOmitido(Consulta consulta, ConsultaRequest request) {
		if (request.veterinarioId() != null) {
			return request;
		}
		Long veterinarioAtualId = consulta.getVeterinario() == null ? null : consulta.getVeterinario().getId();
		return comVeterinarioAutenticado(request, veterinarioAtualId);
	}

	private void validarClinicaDaConsultaParaVeterinario(Consulta consulta) {
		Long clinicaConsultaId = consulta.getClinica() == null ? null : consulta.getClinica().getId();
		Long clinicaVeterinarioId = consulta.getVeterinario().getClinica() == null ? null : consulta.getVeterinario().getClinica().getId();
		if (clinicaConsultaId != null && !Objects.equals(clinicaConsultaId, clinicaVeterinarioId)) {
			throw new AccessDeniedException("Veterinario nao pode criar consulta em outra clinica.");
		}
	}

	private Animal buscarAnimal(Long id) {
		return animalRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Animal nao encontrado."));
	}

	private Veterinario buscarVeterinario(Long id) {
		return veterinarioRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Veterinario nao encontrado."));
	}

	private Clinica buscarClinica(Long id) {
		return clinicaRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Clinica nao encontrada."));
	}

	private void validarModalidadeObrigatoria(String modalidade) {
		if (!MODALIDADES.contains(modalidade)) {
			throw new BusinessException("Modalidade deve ser PRESENCIAL ou REMOTA.");
		}
	}

	private void validarModalidadeQuandoInformada(String modalidade) {
		if (modalidade != null && !modalidade.isBlank() && !MODALIDADES.contains(modalidade)) {
			throw new BusinessException("Modalidade deve ser PRESENCIAL ou REMOTA.");
		}
	}

	private void validarStatusQuandoInformado(String status) {
		StatusConsulta.validarQuandoInformado(status);
	}

	private void validarStatusCriacaoOuAtualizacao(Consulta consulta, String status, boolean criando) {
		validarStatusQuandoInformado(status);
		if (criando) {
			if (status != null && !status.isBlank() && !StatusConsulta.AG.getCodigo().equals(status)) {
				throw new BusinessException("Novas consultas devem iniciar com status AG.");
			}
			return;
		}
		if (status != null && !status.isBlank() && !status.equals(consulta.getStatus())) {
			throw new BusinessException("O status da consulta deve ser alterado pelas operacoes do fluxo clinico.");
		}
	}

	private void exigirVeterinarioAutenticado(UsuarioPrincipal principal) {
		if (principal == null || !TipoUsuario.VETERINARIO.equals(principal.getTipoUsuario()) || principal.getVeterinarioId() == null) {
			throw new AccessDeniedException("Operacao permitida apenas ao veterinario autenticado.");
		}
	}

	private void exigirAssociacoesImutaveis(Consulta consulta, ConsultaRequest request) {
		Long animalAtualId = consulta.getAnimal() == null ? null : consulta.getAnimal().getId();
		Long veterinarioAtualId = consulta.getVeterinario() == null ? null : consulta.getVeterinario().getId();
		Long clinicaAtualId = consulta.getClinica() == null ? null : consulta.getClinica().getId();
		if (!Objects.equals(animalAtualId, request.animalId())
				|| !Objects.equals(veterinarioAtualId, request.veterinarioId())
				|| !Objects.equals(clinicaAtualId, request.clinicaId())) {
			throw new BusinessException("Animal, veterinario e clinica da consulta nao podem ser alterados pelo PUT generico.", HttpStatus.CONFLICT);
		}
	}

	private String vazioParaNulo(String valor) {
		return valor == null || valor.isBlank() ? null : valor;
	}

}
