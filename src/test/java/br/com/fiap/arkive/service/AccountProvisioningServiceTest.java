package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.UsuarioProvisioningRequest;
import br.com.fiap.arkive.dto.request.UsuarioRequest;
import br.com.fiap.arkive.dto.response.AccountProvisioningResult;
import br.com.fiap.arkive.dto.response.UsuarioResponse;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountProvisioningServiceTest {

	@Test
	void provisionaContaAtivaComEmailComoCredencialInicialETrocaObrigatoria() {
		UsuarioService usuarioService = mock(UsuarioService.class);
		AccountProvisioningService service = new AccountProvisioningService(usuarioService);
		UsuarioResponse usuarioResponse = new UsuarioResponse(
				10L,
				"Dra Vera",
				TipoUsuario.VETERINARIO,
				"vera@arkive.com",
				"S",
				LocalDateTime.now(),
				null,
				null,
				1L,
				"Dra Vera",
				null,
				null,
				true,
				null
		);
		when(usuarioService.criarProvisionado(any())).thenReturn(usuarioResponse);

		AccountProvisioningResult result = service.provisionar(new UsuarioProvisioningRequest(
				"Dra Vera",
				TipoUsuario.VETERINARIO,
				"  vera@arkive.com  ",
				null,
				1L,
				null
		));

		assertEquals(usuarioResponse, result.usuario());
		verify(usuarioService).criarProvisionado(new UsuarioRequest(
				"Dra Vera",
				TipoUsuario.VETERINARIO,
				"vera@arkive.com",
				"vera@arkive.com",
				null,
				1L,
				null,
				"S"
		));
	}

	@Test
	void rejeitaLoginSemFormatoDeEmail() {
		UsuarioService usuarioService = mock(UsuarioService.class);
		AccountProvisioningService service = new AccountProvisioningService(usuarioService);

		assertThrows(BusinessException.class, () -> service.provisionar(new UsuarioProvisioningRequest(
				"Usuario",
				TipoUsuario.SYSADMIN,
				"login-invalido",
				null,
				null,
				null
		)));
	}

	@Test
	void rejeitaEmailMaiorQueLimiteDoBcryptParaCredencialInicial() {
		UsuarioService usuarioService = mock(UsuarioService.class);
		AccountProvisioningService service = new AccountProvisioningService(usuarioService);
		String loginMuitoLongo = "usuario." + "a".repeat(65) + "@arkive.com";

		assertThrows(BusinessException.class, () -> service.provisionar(new UsuarioProvisioningRequest(
				"Usuario Longo",
				TipoUsuario.SYSADMIN,
				loginMuitoLongo,
				null,
				null,
				null
		)));
	}
}
