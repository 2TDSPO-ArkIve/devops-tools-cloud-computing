package br.com.fiap.arkive.service;

import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.AdesaoPrescricao;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Diagnostico;
import br.com.fiap.arkive.entity.Prescricao;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.repository.AnimalResponsavelRepository;
import br.com.fiap.arkive.repository.ConsultaRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Objects;

@Service
@Profile("!local-nodb")
public class ClinicalAccessService {

	private final AnimalResponsavelRepository animalResponsavelRepository;
	private final ConsultaRepository consultaRepository;
	private final VeterinarioService veterinarioService;

	public ClinicalAccessService(
			AnimalResponsavelRepository animalResponsavelRepository,
			ConsultaRepository consultaRepository,
			VeterinarioService veterinarioService
	) {
		this.animalResponsavelRepository = animalResponsavelRepository;
		this.consultaRepository = consultaRepository;
		this.veterinarioService = veterinarioService;
	}

	public void exigirEscritaClinicaVeterinario(UsuarioPrincipal principal, Consulta consulta) {
		exigirPrincipal(principal);
		if (!TipoUsuario.VETERINARIO.equals(principal.getTipoUsuario())) {
			throw new AccessDeniedException("Operacao clinica permitida apenas ao veterinario responsavel.");
		}
		if (principal.getVeterinarioId() == null || consulta.getVeterinario() == null
				|| !Objects.equals(principal.getVeterinarioId(), consulta.getVeterinario().getId())) {
			throw new AccessDeniedException("Veterinario nao autorizado para esta consulta.");
		}
	}

	public void exigirLeituraConsulta(UsuarioPrincipal principal, Consulta consulta) {
		exigirPrincipal(principal);
		if (podeLerConsulta(principal, consulta)) {
			return;
		}
		throw new AccessDeniedException("Usuario nao autorizado para esta consulta.");
	}

	public void exigirLeituraAnimal(UsuarioPrincipal principal, Animal animal) {
		exigirPrincipal(principal);
		if (podeLerAnimal(principal, animal)) {
			return;
		}
		throw new AccessDeniedException("Usuario nao autorizado para este animal.");
	}

	public void exigirGestaoVinculosAnimal(UsuarioPrincipal principal, Animal animal) {
		exigirPrincipal(principal);
		if (TipoUsuario.RESPONSAVEL.equals(principal.getTipoUsuario())) {
			throw new AccessDeniedException("Gestao de tutores permitida apenas a veterinarios e administradores autorizados.");
		}
		exigirLeituraAnimal(principal, animal);
	}

	public void exigirEscritaDiagnosticoVeterinario(UsuarioPrincipal principal, Diagnostico diagnostico) {
		exigirEscritaClinicaVeterinario(principal, diagnostico.getConsulta());
	}

	public void exigirLeituraDiagnostico(UsuarioPrincipal principal, Diagnostico diagnostico) {
		exigirLeituraConsulta(principal, diagnostico.getConsulta());
	}

	public void exigirEscritaPrescricaoVeterinario(UsuarioPrincipal principal, Prescricao prescricao) {
		exigirEscritaClinicaVeterinario(principal, prescricao.getConsulta());
	}

	public void exigirLeituraPrescricao(UsuarioPrincipal principal, Prescricao prescricao) {
		exigirLeituraConsulta(principal, prescricao.getConsulta());
	}

	public void exigirRegistroAdesaoResponsavel(UsuarioPrincipal principal, Prescricao prescricao) {
		exigirPrincipal(principal);
		if (!TipoUsuario.RESPONSAVEL.equals(principal.getTipoUsuario()) || principal.getResponsavelId() == null) {
			throw new AccessDeniedException("Registro de adesao permitido apenas ao responsavel vinculado ao animal.");
		}
		if (prescricao.getConsulta() == null || prescricao.getConsulta().getAnimal() == null
				|| !temVinculoResponsavelAnimal(principal.getResponsavelId(), prescricao.getConsulta().getAnimal().getId())) {
			throw new AccessDeniedException("Responsavel nao autorizado para registrar adesao desta prescricao.");
		}
	}

