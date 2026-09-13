package br.com.fiap.arkive.security;

import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArkiveUserDetailsServiceTest {

	private UsuarioRepository usuarioRepository;
	private ArkiveUserDetailsService userDetailsService;

	@BeforeEach
	void setUp() {
		usuarioRepository = mock(UsuarioRepository.class);
		userDetailsService = new ArkiveUserDetailsService(usuarioRepository);
	}

	@Test
	void loginExistenteRetornaPrincipal() {
		when(usuarioRepository.findByLoginIgnoreCase("usuario@arkive.com")).thenReturn(Optional.of(usuario("S")));

		UserDetails userDetails = userDetailsService.loadUserByUsername("usuario@arkive.com");

		assertEquals("usuario@arkive.com", userDetails.getUsername());
		assertEquals("$2a$10$hash", userDetails.getPassword());
		assertEquals("ROLE_SYSADMIN", userDetails.getAuthorities().iterator().next().getAuthority());
	}

	@Test
	void loginInexistenteLancaUsernameNotFound() {
		when(usuarioRepository.findByLoginIgnoreCase("usuario@arkive.com")).thenReturn(Optional.empty());
		when(usuarioRepository.findVeterinarioByCrmvIgnoreCase("usuario@arkive.com")).thenReturn(Optional.empty());

		assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("usuario@arkive.com"));
	}

	@Test
	void usuarioInativoPermaneceDesabilitado() {
		when(usuarioRepository.findByLoginIgnoreCase("usuario@arkive.com")).thenReturn(Optional.of(usuario("N")));

		UserDetails userDetails = userDetailsService.loadUserByUsername("usuario@arkive.com");

		assertFalse(userDetails.isEnabled());
	}

	@Test
	void crmvResolveMesmoUsuarioVeterinarioQuandoLoginNaoExiste() {
		Usuario usuario = usuario("S");
		usuario.setTipo(TipoUsuario.VETERINARIO);
		usuario.setVeterinario(veterinario());
		when(usuarioRepository.findByLoginIgnoreCase("CRMV12345")).thenReturn(Optional.empty());
		when(usuarioRepository.findVeterinarioByCrmvIgnoreCase("CRMV12345")).thenReturn(Optional.of(usuario));

		UserDetails userDetails = userDetailsService.loadUserByUsername(" CRMV12345 ");

		assertEquals("usuario@arkive.com", userDetails.getUsername());
		assertEquals("ROLE_VETERINARIO", userDetails.getAuthorities().iterator().next().getAuthority());
	}

	private Usuario usuario(String ativo) {
		Usuario usuario = new Usuario();
		usuario.setId(1L);
		usuario.setNome("Usuario");
		usuario.setLogin("usuario@arkive.com");
		usuario.setSenhaHash("$2a$10$hash");
		usuario.setTipo(TipoUsuario.SYSADMIN);
		usuario.setAtivo(ativo);
		return usuario;
	}

	private Veterinario veterinario() {
		Veterinario veterinario = new Veterinario();
		veterinario.setId(45L);
		veterinario.setCrmv("CRMV12345");
		veterinario.setEmail("usuario@arkive.com");
		veterinario.setAtivo("S");
		return veterinario;
	}

}
