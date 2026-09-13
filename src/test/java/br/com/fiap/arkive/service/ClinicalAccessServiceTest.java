package br.com.fiap.arkive.service;

import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.AdesaoPrescricao;
import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Diagnostico;
import br.com.fiap.arkive.entity.Prescricao;
import br.com.fiap.arkive.entity.Responsavel;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.repository.AnimalResponsavelRepository;
import br.com.fiap.arkive.repository.ConsultaRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClinicalAccessServiceTest {

	private AnimalResponsavelRepository animalResponsavelRepository;
	private ConsultaRepository consultaRepository;
	private VeterinarioService veterinarioService;
	private ClinicalAccessService clinicalAccessService;

	@BeforeEach
	void setUp() {
		animalResponsavelRepository = mock(AnimalResponsavelRepository.class);
		consultaRepository = mock(ConsultaRepository.class);
		veterinarioService = mock(VeterinarioService.class);
		clinicalAccessService = new ClinicalAccessService(animalResponsavelRepository, consultaRepository, veterinarioService);
	}

	@Test
	void veterinarioPodeEscreverNaPropriaConsulta() {
		assertDoesNotThrow(() -> clinicalAccessService.exigirEscritaClinicaVeterinario(veterinario(10L), consulta(10L, 30L)));
	}

	@Test
	void veterinarioNaoEscreveEmConsultaDeOutroVeterinario() {
		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirEscritaClinicaVeterinario(veterinario(10L), consulta(22L, 30L)));
	}

	@Test
	void adminClinicaSysadminEResponsavelNaoExecutamEscritaClinicaDeVeterinario() {
		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirEscritaClinicaVeterinario(adminClinica(30L), consulta(10L, 30L)));
		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirEscritaClinicaVeterinario(sysadmin(), consulta(10L, 30L)));
		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirEscritaClinicaVeterinario(responsavel(40L), consulta(10L, 30L)));
	}

	@Test
	void consultaPodeSerLidaPorVeterinarioResponsavelResponsavelVinculadoAdminDaClinicaESysadmin() {
		when(animalResponsavelRepository.existsVinculoAtivoVigente(eq(50L), eq(40L), any(LocalDate.class))).thenReturn(true);

		assertDoesNotThrow(() -> clinicalAccessService.exigirLeituraConsulta(veterinario(10L), consulta(10L, 30L)));
		assertDoesNotThrow(() -> clinicalAccessService.exigirLeituraConsulta(responsavel(40L), consulta(10L, 30L)));
		assertDoesNotThrow(() -> clinicalAccessService.exigirLeituraConsulta(adminClinica(30L), consulta(10L, 30L)));
		assertDoesNotThrow(() -> clinicalAccessService.exigirLeituraConsulta(sysadmin(), consulta(10L, 30L)));
	}

	@Test
	void consultaNaoPodeSerLidaForaDoEscopoDoUsuario() {
		when(animalResponsavelRepository.existsVinculoAtivoVigente(eq(50L), eq(41L), any(LocalDate.class))).thenReturn(false);

		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirLeituraConsulta(veterinario(22L), consulta(10L, 30L)));
		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirLeituraConsulta(responsavel(41L), consulta(10L, 30L)));
		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirLeituraConsulta(adminClinica(31L), consulta(10L, 30L)));
	}

	@Test
	void responsavelLeAnimalSomenteComVinculoAtivoVigente() {
		when(animalResponsavelRepository.existsVinculoAtivoVigente(eq(50L), eq(40L), any(LocalDate.class))).thenReturn(true);

		assertDoesNotThrow(() -> clinicalAccessService.exigirLeituraAnimal(responsavel(40L), animal(50L, 30L)));
		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirLeituraAnimal(responsavel(41L), animal(50L, 30L)));
	}

	@Test
	void veterinarioLeAnimalComConsultaPropria() {
		when(consultaRepository.existsConsultaDoVeterinarioParaAnimal(50L, 10L)).thenReturn(true);

		assertDoesNotThrow(() -> clinicalAccessService.exigirLeituraAnimal(veterinario(10L), animal(50L, 30L)));
		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirLeituraAnimal(veterinario(22L), animal(50L, 30L)));
	}

	@Test
	void veterinarioLeAnimalAtivoCadastradoPorEleSemClinicaOuConsulta() {
		Animal animal = animal(50L, null);
		animal.setVeterinarioCadastro(veterinarioEntidade(10L));
		when(consultaRepository.existsConsultaDoVeterinarioParaAnimal(50L, 10L)).thenReturn(false);
		when(veterinarioService.buscarClinicaId(10L)).thenReturn(null);

		assertDoesNotThrow(() -> clinicalAccessService.exigirLeituraAnimal(veterinario(10L), animal));
		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirLeituraAnimal(veterinario(22L), animal));
	}

	@Test
	void veterinarioNaoLeAnimalCliniclessNaoCadastradoPorEleSemConsulta() {
		Animal animal = animal(50L, null);
		animal.setVeterinarioCadastro(veterinarioEntidade(22L));
		when(consultaRepository.existsConsultaDoVeterinarioParaAnimal(50L, 10L)).thenReturn(false);
		when(veterinarioService.buscarClinicaId(10L)).thenReturn(null);

		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirLeituraAnimal(veterinario(10L), animal));
	}

	@Test
	void veterinarioLeAnimalAtivoDaPropriaClinicaSemConsultaPrevia() {
		when(consultaRepository.existsConsultaDoVeterinarioParaAnimal(50L, 10L)).thenReturn(false);
		when(veterinarioService.buscarClinicaId(10L)).thenReturn(30L);

		assertDoesNotThrow(() -> clinicalAccessService.exigirLeituraAnimal(veterinario(10L), animal(50L, 30L)));
	}

	@Test
	void veterinarioNaoLeAnimalDeOutraClinicaSemConsultaPrevia() {
		when(consultaRepository.existsConsultaDoVeterinarioParaAnimal(50L, 10L)).thenReturn(false);
		when(veterinarioService.buscarClinicaId(10L)).thenReturn(30L);

		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirLeituraAnimal(veterinario(10L), animal(50L, 31L)));
	}

	@Test
	void veterinarioNaoLeAnimalInativoDaPropriaClinicaSemConsultaPrevia() {
		when(consultaRepository.existsConsultaDoVeterinarioParaAnimal(50L, 10L)).thenReturn(false);
		when(veterinarioService.buscarClinicaId(10L)).thenReturn(30L);
		Animal animal = animal(50L, 30L);
		animal.setAtivo("N");

		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirLeituraAnimal(veterinario(10L), animal));
	}

	@Test
	void diagnosticoEPrescricaoUsamConsultaParaEscritaVeterinaria() {
		Diagnostico diagnostico = new Diagnostico();
		diagnostico.setConsulta(consulta(10L, 30L));
		Prescricao prescricao = new Prescricao();
		prescricao.setConsulta(consulta(10L, 30L));

		assertDoesNotThrow(() -> clinicalAccessService.exigirEscritaDiagnosticoVeterinario(veterinario(10L), diagnostico));
		assertDoesNotThrow(() -> clinicalAccessService.exigirEscritaPrescricaoVeterinario(veterinario(10L), prescricao));
		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirEscritaPrescricaoVeterinario(responsavel(40L), prescricao));
		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirEscritaPrescricaoVeterinario(adminClinica(30L), prescricao));
	}

	@Test
	void prescricaoUsaEscopoDaConsultaParaLeitura() {
		when(animalResponsavelRepository.existsVinculoAtivoVigente(eq(50L), eq(40L), any(LocalDate.class))).thenReturn(true);
		Prescricao prescricao = prescricao(consulta(10L, 30L));

		assertDoesNotThrow(() -> clinicalAccessService.exigirLeituraPrescricao(veterinario(10L), prescricao));
		assertDoesNotThrow(() -> clinicalAccessService.exigirLeituraPrescricao(responsavel(40L), prescricao));
		assertDoesNotThrow(() -> clinicalAccessService.exigirLeituraPrescricao(adminClinica(30L), prescricao));
		assertDoesNotThrow(() -> clinicalAccessService.exigirLeituraPrescricao(sysadmin(), prescricao));
		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirLeituraPrescricao(veterinario(22L), prescricao));
		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirLeituraPrescricao(responsavel(41L), prescricao));
		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirLeituraPrescricao(adminClinica(31L), prescricao));
	}

	@Test
	void registroDeAdesaoPermitidoSomenteAoResponsavelVinculado() {
		when(animalResponsavelRepository.existsVinculoAtivoVigente(eq(50L), eq(40L), any(LocalDate.class))).thenReturn(true);
		Prescricao prescricao = prescricao(consulta(10L, 30L));

		assertDoesNotThrow(() -> clinicalAccessService.exigirRegistroAdesaoResponsavel(responsavel(40L), prescricao));
		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirRegistroAdesaoResponsavel(responsavel(41L), prescricao));
		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirRegistroAdesaoResponsavel(veterinario(10L), prescricao));
		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirRegistroAdesaoResponsavel(adminClinica(30L), prescricao));
		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirRegistroAdesaoResponsavel(sysadmin(), prescricao));
	}

	@Test
	void leituraDeAdesaoPreservaPrivacidadeEntreResponsaveis() {
		when(animalResponsavelRepository.existsVinculoAtivoVigente(eq(50L), eq(40L), any(LocalDate.class))).thenReturn(true);
		when(animalResponsavelRepository.existsVinculoAtivoVigente(eq(50L), eq(41L), any(LocalDate.class))).thenReturn(true);
		AdesaoPrescricao adesao = adesao(prescricao(consulta(10L, 30L)), 40L);

		assertDoesNotThrow(() -> clinicalAccessService.exigirLeituraAdesaoPrescricao(veterinario(10L), adesao));
		assertDoesNotThrow(() -> clinicalAccessService.exigirLeituraAdesaoPrescricao(responsavel(40L), adesao));
		assertDoesNotThrow(() -> clinicalAccessService.exigirLeituraAdesaoPrescricao(adminClinica(30L), adesao));
		assertDoesNotThrow(() -> clinicalAccessService.exigirLeituraAdesaoPrescricao(sysadmin(), adesao));
		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirLeituraAdesaoPrescricao(veterinario(22L), adesao));
		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirLeituraAdesaoPrescricao(responsavel(41L), adesao));
		assertThrows(AccessDeniedException.class, () -> clinicalAccessService.exigirLeituraAdesaoPrescricao(adminClinica(31L), adesao));
	}

	private Consulta consulta(Long veterinarioId, Long clinicaId) {
		Consulta consulta = new Consulta();
		consulta.setId(1L);
		consulta.setAnimal(animal(50L, clinicaId));
		Veterinario veterinario = new Veterinario();
		veterinario.setId(veterinarioId);
		consulta.setVeterinario(veterinario);
		if (clinicaId != null) {
			Clinica clinica = new Clinica();
			clinica.setId(clinicaId);
			consulta.setClinica(clinica);
		}
		return consulta;
	}

	private Animal animal(Long animalId, Long clinicaId) {
		Animal animal = new Animal();
		animal.setId(animalId);
		animal.setAtivo("S");
		if (clinicaId != null) {
			Clinica clinica = new Clinica();
			clinica.setId(clinicaId);
			animal.setClinica(clinica);
		}
		return animal;
	}

	private Prescricao prescricao(Consulta consulta) {
		Prescricao prescricao = new Prescricao();
		prescricao.setId(9L);
		prescricao.setConsulta(consulta);
		return prescricao;
	}

	private AdesaoPrescricao adesao(Prescricao prescricao, Long responsavelId) {
		Responsavel responsavel = new Responsavel();
		responsavel.setId(responsavelId);
		AdesaoPrescricao adesao = new AdesaoPrescricao();
		adesao.setId(80L);
		adesao.setPrescricao(prescricao);
		adesao.setAnimal(prescricao.getConsulta().getAnimal());
		adesao.setResponsavel(responsavel);
		return adesao;
	}

	private UsuarioPrincipal veterinario(Long veterinarioId) {
		return new UsuarioPrincipal(1L, "Dra", "vet@arkive.com", "$2a$10$hash", TipoUsuario.VETERINARIO, "S", false, null, veterinarioId, null);
	}

	private Veterinario veterinarioEntidade(Long veterinarioId) {
		Veterinario veterinario = new Veterinario();
		veterinario.setId(veterinarioId);
		return veterinario;
	}

	private UsuarioPrincipal responsavel(Long responsavelId) {
		return new UsuarioPrincipal(2L, "Tutor", "tutor@arkive.com", "$2a$10$hash", TipoUsuario.RESPONSAVEL, "S", false, responsavelId, null, null);
	}

	private UsuarioPrincipal adminClinica(Long clinicaId) {
		return new UsuarioPrincipal(3L, "Admin", "admin@arkive.com", "$2a$10$hash", TipoUsuario.ADMIN_CLINICA, "S", false, null, null, clinicaId);
	}

	private UsuarioPrincipal sysadmin() {
		return new UsuarioPrincipal(4L, "Sys", "sys@arkive.com", "$2a$10$hash", TipoUsuario.SYSADMIN, "S", false, null, null, null);
	}

}
