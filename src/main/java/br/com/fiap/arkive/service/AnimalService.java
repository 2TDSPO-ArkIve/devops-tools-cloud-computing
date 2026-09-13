package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.AnimalRequest;
import br.com.fiap.arkive.dto.response.AnimalResponse;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.Especie;
import br.com.fiap.arkive.entity.Raca;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.AnimalRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Clock;
import java.util.Objects;

@Service
@Profile("!local-nodb")
public class AnimalService {

	private final AnimalRepository animalRepository;
	private final EspecieService especieService;
	private final RacaService racaService;
	private final ClinicaService clinicaService;
	private final EventoJornadaService eventoJornadaService;
	private final ClinicalAccessService clinicalAccessService;
	private final VeterinarioService veterinarioService;
	private final Clock clock;

	@Autowired
	public AnimalService(
			AnimalRepository animalRepository,
			EspecieService especieService,
			RacaService racaService,
			ClinicaService clinicaService,
			EventoJornadaService eventoJornadaService,
			ClinicalAccessService clinicalAccessService,
			VeterinarioService veterinarioService
	) {
		this(animalRepository, especieService, racaService, clinicaService, eventoJornadaService,
				clinicalAccessService, veterinarioService, Clock.systemDefaultZone());
	}

	AnimalService(AnimalRepository animalRepository, EspecieService especieService, RacaService racaService,
			ClinicaService clinicaService, EventoJornadaService eventoJornadaService,
			ClinicalAccessService clinicalAccessService, VeterinarioService veterinarioService, Clock clock) {
		this.clock = clock;
		this.animalRepository = animalRepository;
		this.especieService = especieService;
		this.racaService = racaService;
		this.clinicaService = clinicaService;
		this.eventoJornadaService = eventoJornadaService;
		this.clinicalAccessService = clinicalAccessService;
		this.veterinarioService = veterinarioService;
	}

	@Transactional
	public AnimalResponse criar(AnimalRequest request) {
		throw new AccessDeniedException("Criacao de animal exige perfil administrativo.");
	}

	@Transactional
	public AnimalResponse criar(AnimalRequest request, UsuarioPrincipal principal) {
		AnimalRequest requestAutorizado = requestAutorizadoParaCriacao(request, principal);
		Animal animal = new Animal();
		aplicarDados(animal, requestAutorizado, true);
		if (TipoUsuario.VETERINARIO.equals(principal.getTipoUsuario())) {
			animal.setVeterinarioCadastro(veterinarioService.buscarEntidadeAtiva(principal.getVeterinarioId()));
		}
		Animal salvo = animalRepository.save(animal);
		Long clinicaId = salvo.getClinica() == null ? null : salvo.getClinica().getId();
		Long veterinarioId = TipoUsuario.VETERINARIO.equals(principal.getTipoUsuario()) ? principal.getVeterinarioId() : null;
		eventoJornadaService.registrarEvento(
				"ANIMAL_CADASTRADO",
				veterinarioId == null ? "SISTEMA" : "VETERINARIO",
				null,
				veterinarioId,
				salvo.getId(),
				clinicaId,
				"Animal cadastrado.",
				eventoJornadaService.criarPayload("Animal", salvo.getId(), "ANIMAL_CADASTRADO")
		);
		return AnimalResponse.fromEntity(salvo);
	}

	@Transactional(readOnly = true)
	public Page<AnimalResponse> listar(
			String nome,
			Long especieId,
			Long racaId,
			Long clinicaId,
			String ativo,
			Pageable pageable
	) {
		validarAtivoQuandoInformado(ativo);
		return animalRepository.buscar(
				vazioParaNulo(nome),
				especieId,
				racaId,
				clinicaId,
				vazioParaNulo(ativo),
				pageable
		).map(AnimalResponse::fromEntity);
	}

	@Transactional(readOnly = true)
	public Page<AnimalResponse> listarPacientesClinicaVeterinario(
			String nome,
			Long especieId,
			Long racaId,
			Pageable pageable,
			UsuarioPrincipal principal
	) {
		exigirVeterinarioAutenticado(principal);
		Long clinicaId = clinicaVeterinarioAutenticadoOpcional(principal);
		return animalRepository.buscarParaVeterinario(
				principal.getVeterinarioId(),
				clinicaId,
				vazioParaNulo(nome),
				especieId,
				racaId,
				null,
				"S",
				pageable
		).map(AnimalResponse::fromEntity);
	}

