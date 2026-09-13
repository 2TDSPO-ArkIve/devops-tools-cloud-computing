package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.ClinicaRequest;
import br.com.fiap.arkive.dto.request.UsuarioProvisioningRequest;
import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.repository.ClinicaRepository;
import br.com.fiap.arkive.repository.ResponsavelRepository;
import br.com.fiap.arkive.repository.UsuarioRepository;
import br.com.fiap.arkive.repository.VeterinarioRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClinicaServiceTest {

	@Test
	void criarClinicaProvisionaUsuarioAdminClinicaComVinculo() {
		ClinicaRepository clinicaRepository = mock(ClinicaRepository.class);
		AccountProvisioningService provisioningService = mock(AccountProvisioningService.class);
		ClinicaService service = new ClinicaService(clinicaRepository, provisioningService);
		when(clinicaRepository.save(any(Clinica.class))).thenAnswer(invocation -> {
			Clinica clinica = invocation.getArgument(0);
			clinica.setId(20L);
			return clinica;
		});
		ArgumentCaptor<UsuarioProvisioningRequest> captor = ArgumentCaptor.forClass(UsuarioProvisioningRequest.class);

		service.criar(new ClinicaRequest("Clinica Central", "12345678000199", "Rua A", "11999999999", "contato@clinica.com", null));

		verify(provisioningService).provisionar(captor.capture());
		assertEquals("Clinica Central", captor.getValue().nome());
		assertEquals(TipoUsuario.ADMIN_CLINICA, captor.getValue().tipo());
		assertEquals("contato@clinica.com", captor.getValue().login());
		assertEquals(20L, captor.getValue().clinicaId());
		assertNull(captor.getValue().responsavelId());
		assertNull(captor.getValue().veterinarioId());
	}

	@Test
	void criarClinicaComProvisionamentoRealPersisteUsuarioComBcryptETrocaObrigatoria() {
		ClinicaRepository clinicaRepository = mock(ClinicaRepository.class);
		UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
		ResponsavelRepository responsavelRepository = mock(ResponsavelRepository.class);
		VeterinarioRepository veterinarioRepository = mock(VeterinarioRepository.class);
		BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
		UsuarioService usuarioService = new UsuarioService(
				usuarioRepository,
				responsavelRepository,
				veterinarioRepository,
				clinicaRepository,
				passwordEncoder
		);
		AccountProvisioningService provisioningService = new AccountProvisioningService(usuarioService);
		ClinicaService service = new ClinicaService(clinicaRepository, provisioningService);
		AtomicReference<Clinica> salva = new AtomicReference<>();
		when(clinicaRepository.save(any(Clinica.class))).thenAnswer(invocation -> {
			Clinica clinica = invocation.getArgument(0);
			clinica.setId(20L);
			salva.set(clinica);
			return clinica;
		});
		when(clinicaRepository.findById(20L)).thenAnswer(invocation -> Optional.ofNullable(salva.get()));
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
		ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);

		service.criar(new ClinicaRequest("Clinica Central", "12345678000199", "Rua A", "11999999999", "contato@clinica.com", null));

		verify(usuarioRepository).save(usuarioCaptor.capture());
		Usuario usuario = usuarioCaptor.getValue();
		assertEquals(TipoUsuario.ADMIN_CLINICA, usuario.getTipo());
		assertEquals("contato@clinica.com", usuario.getLogin());
		assertTrue(passwordEncoder.matches("contato@clinica.com", usuario.getSenhaHash()));
		assertNotEquals("contato@clinica.com", usuario.getSenhaHash());
		assertEquals("S", usuario.getTrocaSenha());
		assertEquals("S", usuario.getAtivo());
		assertNotNull(usuario.getClinica());
		assertEquals(20L, usuario.getClinica().getId());
	}

	@Test
	void criarClinicaRejeitaEmailAusenteSemPersistir() {
		ClinicaRepository clinicaRepository = mock(ClinicaRepository.class);
		AccountProvisioningService provisioningService = mock(AccountProvisioningService.class);
		ClinicaService service = new ClinicaService(clinicaRepository, provisioningService);

		assertThrows(BusinessException.class, () -> service.criar(new ClinicaRequest(
				"Clinica Central",
				"12345678000199",
				"Rua A",
				"11999999999",
				" ",
				null
		)));
		verify(clinicaRepository, never()).save(any());
		verify(provisioningService, never()).provisionar(any());
	}

	@Test
	void falhaDeProvisionamentoPropagaParaRollbackTransacional() throws Exception {
		ClinicaRepository clinicaRepository = mock(ClinicaRepository.class);
		AccountProvisioningService provisioningService = mock(AccountProvisioningService.class);
		ClinicaService service = new ClinicaService(clinicaRepository, provisioningService);
		when(clinicaRepository.save(any(Clinica.class))).thenAnswer(invocation -> {
			Clinica clinica = invocation.getArgument(0);
			clinica.setId(20L);
			return clinica;
		});
		doThrow(new BusinessException("Login de usuario ja cadastrado.")).when(provisioningService).provisionar(any());

		assertThrows(BusinessException.class, () -> service.criar(new ClinicaRequest(
				"Clinica Central",
				"12345678000199",
				"Rua A",
				"11999999999",
				"contato@clinica.com",
				null
		)));
		Method criar = ClinicaService.class.getMethod("criar", ClinicaRequest.class);
		assertTrue(criar.isAnnotationPresent(Transactional.class));
	}

	@Test
	void atualizarEmailNaoProvisionaSegundaConta() {
		ClinicaRepository clinicaRepository = mock(ClinicaRepository.class);
		AccountProvisioningService provisioningService = mock(AccountProvisioningService.class);
		ClinicaService service = new ClinicaService(clinicaRepository, provisioningService);
		Clinica clinica = new Clinica();
		clinica.setId(20L);
		clinica.setNome("Clinica Central");
		clinica.setCnpj("12345678000199");
		clinica.setEmail("contato@clinica.com");
		clinica.setAtivo("S");
		when(clinicaRepository.findById(20L)).thenReturn(Optional.of(clinica));
		when(clinicaRepository.save(any(Clinica.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.atualizar(20L, new ClinicaRequest("Clinica Central", "12345678000199", "Rua A", "11999999999", "novo@clinica.com", "S"));

		assertEquals("novo@clinica.com", clinica.getEmail());
		verify(provisioningService, never()).provisionar(any());
	}
}