	public void exigirLeituraAdesaoPrescricao(UsuarioPrincipal principal, AdesaoPrescricao adesao) {
		exigirPrincipal(principal);
		if (podeLerAdesaoPrescricao(principal, adesao)) {
			return;
		}
		throw new AccessDeniedException("Usuario nao autorizado para esta adesao de prescricao.");
	}

	public boolean podeLerConsulta(UsuarioPrincipal principal, Consulta consulta) {
		exigirPrincipal(principal);
		return switch (principal.getTipoUsuario()) {
			case SYSADMIN -> true;
			case VETERINARIO -> principal.getVeterinarioId() != null
					&& consulta.getVeterinario() != null
					&& Objects.equals(principal.getVeterinarioId(), consulta.getVeterinario().getId());
			case RESPONSAVEL -> principal.getResponsavelId() != null
					&& consulta.getAnimal() != null
					&& temVinculoResponsavelAnimal(principal.getResponsavelId(), consulta.getAnimal().getId());
			case ADMIN_CLINICA -> principal.getClinicaId() != null
					&& consulta.getClinica() != null
					&& Objects.equals(principal.getClinicaId(), consulta.getClinica().getId());
		};
	}

	private boolean podeLerAnimal(UsuarioPrincipal principal, Animal animal) {
		return switch (principal.getTipoUsuario()) {
			case SYSADMIN -> true;
			case RESPONSAVEL -> principal.getResponsavelId() != null
					&& temVinculoResponsavelAnimal(principal.getResponsavelId(), animal.getId());
			case VETERINARIO -> principal.getVeterinarioId() != null
					&& "S".equals(animal.getAtivo())
					&& (animalCadastradoPeloVeterinario(animal, principal.getVeterinarioId())
					|| consultaRepository.existsConsultaDoVeterinarioParaAnimal(animal.getId(), principal.getVeterinarioId())
					|| animalAtivoDaClinicaDoVeterinario(animal, principal.getVeterinarioId()));
			case ADMIN_CLINICA -> principal.getClinicaId() != null
					&& animal.getClinica() != null
					&& Objects.equals(principal.getClinicaId(), animal.getClinica().getId());
		};
	}

	private boolean animalCadastradoPeloVeterinario(Animal animal, Long veterinarioId) {
		return animal.getVeterinarioCadastro() != null
				&& Objects.equals(veterinarioId, animal.getVeterinarioCadastro().getId());
	}

	private boolean animalAtivoDaClinicaDoVeterinario(Animal animal, Long veterinarioId) {
		Long clinicaId = veterinarioService.buscarClinicaId(veterinarioId);
		return clinicaId != null
				&& "S".equals(animal.getAtivo())
				&& animal.getClinica() != null
				&& Objects.equals(clinicaId, animal.getClinica().getId());
	}

	private boolean podeLerAdesaoPrescricao(UsuarioPrincipal principal, AdesaoPrescricao adesao) {
		Consulta consulta = adesao.getPrescricao() == null ? null : adesao.getPrescricao().getConsulta();
		return switch (principal.getTipoUsuario()) {
			case SYSADMIN -> true;
			case VETERINARIO -> principal.getVeterinarioId() != null
					&& consulta != null
					&& consulta.getVeterinario() != null
					&& Objects.equals(principal.getVeterinarioId(), consulta.getVeterinario().getId());
			case RESPONSAVEL -> principal.getResponsavelId() != null
					&& adesao.getResponsavel() != null
					&& Objects.equals(principal.getResponsavelId(), adesao.getResponsavel().getId())
					&& consulta != null
					&& consulta.getAnimal() != null
					&& temVinculoResponsavelAnimal(principal.getResponsavelId(), consulta.getAnimal().getId());
			case ADMIN_CLINICA -> principal.getClinicaId() != null
					&& consulta != null
					&& consulta.getClinica() != null
					&& Objects.equals(principal.getClinicaId(), consulta.getClinica().getId());
		};
	}

	private boolean temVinculoResponsavelAnimal(Long responsavelId, Long animalId) {
		return animalId != null && animalResponsavelRepository.existsVinculoAtivoVigente(animalId, responsavelId, LocalDate.now());
	}

	private void exigirPrincipal(UsuarioPrincipal principal) {
		if (principal == null) {
			throw new AccessDeniedException("Usuario autenticado invalido.");
		}
	}

}