	@Transactional(readOnly = true)
	public Page<AnimalResponse> listarPacientesVeterinario(
			String nome,
			Long especieId,
			Long racaId,
			Pageable pageable,
			UsuarioPrincipal principal
	) {
		exigirVeterinarioAutenticado(principal);
		return animalRepository.buscarParaVeterinario(
				principal.getVeterinarioId(),
				clinicaVeterinarioAutenticadoOpcional(principal),
				vazioParaNulo(nome),
				especieId,
				racaId,
				null,
				"S",
				pageable
		).map(AnimalResponse::fromEntity);
	}

	@Transactional(readOnly = true)
	public Page<AnimalResponse> listarAutorizado(
			String nome,
			Long especieId,
			Long racaId,
			Long clinicaId,
			String ativo,
			Pageable pageable,
			UsuarioPrincipal principal
	) {
		if (principal == null) {
			throw new AccessDeniedException("Usuario autenticado invalido.");
		}
		validarAtivoQuandoInformado(ativo);
		return switch (principal.getTipoUsuario()) {
			case SYSADMIN -> animalRepository.buscar(vazioParaNulo(nome), especieId, racaId, clinicaId, vazioParaNulo(ativo), pageable)
					.map(AnimalResponse::fromEntity);
			case RESPONSAVEL -> animalRepository.buscarParaResponsavel(
					principal.getResponsavelId(),
					LocalDate.now(),
					vazioParaNulo(nome),
					especieId,
					racaId,
					clinicaId,
					vazioParaNulo(ativo),
					pageable
			).map(AnimalResponse::fromEntity);
			case VETERINARIO -> listarParaVeterinario(nome, especieId, racaId, clinicaId, ativo, pageable, principal);
			case ADMIN_CLINICA -> listarParaClinica(nome, especieId, racaId, clinicaId, ativo, pageable, principal.getClinicaId());
		};
	}

	@Transactional(readOnly = true)
	public AnimalResponse buscarPorId(Long id) {
		return AnimalResponse.fromEntity(buscarEntidade(id));
	}

	@Transactional(readOnly = true)
	public AnimalResponse buscarPorIdAutorizado(Long id, UsuarioPrincipal principal) {
		Animal animal = buscarEntidade(id);
		clinicalAccessService.exigirLeituraAnimal(principal, animal);
		return AnimalResponse.fromEntity(animal);
	}

	@Transactional
	public AnimalResponse atualizar(Long id, AnimalRequest request) {
		throw new AccessDeniedException("Atualizacao de animal exige perfil administrativo.");
	}

	@Transactional
	public AnimalResponse atualizar(Long id, AnimalRequest request, UsuarioPrincipal principal) {
		Animal animal = buscarEntidade(id);
		AnimalRequest requestAutorizado = requestAutorizadoParaAtualizacao(animal, request, principal);
		aplicarDados(animal, requestAutorizado, false);
		return AnimalResponse.fromEntity(animalRepository.save(animal));
	}

	@Transactional
	public void excluir(Long id) {
		throw new AccessDeniedException("Exclusao de animal exige perfil administrativo.");
	}

	@Transactional
	public void excluir(Long id, UsuarioPrincipal principal) {
		Animal animal = buscarEntidade(id);
		exigirPermissaoAdministrativaAnimal(principal, animal);
		animal.setAtivo("N");
		animalRepository.save(animal);
	}

	private Animal buscarEntidade(Long id) {
		return animalRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Animal nao encontrado."));
	}

	private Page<AnimalResponse> listarParaClinica(
			String nome,
			Long especieId,
			Long racaId,
			Long clinicaId,
			String ativo,
			Pageable pageable,
			Long clinicaAutenticadaId
	) {
		if (clinicaAutenticadaId == null || (clinicaId != null && !clinicaId.equals(clinicaAutenticadaId))) {
			return Page.empty(pageable);
		}
		return animalRepository.buscar(vazioParaNulo(nome), especieId, racaId, clinicaAutenticadaId, vazioParaNulo(ativo), pageable)
				.map(AnimalResponse::fromEntity);
	}

