package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.UsuarioRequest;
import br.com.fiap.arkive.dto.request.UsuarioEditRequest;
import br.com.fiap.arkive.dto.response.UsuarioResponse;
import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.Responsavel;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Usuario;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.repository.ClinicaRepository;
import br.com.fiap.arkive.repository.ResponsavelRepository;
import br.com.fiap.arkive.repository.UsuarioRepository;
import br.com.fiap.arkive.repository.VeterinarioRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsuarioServiceTest {

	private UsuarioRepository usuarioRepository;
	private ResponsavelRepository responsavelRepository;
	private VeterinarioRepository veterinarioRepository;
	private ClinicaRepository clinicaRepository;
	private PasswordEncoder passwordEncoder;
	private UsuarioService usuarioService;

	@BeforeEach
	void setUp() {
		usuarioRepository = mock(UsuarioRepository.class);
		responsavelRepository = mock(ResponsavelRepository.class);
		veterinarioRepository = mock(VeterinarioRepository.class);
		clinicaRepository = mock(ClinicaRepository.class);
		passwordEncoder = mock(PasswordEncoder.class);
		usuarioService = new UsuarioService(
				usuarioRepository,
				responsavelRepository,
				veterinarioRepository,
				clinicaRepository,
				passwordEncoder
		);
		when(usuarioRepository.existsByLogin("usuario@arkive.com")).thenReturn(false);
	}

	@Test
	void sysadminAceitaSemAssociacao() {
		assertDoesNotThrow(() -> usuarioService.validarCriacao(request(TipoUsuario.SYSADMIN, null, null, null)));
	}

	@Test
	void sysadminRejeitaQualquerAssociacao() {
		assertThrows(BusinessException.class, () -> usuarioService.validarCriacao(request(TipoUsuario.SYSADMIN, 1L, null, null)));
		assertThrows(BusinessException.class, () -> usuarioService.validarCriacao(request(TipoUsuario.SYSADMIN, null, 1L, null)));
		assertThrows(BusinessException.class, () -> usuarioService.validarCriacao(request(TipoUsuario.SYSADMIN, null, null, 1L)));
	}

	@Test
	void adminClinicaExigeClinica() {
		assertThrows(BusinessException.class, () -> usuarioService.validarCriacao(request(TipoUsuario.ADMIN_CLINICA, null, null, null)));
	}

	@Test
	void adminClinicaRejeitaResponsavelOuVeterinario() {
		assertThrows(BusinessException.class, () -> usuarioService.validarCriacao(request(TipoUsuario.ADMIN_CLINICA, 1L, null, 1L)));
		assertThrows(BusinessException.class, () -> usuarioService.validarCriacao(request(TipoUsuario.ADMIN_CLINICA, null, 1L, 1L)));
	}

	@Test
	void adminClinicaAceitaClinicaAtiva() {
		when(clinicaRepository.findById(1L)).thenReturn(Optional.of(clinicaAtiva()));

		assertDoesNotThrow(() -> usuarioService.validarCriacao(request(TipoUsuario.ADMIN_CLINICA, null, null, 1L)));
	}

	@Test
	void veterinarioExigeVeterinario() {
		assertThrows(BusinessException.class, () -> usuarioService.validarCriacao(request(TipoUsuario.VETERINARIO, null, null, null)));
	}

	@Test
	void veterinarioRejeitaResponsavelOuClinica() {
		assertThrows(BusinessException.class, () -> usuarioService.validarCriacao(request(TipoUsuario.VETERINARIO, 1L, 1L, null)));
		assertThrows(BusinessException.class, () -> usuarioService.validarCriacao(request(TipoUsuario.VETERINARIO, null, 1L, 1L)));
	}

	@Test
	void veterinarioAceitaVeterinarioAtivo() {
		when(veterinarioRepository.findById(1L)).thenReturn(Optional.of(veterinarioAtivo()));

		assertDoesNotThrow(() -> usuarioService.validarCriacao(request(TipoUsuario.VETERINARIO, null, 1L, null)));
	}

	@Test
	void responsavelExigeResponsavel() {
		assertThrows(BusinessException.class, () -> usuarioService.validarCriacao(request(TipoUsuario.RESPONSAVEL, null, null, null)));
	}

	@Test
	void responsavelRejeitaVeterinarioOuClinica() {
		assertThrows(BusinessException.class, () -> usuarioService.validarCriacao(request(TipoUsuario.RESPONSAVEL, 1L, 1L, null)));
		assertThrows(BusinessException.class, () -> usuarioService.validarCriacao(request(TipoUsuario.RESPONSAVEL, 1L, null, 1L)));
	}

	@Test
	void responsavelAceitaResponsavelAtivo() {
		when(responsavelRepository.findById(1L)).thenReturn(Optional.of(responsavelAtivo()));

		assertDoesNotThrow(() -> usuarioService.validarCriacao(request(TipoUsuario.RESPONSAVEL, 1L, null, null)));
	}

	@Test
	void rejeitaLoginDuplicado() {
		when(usuarioRepository.existsByLogin("usuario@arkive.com")).thenReturn(true);

		assertThrows(BusinessException.class, () -> usuarioService.validarCriacao(request(TipoUsuario.SYSADMIN, null, null, null)));
	}

	@Test
	void rejeitaReferenciaInexistente() {
		when(clinicaRepository.findById(1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> usuarioService.validarCriacao(request(TipoUsuario.ADMIN_CLINICA, null, null, 1L)));
	}

	@Test
	void rejeitaReferenciaInativa() {
		Responsavel responsavel = responsavelAtivo();
		responsavel.setAtivo("N");
		when(responsavelRepository.findById(1L)).thenReturn(Optional.of(responsavel));

		assertThrows(BusinessException.class, () -> usuarioService.validarCriacao(request(TipoUsuario.RESPONSAVEL, 1L, null, null)));
	}

	@Test
	void rejeitaAtivoInvalido() {
		UsuarioRequest request = new UsuarioRequest(
				"Usuario Teste",
				TipoUsuario.SYSADMIN,
				"usuario@arkive.com",
				"senha-segura",
				null,
				null,
				null,
				""
		);

		assertThrows(BusinessException.class, () -> usuarioService.criar(request));
		verify(passwordEncoder, never()).encode(any());
		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void criarCodificaSenhaAntesDePersistir() {
		when(passwordEncoder.encode("senha-segura")).thenReturn("$2a$10$hash");
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
		ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);

		UsuarioResponse response = usuarioService.criar(request(TipoUsuario.SYSADMIN, null, null, null));

		verify(passwordEncoder).encode("senha-segura");
		verify(usuarioRepository).save(captor.capture());
		Usuario salvo = captor.getValue();
		Assertions.assertEquals("$2a$10$hash", salvo.getSenhaHash());
		Assertions.assertNotEquals("senha-segura", salvo.getSenhaHash());
		Assertions.assertEquals("S", salvo.getAtivo());
		Assertions.assertEquals("N", salvo.getTrocaSenha());
		Assertions.assertEquals("usuario@arkive.com", response.login());
		Assertions.assertFalse(response.trocaSenhaObrigatoria());
		Assertions.assertTrue(Arrays.stream(UsuarioResponse.class.getRecordComponents())
				.noneMatch(component -> component.getName().toLowerCase().contains("hash")));
	}

	@Test
	void criarProvisionadoUsaLoginComoCredencialInicialComTrocaObrigatoria() {
		when(passwordEncoder.encode("novo@arkive.com")).thenReturn("$2a$10$emailHash");
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
		ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);

		UsuarioResponse response = usuarioService.criarProvisionado(new UsuarioRequest(
				"Novo Usuario",
				TipoUsuario.SYSADMIN,
				"  novo@arkive.com  ",
				"novo@arkive.com",
				null,
				null,
				null,
				"S"
		));

		verify(passwordEncoder).encode("novo@arkive.com");
		verify(usuarioRepository).save(captor.capture());
		Usuario salvo = captor.getValue();
		Assertions.assertEquals("novo@arkive.com", salvo.getLogin());
		Assertions.assertEquals("$2a$10$emailHash", salvo.getSenhaHash());
		Assertions.assertNotEquals("novo@arkive.com", salvo.getSenhaHash());
		Assertions.assertEquals("S", salvo.getTrocaSenha());
		Assertions.assertNull(salvo.getDataUltimaTrocaSenha());
		Assertions.assertTrue(response.trocaSenhaObrigatoria());
	}

	@Test
	void criarProvisionadoComBcryptPermitePrimeiraAutenticacaoComEmailComoSenha() {
		PasswordEncoder encoderReal = new BCryptPasswordEncoder();
		UsuarioRepository repository = mock(UsuarioRepository.class);
		UsuarioService service = new UsuarioService(
				repository,
				responsavelRepository,
				veterinarioRepository,
				clinicaRepository,
				encoderReal
		);
		when(repository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
		ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);

		service.criarProvisionado(new UsuarioRequest(
				"Novo Usuario",
				TipoUsuario.SYSADMIN,
				"novo@arkive.com",
				"novo@arkive.com",
				null,
				null,
				null,
				"S"
		));

		verify(repository).save(captor.capture());
		Usuario salvo = captor.getValue();
		Assertions.assertTrue(encoderReal.matches("novo@arkive.com", salvo.getSenhaHash()));
		Assertions.assertNotEquals("novo@arkive.com", salvo.getSenhaHash());
		Assertions.assertEquals("S", salvo.getTrocaSenha());
	}

	@Test
	void criarAdminClinicaValido() {
		Clinica clinica = clinicaAtiva();
		when(clinicaRepository.findById(1L)).thenReturn(Optional.of(clinica));
		when(passwordEncoder.encode("senha-segura")).thenReturn("$2a$10$hash");
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
		ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);

		usuarioService.criar(request(TipoUsuario.ADMIN_CLINICA, null, null, 1L));

		verify(usuarioRepository).save(captor.capture());
		Assertions.assertSame(clinica, captor.getValue().getClinica());
		Assertions.assertEquals("$2a$10$hash", captor.getValue().getSenhaHash());
	}

	@Test
	void criarVeterinarioValido() {
		Veterinario veterinario = veterinarioAtivo();
		when(veterinarioRepository.findById(1L)).thenReturn(Optional.of(veterinario));
		when(passwordEncoder.encode("senha-segura")).thenReturn("$2a$10$hash");
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
		ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);

		usuarioService.criar(request(TipoUsuario.VETERINARIO, null, 1L, null));

		verify(usuarioRepository).save(captor.capture());
		Assertions.assertSame(veterinario, captor.getValue().getVeterinario());
		Assertions.assertEquals("$2a$10$hash", captor.getValue().getSenhaHash());
	}

	@Test
	void criarResponsavelValido() {
		Responsavel responsavel = responsavelAtivo();
		when(responsavelRepository.findById(1L)).thenReturn(Optional.of(responsavel));
		when(passwordEncoder.encode("senha-segura")).thenReturn("$2a$10$hash");
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
		ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);

		usuarioService.criar(request(TipoUsuario.RESPONSAVEL, 1L, null, null));

		verify(usuarioRepository).save(captor.capture());
		Assertions.assertSame(responsavel, captor.getValue().getResponsavel());
		Assertions.assertEquals("$2a$10$hash", captor.getValue().getSenhaHash());
	}

	@Test
	void criarNaoPersisteQuandoLoginDuplicado() {
		when(usuarioRepository.existsByLogin("usuario@arkive.com")).thenReturn(true);

		assertThrows(BusinessException.class, () -> usuarioService.criar(request(TipoUsuario.SYSADMIN, null, null, null)));
		verify(passwordEncoder, never()).encode(any());
		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void criarMantemValidacaoDeAssociacao() {
		assertThrows(BusinessException.class, () -> usuarioService.criar(request(TipoUsuario.ADMIN_CLINICA, null, null, null)));
		verify(passwordEncoder, never()).encode(any());
		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void desativarContaAtivaAlteraParaInativo() {
		Usuario usuario = usuario(10L, TipoUsuario.RESPONSAVEL, "S");
		when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));

		usuarioService.desativar(10L, 99L);

		Assertions.assertEquals("N", usuario.getAtivo());
		verify(usuarioRepository).save(usuario);
		verify(usuarioRepository, never()).countByTipoAndAtivo(any(), any());
	}

	@Test
	void desativarUsuarioInexistenteRejeita() {
		when(usuarioRepository.findById(10L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> usuarioService.desativar(10L, 99L));
		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void desativarContaAtualRejeita() {
		Usuario usuario = usuario(10L, TipoUsuario.SYSADMIN, "S");
		when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));

		assertThrows(BusinessException.class, () -> usuarioService.desativar(10L, 10L));
		verify(usuarioRepository, never()).save(any());
		verify(usuarioRepository, never()).countByTipoAndAtivo(any(), any());
	}

	@Test
	void desativarUltimoSysadminAtivoRejeita() {
		Usuario usuario = usuario(10L, TipoUsuario.SYSADMIN, "S");
		when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
		when(usuarioRepository.countByTipoAndAtivo(TipoUsuario.SYSADMIN, "S")).thenReturn(1L);

		assertThrows(BusinessException.class, () -> usuarioService.desativar(10L, 99L));
		verify(usuarioRepository, never()).save(any());
		verify(usuarioRepository).countByTipoAndAtivo(TipoUsuario.SYSADMIN, "S");
	}

	@Test
	void desativarSysadminQuandoExisteOutroAtivo() {
		Usuario usuario = usuario(10L, TipoUsuario.SYSADMIN, "S");
		when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
		when(usuarioRepository.countByTipoAndAtivo(TipoUsuario.SYSADMIN, "S")).thenReturn(2L);

		usuarioService.desativar(10L, 99L);

		Assertions.assertEquals("N", usuario.getAtivo());
		verify(usuarioRepository).save(usuario);
		verify(usuarioRepository).countByTipoAndAtivo(TipoUsuario.SYSADMIN, "S");
	}

	@Test
	void desativarUsuarioJaInativoRejeita() {
		Usuario usuario = usuario(10L, TipoUsuario.RESPONSAVEL, "N");
		when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));

		assertThrows(BusinessException.class, () -> usuarioService.desativar(10L, 99L));
		verify(usuarioRepository, never()).save(any());
		verify(usuarioRepository, never()).countByTipoAndAtivo(any(), any());
	}

	@Test
	void ativarContaInativaAlteraParaAtivo() {
		Usuario usuario = usuario(10L, TipoUsuario.RESPONSAVEL, "N");
		when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));

		usuarioService.ativar(10L);

		Assertions.assertEquals("S", usuario.getAtivo());
		verify(usuarioRepository).save(usuario);
	}

	@Test
	void ativarUsuarioInexistenteRejeita() {
		when(usuarioRepository.findById(10L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> usuarioService.ativar(10L));
		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void ativarUsuarioJaAtivoRejeita() {
		Usuario usuario = usuario(10L, TipoUsuario.RESPONSAVEL, "S");
		when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));

		assertThrows(BusinessException.class, () -> usuarioService.ativar(10L));
		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void editarCorrigeNomeLoginEPerfil() {
		Usuario usuario = usuario(10L, TipoUsuario.ADMIN_CLINICA, "S");
		Clinica clinica = clinicaAtiva();
		usuario.setClinica(clinica);
		usuario.setSenhaHash("$2a$10$hashOriginal");
		usuario.setTrocaSenha("N");
		Veterinario veterinario = veterinarioAtivo();
		when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
		when(usuarioRepository.existsByLoginAndIdNot("vera@arkive.com", 10L)).thenReturn(false);
		when(usuarioRepository.existsByVeterinarioIdAndIdNot(1L, 10L)).thenReturn(false);
		when(veterinarioRepository.findById(1L)).thenReturn(Optional.of(veterinario));
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

		UsuarioResponse response = usuarioService.atualizar(10L, new UsuarioEditRequest(
				"Dra Vera",
				TipoUsuario.VETERINARIO,
				"  vera@arkive.com  ",
				null,
				1L,
				null
		), 99L);

		Assertions.assertEquals("Dra Vera", response.nome());
		Assertions.assertEquals("vera@arkive.com", usuario.getLogin());
		Assertions.assertEquals(TipoUsuario.VETERINARIO, usuario.getTipo());
		Assertions.assertEquals("$2a$10$hashOriginal", usuario.getSenhaHash());
		Assertions.assertEquals("N", usuario.getTrocaSenha());
		Assertions.assertNull(usuario.getClinica());
		Assertions.assertNull(usuario.getResponsavel());
		Assertions.assertSame(veterinario, usuario.getVeterinario());
		verify(passwordEncoder, never()).encode(any());
	}

	@Test
	void editarRejeitaLoginDuplicado() {
		Usuario usuario = usuario(10L, TipoUsuario.SYSADMIN, "S");
		when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
		when(usuarioRepository.existsByLoginAndIdNot("duplicado@arkive.com", 10L)).thenReturn(true);

		assertThrows(BusinessException.class, () -> usuarioService.atualizar(10L, new UsuarioEditRequest(
				"Ana",
				TipoUsuario.SYSADMIN,
				"duplicado@arkive.com",
				null,
				null,
				null
		), 99L));
		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void editarRejeitaVinculosJaUsados() {
		Usuario usuario = usuario(10L, TipoUsuario.SYSADMIN, "S");
		when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
		when(usuarioRepository.existsByLoginAndIdNot("vera@arkive.com", 10L)).thenReturn(false);
		when(usuarioRepository.existsByVeterinarioIdAndIdNot(1L, 10L)).thenReturn(true);

		assertThrows(BusinessException.class, () -> usuarioService.atualizar(10L, new UsuarioEditRequest(
				"Dra Vera",
				TipoUsuario.VETERINARIO,
				"vera@arkive.com",
				null,
				1L,
				null
		), 99L));
		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void criarRejeitaVeterinarioJaAssociado() {
		when(usuarioRepository.existsByVeterinarioId(1L)).thenReturn(true);

		assertThrows(BusinessException.class, () -> usuarioService.criar(request(TipoUsuario.VETERINARIO, null, 1L, null)));
		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void sysadminAtualNaoPodeDemoverProprioPerfil() {
		Usuario usuario = usuario(10L, TipoUsuario.SYSADMIN, "S");
		when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));

		assertThrows(BusinessException.class, () -> usuarioService.atualizar(10L, new UsuarioEditRequest(
				"Ana",
				TipoUsuario.ADMIN_CLINICA,
				"ana@arkive.com",
				null,
				null,
				1L
		), 10L));
		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void ultimoSysadminAtivoNaoPodeSerDemovido() {
		Usuario usuario = usuario(10L, TipoUsuario.SYSADMIN, "S");
		when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
		when(usuarioRepository.countByTipoAndAtivo(TipoUsuario.SYSADMIN, "S")).thenReturn(1L);

		assertThrows(BusinessException.class, () -> usuarioService.atualizar(10L, new UsuarioEditRequest(
				"Ana",
				TipoUsuario.ADMIN_CLINICA,
				"ana@arkive.com",
				null,
				null,
				1L
		), 99L));
		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void outroSysadminPodeSerDemovidoQuandoExisteOutroAtivo() {
		Usuario usuario = usuario(10L, TipoUsuario.SYSADMIN, "S");
		Clinica clinica = clinicaAtiva();
		when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
		when(usuarioRepository.countByTipoAndAtivo(TipoUsuario.SYSADMIN, "S")).thenReturn(2L);
		when(usuarioRepository.existsByLoginAndIdNot("admin@arkive.com", 10L)).thenReturn(false);
		when(clinicaRepository.findById(1L)).thenReturn(Optional.of(clinica));
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

		usuarioService.atualizar(10L, new UsuarioEditRequest(
				"Admin Clinica",
				TipoUsuario.ADMIN_CLINICA,
				"admin@arkive.com",
				null,
				null,
				1L
		), 99L);

		Assertions.assertEquals(TipoUsuario.ADMIN_CLINICA, usuario.getTipo());
		Assertions.assertSame(clinica, usuario.getClinica());
		verify(usuarioRepository).save(usuario);
	}

	@Test
	void listaVeterinariosDisponiveisParaCriacaoAPartirDaConsultaFiltrada() {
		Veterinario veterinario = veterinarioAtivo();
		veterinario.setId(1L);
		veterinario.setNome("Dra Livre");
		when(veterinarioRepository.findAtivosSemUsuarioOrderByNomeAsc()).thenReturn(List.of(veterinario));

		var opcoes = usuarioService.listarVeterinariosDisponiveisParaCriacao();

		Assertions.assertEquals(1, opcoes.size());
		Assertions.assertEquals(1L, opcoes.get(0).id());
		Assertions.assertEquals("Dra Livre", opcoes.get(0).label());
		verify(veterinarioRepository).findAtivosSemUsuarioOrderByNomeAsc();
	}

	@Test
	void listaVeterinariosDisponiveisParaEdicaoIncluindoVinculoAtual() {
		Veterinario veterinario = veterinarioAtivo();
		veterinario.setId(1L);
		veterinario.setNome("Dra Atual");
		when(veterinarioRepository.findDisponiveisParaUsuarioOrderByNomeAsc(10L)).thenReturn(List.of(veterinario));

		var opcoes = usuarioService.listarVeterinariosDisponiveisParaEdicao(10L);

		Assertions.assertEquals("Dra Atual", opcoes.get(0).label());
		verify(veterinarioRepository).findDisponiveisParaUsuarioOrderByNomeAsc(10L);
	}

	@Test
	void listaResponsaveisDisponiveisParaCriacaoAPartirDaConsultaFiltrada() {
		Responsavel responsavel = responsavelAtivo();
		responsavel.setId(2L);
		responsavel.setNome("Rui Livre");
		when(responsavelRepository.findAtivosSemUsuarioOrderByNomeAsc()).thenReturn(List.of(responsavel));

		var opcoes = usuarioService.listarResponsaveisDisponiveisParaCriacao();

		Assertions.assertEquals(1, opcoes.size());
		Assertions.assertEquals(2L, opcoes.get(0).id());
		Assertions.assertEquals("Rui Livre", opcoes.get(0).label());
		verify(responsavelRepository).findAtivosSemUsuarioOrderByNomeAsc();
	}

	@Test
	void listaResponsaveisDisponiveisParaEdicaoIncluindoVinculoAtual() {
		Responsavel responsavel = responsavelAtivo();
		responsavel.setId(2L);
		responsavel.setNome("Rui Atual");
		when(responsavelRepository.findDisponiveisParaUsuarioOrderByNomeAsc(10L)).thenReturn(List.of(responsavel));

		var opcoes = usuarioService.listarResponsaveisDisponiveisParaEdicao(10L);

		Assertions.assertEquals("Rui Atual", opcoes.get(0).label());
		verify(responsavelRepository).findDisponiveisParaUsuarioOrderByNomeAsc(10L);
	}

	private UsuarioRequest request(TipoUsuario tipo, Long responsavelId, Long veterinarioId, Long clinicaId) {
		return new UsuarioRequest(
				"Usuario Teste",
				tipo,
				"usuario@arkive.com",
				"senha-segura",
				responsavelId,
				veterinarioId,
				clinicaId,
				"S"
		);
	}

	private Usuario usuario(Long id, TipoUsuario tipo, String ativo) {
		Usuario usuario = new Usuario();
		usuario.setId(id);
		usuario.setTipo(tipo);
		usuario.setAtivo(ativo);
		usuario.setLogin("usuario@arkive.com");
		usuario.setNome("Usuario");
		return usuario;
	}

	private Responsavel responsavelAtivo() {
		Responsavel responsavel = new Responsavel();
		responsavel.setAtivo("S");
		return responsavel;
	}

	private Veterinario veterinarioAtivo() {
		Veterinario veterinario = new Veterinario();
		veterinario.setAtivo("S");
		return veterinario;
	}

	private Clinica clinicaAtiva() {
		Clinica clinica = new Clinica();
		clinica.setAtivo("S");
		return clinica;
	}

}
