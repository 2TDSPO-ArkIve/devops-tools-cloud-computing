package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.config.SecurityConfig;
import br.com.fiap.arkive.dto.response.AnimalResponse;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.exception.GlobalExceptionHandler;
import br.com.fiap.arkive.security.ArkiveUserDetailsService;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.AnimalService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AnimalController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class AnimalControllerMvcTest {

	private final MockMvc mockMvc;

	@MockitoBean
	private AnimalService animalService;

	@MockitoBean
	private ArkiveUserDetailsService arkiveUserDetailsService;

	@Autowired
	AnimalControllerMvcTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void listaPacientesDaClinicaDoVeterinarioComFiltros() throws Exception {
		when(animalService.listarPacientesClinicaVeterinario(eq("Bilu"), eq(1L), eq(2L), any(Pageable.class), any(UsuarioPrincipal.class)))
				.thenReturn(new PageImpl<>(List.of(new AnimalResponse(
						50L,
						"Bilu",
						1L,
						"Cachorro",
						2L,
						"Poodle",
						"M",
						"S",
						30L,
						"Clinica ArkIve",
						"S"
				))));
		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

		mockMvc.perform(get("/api/animais/clinica")
						.with(user(veterinario()))
						.accept(MediaType.APPLICATION_JSON)
						.param("nome", "Bilu")
						.param("especieId", "1")
						.param("racaId", "2")
						.param("page", "0")
						.param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(50))
				.andExpect(jsonPath("$.content[0].nome").value("Bilu"))
				.andExpect(jsonPath("$.content[0].clinicaId").value(30));

		verify(animalService).listarPacientesClinicaVeterinario(eq("Bilu"), eq(1L), eq(2L), pageableCaptor.capture(), any(UsuarioPrincipal.class));
		assertEquals(0, pageableCaptor.getValue().getPageNumber());
		assertEquals(10, pageableCaptor.getValue().getPageSize());
	}

	@Test
	void veterinarioNaoDesativaAnimalPelaApi() throws Exception {
		org.mockito.Mockito.doThrow(new AccessDeniedException("Operacao permitida apenas a SYSADMIN ou ADMIN_CLINICA."))
				.when(animalService).excluir(eq(50L), any(UsuarioPrincipal.class));

		mockMvc.perform(delete("/api/animais/50").with(user(veterinario())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.message").value("Operacao permitida apenas a SYSADMIN ou ADMIN_CLINICA."));
	}

	private UsuarioPrincipal veterinario() {
		return new UsuarioPrincipal(1L, "Dra Vera", "vera@arkive.com", "$2a$10$hash", TipoUsuario.VETERINARIO, "S", false, null, 10L, null);
	}

	@Test
	void meUsaPrincipalAutenticadoEIgnoraTentativaDeTrocarEscopo() throws Exception {
		var principal = veterinario();
		when(animalService.listarPacientesVeterinario(eq("Bilu"), eq(1L), eq(2L), any(Pageable.class), any()))
				.thenReturn(new PageImpl<>(List.of(new AnimalResponse(50L, "Bilu", 1L, "Cachorro",
						2L, "Poodle", "M", "S", null, null, "S"))));
		mockMvc.perform(get("/api/animais/me").with(user(principal))
						.param("nome", "Bilu").param("especieId", "1").param("racaId", "2")
						.param("veterinarioId", "999").param("clinicaId", "999").param("ativo", "N"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(50));
		verify(animalService).listarPacientesVeterinario(eq("Bilu"), eq(1L), eq(2L), any(Pageable.class),
				org.mockito.Mockito.same(principal));
	}
}
