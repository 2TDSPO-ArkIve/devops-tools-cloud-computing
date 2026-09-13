package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.UsuarioProvisioningRequest;
import br.com.fiap.arkive.dto.request.VeterinarioRequest;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.entity.Veterinario;
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

class VeterinarioServiceTest {

	@Test
	void criarVeterinarioProvisionaUsuarioComPerfilEVinculo() {
		VeterinarioRepository veterinarioRepository = mock(VeterinarioRepository.class);
		ClinicaService clinicaService = mock(ClinicaService.class);
		AccountProvisioningService provisioningService = mock(AccountProvisioningService.class);
		VeterinarioService service = new VeterinarioService(veterinarioRepository, clinicaService, provisioningService);
		when(veterinarioRepository.save(any(Veterinario.class))).thenAnswer(invocation -> {
			Veterinario veterinario = invocation.getArgument(0);
			veterinario.setId(10L);
			return veterinario;
		});
		ArgumentCaptor<UsuarioProvisioningRequest> captor = ArgumentCaptor.forClass(UsuarioProvisioningRequest.class);

		service.criar(new VeterinarioRequest("Dra Vera", "CRMV123", "Clínica", "vera@arkive.com", null, null));

		verify(provisioningService).provisionar(captor.capture());
		assertEquals("Dra Vera", captor.getValue().nome());
		assertEquals(TipoUsuario.VETERINARIO, captor.getValue().tipo());
		assertEquals("vera@arkive.com", captor.getValue().login());
		assertEquals(10L, captor.getValue().veterinarioId());
		assertNull(captor.getValue().responsavelId());
		assertNull(captor.getValue().clinicaId());
	}

	@Test
	void criarVeterinarioComProvisionamentoRealPersisteUsuarioComBcryptETrocaObrigatoria() {
		VeterinarioRepository veterinarioRepository = mock(VeterinarioRepository.class);
		ClinicaService clinicaService = mock(ClinicaService.class);
		UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
		ResponsavelRepository responsavelRepository = mock(ResponsavelRepository.class);
		ClinicaRepository clinicaRepository = mock(ClinicaRepository.class);
		BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
		UsuarioService usuarioService = new UsuarioService(
				usuarioRepository,
				responsavelRepository,
				veterinarioRepository,
				clinicaRepository,
				passwordEncoder
		);
		AccountProvisioningService provisioningService = new AccountProvisioningService(usuarioService);
		VeterinarioService service = new VeterinarioService(veterinarioRepository, clinicaService, provisioningService);
		AtomicReference<Veterinario> salvo = new AtomicReference<>();
		when(veterinarioRepository.save(any(Veterinario.class))).thenAnswer(invocation -> {
			Veterinario veterinario = invocation.getArgument(0);
			veterinario.setId(10L);
			salvo.set(veterinario);
			return veterinario;
		});
		when(veterinarioRepository.findById(10L)).thenAnswer(invocation -> Optional.ofNullable(salvo.get()));
		when(usuarioRepository.existsByVeterinarioId(10L)).thenReturn(false);
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
		ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);

		service.criar(new VeterinarioRequest("Dra Vera", "CRMV123", "Clínica", "vera@arkive.com", null, null));

		verify(usuarioRepository).save(usuarioCaptor.capture());
		Usuario usuario = usuarioCaptor.getValue();
		assertEquals(TipoUsuario.VETERINARIO, usuario.getTipo());
		assertEquals("vera@arkive.com", usuario.getLogin());
		assertTrue(passwordEncoder.matches("vera@arkive.com", usuario.getSenhaHash()));
		assertNotEquals("vera@arkive.com", usuario.getSenhaHash());
		assertEquals("S", usuario.getTrocaSenha());
		assertEquals("S", usuario.getAtivo());
		assertNotNull(usuario.getVeterinario());
		assertEquals(10L, usuario.getVeterinario().getId());
	}

	@Test
	void criarVeterinarioRejeitaEmailAusenteSemPersistir() {
		VeterinarioRepository veterinarioRepository = mock(VeterinarioRepository.class);
		AccountProvisioningService provisioningService = mock(AccountProvisioningService.class);
		VeterinarioService service = new VeterinarioService(veterinarioRepository, mock(ClinicaService.class), provisioningService);

		assertThrows(BusinessException.class, () -> service.criar(new VeterinarioRequest(
				"Dra Vera",
				"CRMV123",
				"Clínica",
				" ",
				null,
				null
		)));
		verify(veterinarioRepository, never()).save(any());
		verify(provisioningService, never()).provisionar(any());
	}

	@Test
	void criarVeterinarioRejeitaCrmvDuplicadoSemPersistir() {
		VeterinarioRepository veterinarioRepository = mock(VeterinarioRepository.class);
		AccountProvisioningService provisioningService = mock(AccountProvisioningService.class);
		VeterinarioService service = new VeterinarioService(veterinarioRepository, mock(ClinicaService.class), provisioningService);
		when(veterinarioRepository.existsByCrmvIgnoreCase("CRMV123")).thenReturn(true);

		BusinessException ex = assertThrows(BusinessException.class, () -> service.criar(new VeterinarioRequest(
				"Dra Vera",
				"CRMV123",
				"Clínica",
				"vera@arkive.com",
				null,
				null
		)));
		assertEquals(org.springframework.http.HttpStatus.CONFLICT, ex.getStatus());
		verify(veterinarioRepository, never()).save(any());
		verify(provisioningService, never()).provisionar(any());
	}

	@Test
	void falhaDeProvisionamentoPropagaParaRollbackTransacional() throws Exception {
		VeterinarioRepository veterinarioRepository = mock(VeterinarioRepository.class);
		AccountProvisioningService provisioningService = mock(AccountProvisioningService.class);
		VeterinarioService service = new VeterinarioService(veterinarioRepository, mock(ClinicaService.class), provisioningService);
		when(veterinarioRepository.save(any(Veterinario.class))).thenAnswer(invocation -> {
			Veterinario veterinario = invocation.getArgument(0);
			veterinario.setId(10L);
			return veterinario;
		});
		doThrow(new BusinessException("Login de usuario ja cadastrado.")).when(provisioningService).provisionar(any());

		assertThrows(BusinessException.class, () -> service.criar(new VeterinarioRequest(
				"Dra Vera",
				"CRMV123",
				"Clínica",
				"vera@arkive.com",
				null,
				null
		)));
		Method criar = VeterinarioService.class.getMethod("criar", VeterinarioRequest.class);
		assertTrue(criar.isAnnotationPresent(Transactional.class));
	}

	@Test
	void atualizarEmailNaoProvisionaSegundaConta() {
		VeterinarioRepository veterinarioRepository = mock(VeterinarioRepository.class);
		AccountProvisioningService provisioningService = mock(AccountProvisioningService.class);
		VeterinarioService service = new VeterinarioService(veterinarioRepository, mock(ClinicaService.class), provisioningService);
		Veterinario veterinario = new Veterinario();
		veterinario.setId(10L);
		veterinario.setNome("Dra Vera");
		veterinario.setCrmv("CRMV123");
		veterinario.setEmail("vera@arkive.com");
		veterinario.setAtivo("S");
		when(veterinarioRepository.findById(10L)).thenReturn(Optional.of(veterinario));
		when(veterinarioRepository.save(any(Veterinario.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.atualizar(10L, new VeterinarioRequest("Dra Vera", "CRMV123", "Clínica", "nova.vera@arkive.com", null, "S"));

		assertEquals("nova.vera@arkive.com", veterinario.getEmail());
		verify(provisioningService, never()).provisionar(any());
	}
}
