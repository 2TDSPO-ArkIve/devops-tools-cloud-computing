package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.config.SecurityConfig;
import br.com.fiap.arkive.dto.request.*;
import br.com.fiap.arkive.dto.response.*;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.exception.GlobalExceptionHandler;
import br.com.fiap.arkive.security.ArkiveUserDetailsService;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({RacaController.class, ResponsavelController.class, AnimalResponsavelController.class, AnimalController.class, ConsultaController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class Batch2ApiMvcTest {
	@Autowired MockMvc mvc;
	@MockitoBean ArkiveUserDetailsService usuarios;
	@MockitoBean RacaService racas;
	@MockitoBean ResponsavelService responsaveis;
	@MockitoBean AnimalResponsavelService vinculos;
	@MockitoBean AnimalService animais;
	@MockitoBean ConsultaService consultas;

	@ParameterizedTest @ValueSource(strings = {"VETERINARIO", "SYSADMIN", "ADMIN_CLINICA"})
	void permiteCriarRaca(String role) throws Exception {
		mvc.perform(post("/api/racas").with(user("user").roles(role)).contentType("application/json")
				.content("{\"nome\":\"Bulldog\",\"especieId\":1}"))
				.andExpect(status().isCreated());
		verify(racas).criar(new RacaRequest("Bulldog", 1L, null));
	}
	@Test void tutorNaoCriaRacaEVetNaoAdministraCatalogo() throws Exception {
		mvc.perform(post("/api/racas").with(user("tutor").roles("RESPONSAVEL")).contentType("application/json").content("{\"nome\":\"Raca\",\"especieId\":1}")).andExpect(status().isForbidden());
		mvc.perform(put("/api/racas/1").with(user("vet").roles("VETERINARIO")).contentType("application/json").content("{\"nome\":\"Raca\",\"especieId\":1}")).andExpect(status().isForbidden());
		mvc.perform(delete("/api/racas/1").with(user("vet").roles("VETERINARIO"))).andExpect(status().isForbidden());
		verifyNoInteractions(racas);
	}
	@ParameterizedTest @ValueSource(strings = {"SYSADMIN", "ADMIN_CLINICA"})
	void administradoresMantemPutDeleteRaca(String role) throws Exception {
		mvc.perform(put("/api/racas/1").with(user("admin").roles(role)).contentType("application/json").content("{\"nome\":\"Raca\",\"especieId\":1}")).andExpect(status().isOk());
		mvc.perform(delete("/api/racas/1").with(user("admin").roles(role))).andExpect(status().isNoContent());
	}
	@Test void apiEncaminhaFiltroDeEspecie() throws Exception {
		when(racas.listar(isNull(), eq(1L), any())).thenReturn(new PageImpl<>(List.of(new RacaResponse(2L, "Bulldog", null, 1L, "Cachorro", "S"))));
		mvc.perform(get("/api/racas?especieId=1").with(user("vet").roles("VETERINARIO")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].especieId").value(1));
	}
	@Test void racaExigeEspecieENome() throws Exception {
		mvc.perform(post("/api/racas").with(user("vet").roles("VETERINARIO")).contentType("application/json").content("{\"nome\":\" \"}"))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(racas);
	}
	@Test void buscaTutorMinimaSemDocumentoOuTelefone() throws Exception {
		when(responsaveis.buscarParaVinculo(eq("Ana"), any())).thenReturn(new PageImpl<>(List.of(new ResponsavelLookupResponse(20L, "Ana", "ana@example.test"))));
		mvc.perform(get("/api/responsaveis/busca?busca=Ana").with(user("vet").roles("VETERINARIO")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(20))
				.andExpect(jsonPath("$.content[0].documento").doesNotExist()).andExpect(jsonPath("$.content[0].telefone").doesNotExist());
		mvc.perform(get("/api/responsaveis").with(user("vet").roles("VETERINARIO"))).andExpect(status().isForbidden());
		mvc.perform(get("/api/responsaveis/20").with(user("vet").roles("VETERINARIO"))).andExpect(status().isForbidden());
		mvc.perform(post("/api/responsaveis").with(user("vet").roles("VETERINARIO"))).andExpect(status().isForbidden());
	}
	@Test void responsavelConsultaSomenteSeuCadastro() throws Exception {
		var tutor = principal(TipoUsuario.RESPONSAVEL);
		when(responsaveis.buscarPorId(20L)).thenReturn(new ResponsavelResponse(20L, "Ana", "documento", null, null, "TUTOR", LocalDate.now(), "S", "S"));
		mvc.perform(get("/api/responsaveis/20").with(user(tutor))).andExpect(status().isOk());
		mvc.perform(get("/api/responsaveis/21").with(user(tutor))).andExpect(status().isForbidden());
		mvc.perform(get("/api/responsaveis").with(user(tutor))).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1));
	}
	@Test void vinculosRecebemPrincipalAutenticado() throws Exception {
		var vet = principal(TipoUsuario.VETERINARIO);
		mvc.perform(get("/api/animais-responsaveis/animal/1").with(user(vet))).andExpect(status().isOk());
		verify(vinculos).listarAtivosPorAnimal(1L, vet);
		mvc.perform(post("/api/animais-responsaveis").with(user(vet)).contentType("application/json")
				.content("{\"animalId\":1,\"responsavelId\":20,\"tipoVinculo\":\"TUTOR_LEGAL\",\"principal\":\"S\"}"))
				.andExpect(status().isCreated());
		verify(vinculos).criar(any(AnimalResponsavelRequest.class), eq(vet));
	}
	@Test void nascimentoAceitoERetornadoComoDataIso() throws Exception {
		var vet = principal(TipoUsuario.VETERINARIO);
		when(animais.criar(any(), eq(vet))).thenReturn(new AnimalResponse(1L, "Nina", 1L, "Cachorro", null, null, null, "N", null, null, "S", LocalDate.parse("2021-04-17")));
		mvc.perform(post("/api/animais").with(user(vet)).contentType("application/json")
				.content("{\"nome\":\"Nina\",\"especieId\":1,\"dataNascimento\":\"2021-04-17\"}"))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.dataNascimento").value("2021-04-17"));
		verify(animais).criar(argThat(r -> LocalDate.parse("2021-04-17").equals(r.dataNascimento())), eq(vet));
	}
	@Test void enderecoMaiorQue255EhRejeitado() throws Exception {
		mvc.perform(post("/api/consultas").with(user(principal(TipoUsuario.VETERINARIO))).contentType("application/json")
				.content("{\"animalId\":1,\"dataHora\":\"2099-01-01T10:00:00\",\"modalidade\":\"PRESENCIAL\",\"motivo\":\"Retorno\",\"endereco\":\"" + "x".repeat(256) + "\"}"))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(consultas);
	}
	private UsuarioPrincipal principal(TipoUsuario tipo) {
		return new UsuarioPrincipal(1L, "Usuario", "user@example.test", "hash", tipo, "S", false,
				tipo == TipoUsuario.RESPONSAVEL ? 20L : null, tipo == TipoUsuario.VETERINARIO ? 10L : null, null);
	}
}