	private AnimalRequest requestAutorizadoParaCriacao(AnimalRequest request, UsuarioPrincipal principal) {
		exigirPrincipal(principal);
		return switch (principal.getTipoUsuario()) {
			case SYSADMIN -> request;
			case ADMIN_CLINICA -> {
				Long clinicaId = exigirClinicaAutenticada(principal);
				if (request.clinicaId() != null && !Objects.equals(request.clinicaId(), clinicaId)) {
					throw new AccessDeniedException("Admin da clinica nao pode criar animal em outra clinica.");
				}
				yield comClinica(request, clinicaId);
			}
			case VETERINARIO -> {
				Long clinicaId = clinicaVeterinarioAutenticadoOpcional(principal);
				if (request.clinicaId() != null && !Objects.equals(request.clinicaId(), clinicaId)) {
					throw new AccessDeniedException("Veterinario nao pode criar animal em outra clinica.");
				}
				if (request.ativo() != null && !"S".equals(request.ativo())) {
					throw new BusinessException("Veterinario so pode cadastrar paciente ativo.");
				}
				yield comClinicaEAtivo(request, clinicaId, "S");
			}
			case RESPONSAVEL -> throw new AccessDeniedException("Operacao permitida apenas a SYSADMIN, ADMIN_CLINICA ou VETERINARIO.");
		};
	}

	private AnimalRequest requestAutorizadoParaAtualizacao(Animal animal, AnimalRequest request, UsuarioPrincipal principal) {
		exigirPrincipal(principal);
		return switch (principal.getTipoUsuario()) {
			case SYSADMIN -> request;
			case ADMIN_CLINICA -> {
				Long clinicaId = exigirClinicaAutenticada(principal);
				Long clinicaAtualId = animal.getClinica() == null ? null : animal.getClinica().getId();
				if (!Objects.equals(clinicaAtualId, clinicaId)) {
					throw new AccessDeniedException("Admin da clinica nao autorizado para este animal.");
				}
				if (request.clinicaId() != null && !Objects.equals(request.clinicaId(), clinicaId)) {
					throw new BusinessException("Admin da clinica nao pode mover animal para outra clinica.", org.springframework.http.HttpStatus.CONFLICT);
				}
				yield comClinica(request, clinicaId);
			}
			case VETERINARIO -> {
				Long clinicaAtualId = animal.getClinica() == null ? null : animal.getClinica().getId();
				clinicalAccessService.exigirLeituraAnimal(principal, animal);
				if (request.clinicaId() != null && !Objects.equals(request.clinicaId(), clinicaAtualId)) {
					throw new BusinessException("Veterinario nao pode mover animal para outra clinica.", org.springframework.http.HttpStatus.CONFLICT);
				}
				if (request.ativo() != null && !Objects.equals(request.ativo(), animal.getAtivo())) {
					throw new BusinessException("Veterinario nao pode alterar status do animal.", org.springframework.http.HttpStatus.CONFLICT);
				}
				yield comClinicaEAtivo(request, clinicaAtualId, animal.getAtivo());
			}
			case RESPONSAVEL -> throw new AccessDeniedException("Operacao permitida apenas a SYSADMIN, ADMIN_CLINICA ou VETERINARIO.");
		};
	}

	private void exigirPermissaoAdministrativaAnimal(UsuarioPrincipal principal, Animal animal) {
		exigirPrincipal(principal);
		switch (principal.getTipoUsuario()) {
			case SYSADMIN -> {
			}
			case ADMIN_CLINICA -> {
				Long clinicaId = exigirClinicaAutenticada(principal);
				Long clinicaAnimalId = animal.getClinica() == null ? null : animal.getClinica().getId();
				if (!Objects.equals(clinicaAnimalId, clinicaId)) {
					throw new AccessDeniedException("Admin da clinica nao autorizado para este animal.");
				}
			}
			case VETERINARIO, RESPONSAVEL -> throw new AccessDeniedException("Operacao permitida apenas a SYSADMIN ou ADMIN_CLINICA.");
		}
	}

	private Long exigirClinicaAutenticada(UsuarioPrincipal principal) {
		if (principal.getClinicaId() == null) {
			throw new AccessDeniedException("Admin da clinica sem clinica vinculada.");
		}
		return principal.getClinicaId();
	}

