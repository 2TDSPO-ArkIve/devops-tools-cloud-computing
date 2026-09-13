package br.com.fiap.arkive.security;

import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.repository.UsuarioRepository;
import br.com.fiap.arkive.service.PasswordLifecycleService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VeterinarioAuthenticationFlowTest {

	@Test
	void veterinarioAutenticaPorEmailECrmvComSenhaTemporariaEDepoisComNovaSenha() {
		UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
		BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
		Usuario usuario = usuarioVeterinario(passwordEncoder.encode("vet@arkive.com.br"));
		ArkiveUserDetailsService userDetailsService = new ArkiveUserDetailsService(usuarioRepository);
		PasswordLifecycleService passwordLifecycleService = new PasswordLifecycleService(
				usuarioRepository,
				passwordEncoder,
				new PasswordPolicy(),
				mock(TemporaryPasswordGenerator.class)
		);
		when(usuarioRepository.findByLoginIgnoreCase("vet@arkive.com.br")).thenReturn(Optional.of(usuario));
		when(usuarioRepository.findByLoginIgnoreCase("CRMV12345")).thenReturn(Optional.empty());
		when(usuarioRepository.findVeterinarioByCrmvIgnoreCase("CRMV12345")).thenReturn(Optional.of(usuario));
		when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

		UserDetails porEmail = userDetailsService.loadUserByUsername("vet@arkive.com.br");
		UserDetails porCrmv = userDetailsService.loadUserByUsername("CRMV12345");

		assertEquals(porEmail.getUsername(), porCrmv.getUsername());
		assertTrue(passwordEncoder.matches("vet@arkive.com.br", porEmail.getPassword()));
		assertTrue(passwordEncoder.matches("vet@arkive.com.br", porCrmv.getPassword()));
		assertSame(usuario, passwordLifecycleService.alterarSenhaObrigatoria(1L, "NovaSenha1", "NovaSenha1"));
		assertEquals("N", usuario.getTrocaSenha());

		assertFalse(passwordEncoder.matches("vet@arkive.com.br", userDetailsService.loadUserByUsername("vet@arkive.com.br").getPassword()));
		assertTrue(passwordEncoder.matches("NovaSenha1", userDetailsService.loadUserByUsername("vet@arkive.com.br").getPassword()));
		assertTrue(passwordEncoder.matches("NovaSenha1", userDetailsService.loadUserByUsername("CRMV12345").getPassword()));
	}

	private Usuario usuarioVeterinario(String senhaHash) {
		Veterinario veterinario = new Veterinario();
		veterinario.setId(45L);
		veterinario.setCrmv("CRMV12345");
		veterinario.setEmail("vet@arkive.com.br");
		veterinario.setAtivo("S");

		Usuario usuario = new Usuario();
		usuario.setId(1L);
		usuario.setNome("Dra Vera");
		usuario.setTipo(TipoUsuario.VETERINARIO);
		usuario.setLogin("vet@arkive.com.br");
		usuario.setSenhaHash(senhaHash);
		usuario.setAtivo("S");
		usuario.setTrocaSenha("S");
		usuario.setVeterinario(veterinario);
		return usuario;
	}
}
