package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.config.SecurityConfig;
import br.com.fiap.arkive.dto.response.TranscricaoResponse;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.exception.GlobalExceptionHandler;
import br.com.fiap.arkive.security.ArkiveUserDetailsService;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.TranscricaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TranscricaoController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class TranscricaoControllerMvcTest {

	private final MockMvc mockMvc;

	@MockitoBean
	private TranscricaoService transcricaoService;

	@MockitoBean
	private ArkiveUserDetailsService arkiveUserDetailsService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	TranscricaoControllerMvcTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void apiProtegidaAnonimaRetorna401Json() throws Exception {
		mockMvc.perform(multipart("/api/transcricoes")
						.file(audio())
						.param("idioma", "pt-BR")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized())
				.andExpect(header().doesNotExist("Location"))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.message").value("Autenticacao obrigatoria."))
				.andExpect(jsonPath("$.path").value("/api/transcricoes"));
	}

	@Test
	void apiProtegidaComBasicInvalidoRetorna401SemRedirect() throws Exception {
		when(arkiveUserDetailsService.loadUserByUsername("vet.invalido@arkive.com"))
				.thenThrow(new UsernameNotFoundException("Usuario nao encontrado"));

		mockMvc.perform(multipart("/api/transcricoes")
						.file(audio())
						.param("idioma", "pt-BR")
						.with(httpBasic("vet.invalido@arkive.com", "senha-invalida")))
				.andExpect(status().isUnauthorized())
				.andExpect(header().doesNotExist("Location"))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.message").value("Autenticacao obrigatoria."));

		verify(transcricaoService, never()).transcrever(any(), any(), any());
	}

	@Test
	void veterinarioAutenticadoViaBasicChegaAoController() throws Exception {
		when(arkiveUserDetailsService.loadUserByUsername("vera@arkive.com"))
				.thenReturn(veterinario(passwordEncoder.encode("SenhaTeste1")));
		when(transcricaoService.transcrever(any(), eq("pt-BR"), any()))
				.thenReturn(new TranscricaoResponse("Paciente canino.", "pt-BR"));

		mockMvc.perform(multipart("/api/transcricoes")
						.file(audio())
						.param("idioma", "pt-BR")
						.with(httpBasic("vera@arkive.com", "SenhaTeste1")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.transcricao").value("Paciente canino."))
				.andExpect(jsonPath("$.idioma").value("pt-BR"));

		verify(transcricaoService).transcrever(any(), eq("pt-BR"), any(UsuarioPrincipal.class));
	}

	@Test
	void perfilNaoVeterinarioRecebe403Json() throws Exception {
		when(transcricaoService.transcrever(any(), eq("pt-BR"), any()))
				.thenThrow(new AccessDeniedException("Transcricao permitida apenas ao veterinario autenticado."));

		mockMvc.perform(multipart("/api/transcricoes")
						.file(audio())
						.param("idioma", "pt-BR")
						.with(user(responsavel())))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist("Location"))
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.message").value("Transcricao permitida apenas ao veterinario autenticado."));
	}

	private MockMultipartFile audio() {
		return new MockMultipartFile("audio", "consulta.wav", "audio/wav", new byte[] { 'R', 'I', 'F', 'F', 36, 0, 0, 0, 'W', 'A', 'V', 'E' });
	}

	private UsuarioPrincipal veterinario() {
		return new UsuarioPrincipal(1L, "Dra Vera", "vera@arkive.com", "$2a$10$hash", TipoUsuario.VETERINARIO, "S", false, null, 10L, null);
	}

	private UsuarioPrincipal veterinario(String senhaHash) {
		return new UsuarioPrincipal(1L, "Dra Vera", "vera@arkive.com", senhaHash, TipoUsuario.VETERINARIO, "S", false, null, 10L, null);
	}

	private UsuarioPrincipal responsavel() {
		return new UsuarioPrincipal(2L, "Joao", "joao@arkive.com", "$2a$10$hash", TipoUsuario.RESPONSAVEL, "S", false, 20L, null, null);
	}
}
