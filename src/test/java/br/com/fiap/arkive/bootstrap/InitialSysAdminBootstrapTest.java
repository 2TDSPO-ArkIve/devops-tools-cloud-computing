package br.com.fiap.arkive.bootstrap;

import br.com.fiap.arkive.dto.request.UsuarioRequest;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.repository.UsuarioRepository;
import br.com.fiap.arkive.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class InitialSysAdminBootstrapTest {

	private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
	private final UsuarioService usuarioService = mock(UsuarioService.class);

	@Test
	void bootstrapDesabilitadoNaoInterageComCriacao() {
		InitialSysAdminBootstrap bootstrap = bootstrap(properties(false, "", "", ""));

		bootstrap.run(null);

		verifyNoInteractions(usuarioRepository, usuarioService);
	}

	@Test
	void sysadminAtivoExistenteIgnoraCriacaoEConfiguracao() {
		when(usuarioRepository.countByTipoAndAtivo(TipoUsuario.SYSADMIN, "S")).thenReturn(1L);
		InitialSysAdminBootstrap bootstrap = bootstrap(properties(true, "", "", ""));

		bootstrap.run(null);

		verify(usuarioRepository).countByTipoAndAtivo(TipoUsuario.SYSADMIN, "S");
		verify(usuarioService, never()).criar(any());
		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void sysadminAusenteCriaContaPeloUsuarioService() {
		when(usuarioRepository.countByTipoAndAtivo(TipoUsuario.SYSADMIN, "S")).thenReturn(0L);
		InitialSysAdminBootstrap bootstrap = bootstrap(properties(
				true,
				"Ana SysAdmin",
				"ana.sysadmin@arkive.com",
				"senha-segura"
		));
		ArgumentCaptor<UsuarioRequest> captor = ArgumentCaptor.forClass(UsuarioRequest.class);

		bootstrap.run(null);

		verify(usuarioService).criar(captor.capture());
		verify(usuarioRepository, never()).save(any());
		UsuarioRequest request = captor.getValue();
		assertEquals("Ana SysAdmin", request.nome());
		assertEquals(TipoUsuario.SYSADMIN, request.tipo());
		assertEquals("ana.sysadmin@arkive.com", request.login());
		assertEquals("senha-segura", request.senha());
		assertEquals("S", request.ativo());
		assertNull(request.responsavelId());
		assertNull(request.veterinarioId());
		assertNull(request.clinicaId());
	}

	@Test
	void rejeitaNomeBootstrapAusente() {
		assertMissingConfiguration(properties(true, " ", "ana.sysadmin@arkive.com", "senha-segura"), "name");
	}

	@Test
	void rejeitaLoginBootstrapAusente() {
		assertMissingConfiguration(properties(true, "Ana SysAdmin", " ", "senha-segura"), "login");
	}

	@Test
	void rejeitaSenhaBootstrapAusenteSemExporValor() {
		IllegalStateException exception = assertMissingConfiguration(
				properties(true, "Ana SysAdmin", "ana.sysadmin@arkive.com", " "),
				"password"
		);

		assertFalse(exception.getMessage().contains("senha-segura"));
	}

	@Test
	void falhaDoUsuarioServicePropagaSemPersistenciaDireta() {
		when(usuarioRepository.countByTipoAndAtivo(TipoUsuario.SYSADMIN, "S")).thenReturn(0L);
		when(usuarioService.criar(any())).thenThrow(new BusinessException("Login de usuario ja cadastrado."));
		InitialSysAdminBootstrap bootstrap = bootstrap(properties(
				true,
				"Ana SysAdmin",
				"ana.sysadmin@arkive.com",
				"senha-segura"
		));

		assertThrows(BusinessException.class, () -> bootstrap.run(null));
		verify(usuarioRepository, never()).save(any());
	}

	private IllegalStateException assertMissingConfiguration(BootstrapSysAdminProperties properties, String propertyName) {
		when(usuarioRepository.countByTipoAndAtivo(TipoUsuario.SYSADMIN, "S")).thenReturn(0L);
		InitialSysAdminBootstrap bootstrap = bootstrap(properties);

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> bootstrap.run(null));
		assertEquals("Initial SysAdmin bootstrap requires " + propertyName
				+ " to be configured with ARKIVE_BOOTSTRAP_SYSADMIN_" + propertyName.toUpperCase() + ".", exception.getMessage());
		verify(usuarioService, never()).criar(any());
		verify(usuarioRepository, never()).save(any());
		return exception;
	}

	private InitialSysAdminBootstrap bootstrap(BootstrapSysAdminProperties properties) {
		return new InitialSysAdminBootstrap(properties, usuarioRepository, usuarioService);
	}

	private BootstrapSysAdminProperties properties(boolean enabled, String name, String login, String password) {
		BootstrapSysAdminProperties properties = new BootstrapSysAdminProperties();
		properties.setEnabled(enabled);
		properties.setName(name);
		properties.setLogin(login);
		properties.setPassword(password);
		return properties;
	}

}