	private Page<AnimalResponse> listarParaVeterinario(
			String nome,
			Long especieId,
			Long racaId,
			Long clinicaId,
			String ativo,
			Pageable pageable,
			UsuarioPrincipal principal
	) {
		if (principal.getVeterinarioId() == null) {
			return Page.empty(pageable);
		}
		return animalRepository.buscarParaVeterinario(
				principal.getVeterinarioId(),
				clinicaVeterinarioAutenticadoOpcional(principal),
				vazioParaNulo(nome),
				especieId,
				racaId,
				clinicaId,
				vazioParaNulo(ativo),
				pageable
		).map(AnimalResponse::fromEntity);
	}

	private void exigirVeterinarioAutenticado(UsuarioPrincipal principal) {
		if (principal == null || !TipoUsuario.VETERINARIO.equals(principal.getTipoUsuario()) || principal.getVeterinarioId() == null) {
			throw new AccessDeniedException("Operacao permitida apenas ao veterinario autenticado.");
		}
	}

	private Long clinicaVeterinarioAutenticadoOpcional(UsuarioPrincipal principal) {
		exigirVeterinarioAutenticado(principal);
		return veterinarioService.buscarClinicaId(principal.getVeterinarioId());
	}

	private void exigirPrincipal(UsuarioPrincipal principal) {
		if (principal == null) {
			throw new AccessDeniedException("Usuario autenticado invalido.");
		}
	}

	private AnimalRequest comClinica(AnimalRequest request, Long clinicaId) {
		return new AnimalRequest(
				request.nome(),
				request.especieId(),
				request.racaId(),
				request.sexo(),
				request.castrado(),
				clinicaId,
				request.ativo(),
				request.dataNascimento()
		);
	}

	private AnimalRequest comClinicaEAtivo(AnimalRequest request, Long clinicaId, String ativo) {
		return new AnimalRequest(
				request.nome(),
				request.especieId(),
				request.racaId(),
				request.sexo(),
				request.castrado(),
				clinicaId,
				ativo,
				request.dataNascimento()
		);
	}

	private void aplicarDados(Animal animal, AnimalRequest request, boolean criando) {
		if (request.dataNascimento() != null && request.dataNascimento().isAfter(LocalDate.now(clock))) {
			throw new BusinessException("Data de nascimento do animal nao pode estar no futuro.");
		}
		String ativo = criando && request.ativo() == null ? "S" : request.ativo();
		String castrado = criando && request.castrado() == null ? "N" : request.castrado();
		validarAtivoQuandoInformado(ativo);
		validarSexoQuandoInformado(request.sexo());
		validarSNQuandoInformado(castrado, "Castrado");
		Especie especie = especieService.buscarEntidade(request.especieId());
		Raca raca = request.racaId() == null ? null : racaService.buscarEntidade(request.racaId());
		if (raca != null && !raca.getEspecie().getId().equals(especie.getId())) {
			throw new BusinessException("Raca deve pertencer a mesma especie do animal.");
		}
		Clinica clinica = request.clinicaId() == null ? null : clinicaService.buscarEntidade(request.clinicaId());
		animal.setNome(request.nome());
		animal.setDataNascimento(request.dataNascimento());
		animal.setSexo(request.sexo());
		animal.setCastrado(castrado == null ? animal.getCastrado() : castrado);
		animal.setEspecie(especie);
		animal.setRaca(raca);
		animal.setClinica(clinica);
		animal.setAtivo(ativo == null ? animal.getAtivo() : ativo);
	}

	private void validarAtivoQuandoInformado(String valor) {
		if (valor != null && !valor.isBlank() && !"S".equals(valor) && !"N".equals(valor)) {
			throw new BusinessException("Ativo deve ser S ou N.");
		}
	}

	private void validarSexoQuandoInformado(String valor) {
		if (valor != null && !valor.isBlank() && !"M".equals(valor) && !"F".equals(valor)) {
			throw new BusinessException("Sexo deve ser M ou F.");
		}
	}

	private void validarSNQuandoInformado(String valor, String campo) {
		if (valor != null && !valor.isBlank() && !"S".equals(valor) && !"N".equals(valor)) {
			throw new BusinessException(campo + " deve ser S ou N.");
		}
	}

	private String vazioParaNulo(String valor) {
		return valor == null || valor.isBlank() ? null : valor;
	}

}
