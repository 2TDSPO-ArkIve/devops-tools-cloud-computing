package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.response.PasswordResetResult;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.repository.UsuarioRepository;
import br.com.fiap.arkive.security.PasswordPolicy;
import br.com.fiap.arkive.security.TemporaryPasswordGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordLifecycleServiceTest {

	private UsuarioRepository usuarioRepository;
	private PasswordEncoder passwordEncoder;
	private TemporaryPasswordGenerator temporaryPasswordGenerator;
	private PasswordLifecycleService service;

	@BeforeEach
	void setUp() {
		usuarioRepository = mock(UsuarioRepository.class);
		passwordEncoder = mock(PasswordEncoder.class);
		temporaryPasswordGenerator = mock(TemporaryPasswordGenerator.class);
		service = new PasswordLifecycleService(usuarioRepository, passwordEncoder, new PasswordPolicy(), temporaryPasswordGenerator);
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void trocaObrigatoriaSubstituiHashELimpaFlag() {
		Usuario usuario = usuario();
		when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
		when(passwordEncoder.encode("NovaSenha1")).thenReturn("$2a$10$novoHash");

		Usuario atualizado = service.alterarSenhaObrigatoria(1L, "NovaSenha1", "NovaSenha1");

		assertEquals("$2a$10$novoHash", atualizado.getSenhaHash());
		assertEquals("N", atualizado.getTrocaSenha());
		assertNotNull(atualizado.getDataUltimaTrocaSenha());
		assertNotEquals("NovaSenha1", atualizado.getSenhaHash());
		verify(usuarioRepository).save(usuario);
	}

	@Test
	void trocaNormalExigeSenhaAtualCorreta() {
		Usuario usuario = usuario();
		when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
		when(passwordEncoder.matches("Atual123", "$2a$10$hashAtual")).thenReturn(true);
		when(passwordEncoder.encode("NovaSenha1")).thenReturn("$2a$10$novoHash");

		service.alterarSenhaAutenticado(1L, "Atual123", "NovaSenha1", "NovaSenha1");

		assertEquals("$2a$10$novoHash", usuario.getSenhaHash());
		assertEquals("N", usuario.getTrocaSenha());
	}

	@Test
	void trocaNormalRejeitaSenhaAtualIncorretaConfirmacaoEPolitica() {
		Usuario usuario = usuario();
		when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
		when(passwordEncoder.matches("Errada123", "$2a$10$hashAtual")).thenReturn(false);

		assertThrows(BusinessException.class, () -> service.alterarSenhaAutenticado(1L, "Errada123", "NovaSenha1", "NovaSenha1"));
		assertThrows(BusinessException.class, () -> service.alterarSenhaObrigatoria(1L, "NovaSenha1", "OutraSenha1"));
		assertThrows(BusinessException.class, () -> service.alterarSenhaObrigatoria(1L, "fraca", "fraca"));
		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void resetGeraSenhaTemporariaPersistindoSomenteHashEFlag() {
		Usuario usuario = usuario();
		when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
		when(temporaryPasswordGenerator.gerar()).thenReturn("TempSenha1");
		when(passwordEncoder.encode("TempSenha1")).thenReturn("$2a$10$tempHash");

		PasswordResetResult result = service.resetarSenha(1L);

		assertEquals(1L, result.usuarioId());
		assertEquals("TempSenha1", result.senhaTemporaria());
		assertEquals("$2a$10$tempHash", usuario.getSenhaHash());
		assertEquals("S", usuario.getTrocaSenha());
		assertNotEquals("TempSenha1", usuario.getSenhaHash());
		assertNotNull(usuario.getDataUltimaTrocaSenha());
		verify(temporaryPasswordGenerator).gerar();
		verify(passwordEncoder, never()).encode("ana@arkive.com");
		verify(usuarioRepository).save(usuario);
	}

	private Usuario usuario() {
		Usuario usuario = new Usuario();
		usuario.setId(1L);
		usuario.setNome("Ana");
		usuario.setLogin("ana@arkive.com");
		usuario.setTipo(TipoUsuario.SYSADMIN);
		usuario.setSenhaHash("$2a$10$hashAtual");
		usuario.setAtivo("S");
		usuario.setTrocaSenha("S");
		return usuario;
	}
}
