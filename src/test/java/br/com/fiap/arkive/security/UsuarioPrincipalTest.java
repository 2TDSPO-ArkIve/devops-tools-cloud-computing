package br.com.fiap.arkive.security;

import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.Responsavel;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.entity.Veterinario;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsuarioPrincipalTest {

	@Test
	void mapeiaSysadminParaRoleSysadmin() {
		assertEquals("ROLE_SYSADMIN", principal(TipoUsuario.SYSADMIN, "S").getAuthorities().iterator().next().getAuthority());
	}

	@Test
	void mapeiaAdminClinicaParaRoleAdminClinica() {
		assertEquals("ROLE_ADMIN_CLINICA", principal(TipoUsuario.ADMIN_CLINICA, "S").getAuthorities().iterator().next().getAuthority());
	}

	@Test
	void mapeiaVeterinarioParaRoleVeterinario() {
		assertEquals("ROLE_VETERINARIO", principal(TipoUsuario.VETERINARIO, "S").getAuthorities().iterator().next().getAuthority());
	}

	@Test
	void mapeiaResponsavelParaRoleResponsavel() {
		assertEquals("ROLE_RESPONSAVEL", principal(TipoUsuario.RESPONSAVEL, "S").getAuthorities().iterator().next().getAuthority());
	}

	@Test
	void ativoSHabilitaUsuario() {
		assertTrue(principal(TipoUsuario.SYSADMIN, "S").isEnabled());
	}

	@Test
	void ativoNDesabilitaUsuario() {
		assertFalse(principal(TipoUsuario.SYSADMIN, "N").isEnabled());
	}

	@Test
	void expoeTrocaSenhaObrigatoriaSemEntidadeJpa() {
		UsuarioPrincipal principal = new UsuarioPrincipal(1L, "Usuario", "usuario@arkive.com", "$2a$10$hash", TipoUsuario.SYSADMIN, "S", true);

		assertTrue(principal.isTrocaSenhaObrigatoria());
	}

	@Test
	void veterinarioExpoeApenasIdVeterinario() {
		Veterinario veterinario = new Veterinario();
		veterinario.setId(10L);
		Usuario usuario = usuario(TipoUsuario.VETERINARIO);
		usuario.setVeterinario(veterinario);

		UsuarioPrincipal principal = UsuarioPrincipal.fromEntity(usuario);

		assertEquals(10L, principal.getVeterinarioId());
		assertEquals(TipoUsuario.VETERINARIO, principal.getTipoUsuario());
		assertEquals(null, principal.getResponsavelId());
		assertEquals(null, principal.getClinicaId());
	}

	@Test
	void responsavelExpoeApenasIdResponsavel() {
		Responsavel responsavel = new Responsavel();
		responsavel.setId(20L);
		Usuario usuario = usuario(TipoUsuario.RESPONSAVEL);
		usuario.setResponsavel(responsavel);

		UsuarioPrincipal principal = UsuarioPrincipal.fromEntity(usuario);

		assertEquals(20L, principal.getResponsavelId());
		assertEquals(null, principal.getVeterinarioId());
		assertEquals(null, principal.getClinicaId());
	}

	@Test
	void adminClinicaExpoeApenasIdClinica() {
		Clinica clinica = new Clinica();
		clinica.setId(30L);
		Usuario usuario = usuario(TipoUsuario.ADMIN_CLINICA);
		usuario.setClinica(clinica);

		UsuarioPrincipal principal = UsuarioPrincipal.fromEntity(usuario);

		assertEquals(30L, principal.getClinicaId());
		assertEquals(null, principal.getResponsavelId());
		assertEquals(null, principal.getVeterinarioId());
	}

	@Test
	void sysadminNaoExpoeAssociacaoContextual() {
		UsuarioPrincipal principal = UsuarioPrincipal.fromEntity(usuario(TipoUsuario.SYSADMIN));

		assertEquals(null, principal.getResponsavelId());
		assertEquals(null, principal.getVeterinarioId());
		assertEquals(null, principal.getClinicaId());
	}

	private UsuarioPrincipal principal(TipoUsuario tipo, String ativo) {
		return new UsuarioPrincipal(1L, "Usuario", "usuario@arkive.com", "$2a$10$hash", tipo, ativo);
	}

	private Usuario usuario(TipoUsuario tipo) {
		Usuario usuario = new Usuario();
		usuario.setId(1L);
		usuario.setNome("Usuario");
		usuario.setLogin("usuario@arkive.com");
		usuario.setSenhaHash("$2a$10$hash");
		usuario.setTipo(tipo);
		usuario.setAtivo("S");
		usuario.setTrocaSenha("S");
		return usuario;
	}

}
