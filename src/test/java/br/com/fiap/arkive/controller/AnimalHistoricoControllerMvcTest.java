package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.config.SecurityConfig;
import br.com.fiap.arkive.entity.*;
import br.com.fiap.arkive.exception.GlobalExceptionHandler;
import br.com.fiap.arkive.repository.*;
import br.com.fiap.arkive.security.ArkiveUserDetailsService;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnimalHistoricoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, AnimalHistoricoService.class,
		AnimalHistoricoPdfService.class, ClinicalAccessService.class})
class AnimalHistoricoControllerMvcTest {
	@Autowired MockMvc mvc;
	@Autowired ClinicalAccessService acesso;
	@MockitoBean ArkiveUserDetailsService usuarios;
	@MockitoBean AnimalRepository animais;
	@MockitoBean ConsultaRepository consultas;
	@MockitoBean DiagnosticoRepository diagnosticos;
	@MockitoBean PrescricaoRepository prescricoes;
	@MockitoBean AnimalResponsavelRepository responsaveis;
	@MockitoBean VeterinarioService veterinarios;
	private Animal animal;
	private Consulta consulta;

	@BeforeEach
	void preparar() {
		Especie especie = new Especie(); especie.setNome("Cachorro");
		Raca raca = new Raca(); raca.setNome("Poodle");
		Veterinario vet = new Veterinario(); vet.setId(10L); vet.setNome("Dra. Vera"); vet.setCrmv("SP12345");
		Clinica clinica = new Clinica(); clinica.setId(30L); clinica.setNome("Clinica ArkIve");
		animal = new Animal(); animal.setId(50L); animal.setNome("Bilu"); animal.setEspecie(especie);
		animal.setRaca(raca); animal.setSexo("M"); animal.setCastrado("S"); animal.setVeterinarioCadastro(vet);
		animal.setClinica(clinica); animal.setDataNascimento(LocalDate.parse("2021-04-17"));
		consulta = new Consulta(); consulta.setId(63L); consulta.setAnimal(animal); consulta.setVeterinario(vet);
		consulta.setClinica(clinica); consulta.setDataHora(LocalDateTime.parse("2026-09-05T14:30:00"));
		consulta.setModalidade("PRESENCIAL"); consulta.setEndereco("Rua do Atendimento, 10");
		consulta.setMotivo("Retorno para acompanhamento"); consulta.setStatus("FI");
		consulta.setObservacao("Retorno em sete dias."); consulta.setTranscricao("SECRET_TRANSCRICAO");
		consulta.setSintomas("SECRET_NARRATIVA");
		Diagnostico diagnostico = new Diagnostico(); diagnostico.setDiagnostico("Dermatite confirmada");
		diagnostico.setConfirmado("S"); diagnostico.setValidacaoVet("S"); diagnostico.setSeveridade("LEVE");
		diagnostico.setInsightIa("SECRET_AI_INSIGHT"); diagnostico.setFontesIaJson("SECRET_AI_SOURCE");
		diagnostico.setConfianca(new BigDecimal("0.876543"));
		Prescricao prescricao = new Prescricao(); prescricao.setMedicamento("Medicamento registrado");
		prescricao.setDosagem("5 mg"); prescricao.setFrequencia("12 horas"); prescricao.setViaAdministracao("ORAL");
		prescricao.setDataInicio(LocalDate.parse("2026-09-05")); prescricao.setDataFim(LocalDate.parse("2026-09-12"));
		prescricao.setInstrucoes("Administrar com alimento.");
		Responsavel tutor = new Responsavel(); tutor.setNome("Tutora Ana"); tutor.setDocumento("SECRET_CPF");
		tutor.setEmail("SECRET_EMAIL");
		AnimalResponsavel vinculo = new AnimalResponsavel(); vinculo.setResponsavel(tutor);
		when(animais.findById(50L)).thenReturn(Optional.of(animal));
		when(consultas.buscarHistoricoPorAnimal(50L)).thenReturn(List.of(consulta));
		when(diagnosticos.buscarDiagnosticosConfirmadosVeterinario(eq(63L), any())).thenReturn(List.of(diagnostico));
		when(prescricoes.buscarPorConsulta(63L)).thenReturn(List.of(prescricao));
		when(responsaveis.buscarResponsaveisPrincipaisAtivosVigentes(eq(50L), any())).thenReturn(List.of(vinculo));
	}

	@Test
	void jsonPublicaApenasCamposPermitidos() throws Exception {
		mvc.perform(get("/api/animais/50/historico").with(user(principal(TipoUsuario.VETERINARIO))))
				.andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(jsonPath("$.paciente.id").value(50)).andExpect(jsonPath("$.paciente.nome").value("Bilu"))
				.andExpect(jsonPath("$.paciente.especie").value("Cachorro")).andExpect(jsonPath("$.paciente.raca").value("Poodle"))
				.andExpect(jsonPath("$.paciente.sexo").value("M")).andExpect(jsonPath("$.paciente.castrado").value("S"))
				.andExpect(jsonPath("$.paciente.dataNascimento").value("2021-04-17"))
				.andExpect(jsonPath("$.paciente.responsavelNome").value("Tutora Ana"))
				.andExpect(jsonPath("$.consultas[0].dataHora").value("2026-09-05T14:30:00"))
				.andExpect(jsonPath("$.consultas[0].modalidade").value("PRESENCIAL"))
				.andExpect(jsonPath("$.consultas[0].endereco").value("Rua do Atendimento, 10"))
				.andExpect(jsonPath("$.consultas[0].veterinarioNome").value("Dra. Vera"))
				.andExpect(jsonPath("$.consultas[0].crmv").value("SP12345"))
				.andExpect(jsonPath("$.consultas[0].clinicaNome").value("Clinica ArkIve"))
				.andExpect(jsonPath("$.consultas[0].diagnosticoConfirmado").value("Dermatite confirmada"))
				.andExpect(jsonPath("$.consultas[0].severidadeFinal").value("LEVE"))
				.andExpect(jsonPath("$.consultas[0].conclusao").value("Retorno em sete dias."))
				.andExpect(jsonPath("$.consultas[0].prescricoes[0].medicamento").value("Medicamento registrado"))
				.andExpect(jsonPath("$.consultas[0].prescricoes[0].dosagem").value("5 mg"))
				.andExpect(jsonPath("$.consultas[0].prescricoes[0].frequencia").value("12 horas"))
				.andExpect(jsonPath("$.consultas[0].prescricoes[0].viaAdministracao").value("ORAL"))
				.andExpect(jsonPath("$.consultas[0].prescricoes[0].dataInicio").value("2026-09-05"))
				.andExpect(jsonPath("$.consultas[0].prescricoes[0].dataFim").value("2026-09-12"))
				.andExpect(jsonPath("$.consultas[0].prescricoes[0].instrucoes").value("Administrar com alimento."))
				.andExpect(jsonPath("$.consultas[0].id").doesNotExist())
				.andExpect(content().string(not(containsString("SECRET"))))
				.andExpect(content().string(not(containsString("confianca"))))
				.andExpect(content().string(not(containsString("validacaoVet"))));
	}

	@Test
	void jsonSemConsultasSemTutorSemNascimento() throws Exception {
		animal.setDataNascimento(null);
		when(consultas.buscarHistoricoPorAnimal(50L)).thenReturn(List.of());
		when(responsaveis.buscarResponsaveisPrincipaisAtivosVigentes(eq(50L), any())).thenReturn(List.of());
		mvc.perform(get("/api/animais/50/historico").with(user(principal(TipoUsuario.VETERINARIO))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.paciente.nome").value("Bilu"))
				.andExpect(jsonPath("$.paciente.dataNascimento").isEmpty())
				.andExpect(jsonPath("$.paciente.responsavelNome").isEmpty())
				.andExpect(jsonPath("$.consultas").isEmpty());
		verifyNoInteractions(diagnosticos, prescricoes);
	}

	@Test
	void pdfCompletoSemVazamentoComMesmoCabecalhoDoResumo() throws Exception {
		byte[] bytes = pdf();
		try (var document = Loader.loadPDF(bytes)) {
			String text = new PDFTextStripper().getText(document);
			for (String esperado : List.of("ArkIve", "Bilu", "17/04/2021", "05/09/2026 14:30", "Tutora Ana",
					"Dermatite confirmada", "Retorno em sete dias.", "Medicamento registrado", "5 mg", "12 horas",
					"ORAL", "05/09/2026", "12/09/2026", "Administrar com alimento.", "Rua do Atendimento, 10")) {
				assertTrue(text.contains(esperado), esperado);
			}
			for (String proibido : List.of("SECRET", "0.876543", "validacaoVet", "consultaId", "animalId")) {
				assertFalse(text.contains(proibido), proibido);
			}
			var consultaService = mock(ConsultaService.class);
			when(consultaService.buscarEntidade(63L)).thenReturn(consulta);
			byte[] resumo = new ConsultaResumoPdfService(consultaService, acesso, diagnosticos, prescricoes, responsaveis)
					.gerarResumo(63L, principal(TipoUsuario.VETERINARIO)).bytes();
			try (var summary = Loader.loadPDF(resumo)) {
				var first = new org.apache.pdfbox.rendering.PDFRenderer(document).renderImageWithDPI(0, 144);
				var second = new org.apache.pdfbox.rendering.PDFRenderer(summary).renderImageWithDPI(0, 144);
				for (int y = 100; y < 180; y++) {
					for (int x = 100; x < 500; x++) assertEquals(first.getRGB(x, y), second.getRGB(x, y));
				}
				assertEquals(0xFFFFFF, first.getRGB(109, 109) & 0xFFFFFF, "No background tile at logo corner");
				if (Boolean.getBoolean("arkive.pdf.visual-check")) {
					Path dir = Path.of("target", "pdf-review"); Files.createDirectories(dir);
					Files.write(dir.resolve("historico.pdf"), bytes); Files.write(dir.resolve("resumo.pdf"), resumo);
					javax.imageio.ImageIO.write(first, "png", dir.resolve("historico.png").toFile());
					javax.imageio.ImageIO.write(second, "png", dir.resolve("resumo.png").toFile());
				}
			}
		}
	}

	@Test
	void pdfSemConsultasContinuaValido() throws Exception {
		when(consultas.buscarHistoricoPorAnimal(50L)).thenReturn(List.of());
		try (var document = Loader.loadPDF(pdf())) {
			String text = new PDFTextStripper().getText(document);
			assertEquals(1, document.getNumberOfPages());
			assertTrue(text.contains("Bilu")); assertTrue(text.contains("Nenhuma consulta"));
		}
	}

	@Test
	void pdfLongoPreservaFimDosTextosEConsultasPosteriores() throws Exception {
		consulta.setObservacao("Acompanhamento semanal e hidrata\u00e7\u00e3o. ".repeat(500) + "FIM_CONCLUSAO");
		Consulta antiga = outraConsulta(); antiga.setDataHora(consulta.getDataHora().minusYears(1));
		antiga.setMotivo("ULTIMA_CONSULTA"); antiga.setVeterinario(consulta.getVeterinario());
		when(consultas.buscarHistoricoPorAnimal(50L)).thenReturn(List.of(consulta, antiga));
		try (var document = Loader.loadPDF(pdf())) {
			String text = new PDFTextStripper().getText(document);
			assertTrue(document.getNumberOfPages() > 2);
			assertTrue(text.contains("FIM_CONCLUSAO"));
			assertTrue(text.indexOf("ULTIMA_CONSULTA") > text.indexOf("FIM_CONCLUSAO"));
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"AG", "EP", "AP", "CA"})
	void consultasNaoFinalizadasNaoPublicamRascunhos(String statusConsulta) throws Exception {
		consulta.setStatus(statusConsulta); consulta.setObservacao("SECRET_RASCUNHO");
		mvc.perform(get("/api/animais/50/historico").with(user(principal(TipoUsuario.VETERINARIO))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.consultas[0].status").value(statusConsulta))
				.andExpect(jsonPath("$.consultas[0].diagnosticoConfirmado").isEmpty())
				.andExpect(jsonPath("$.consultas[0].severidadeFinal").isEmpty())
				.andExpect(jsonPath("$.consultas[0].conclusao").isEmpty())
				.andExpect(jsonPath("$.consultas[0].prescricoes").isEmpty());
		try (var document = Loader.loadPDF(pdf())) {
			assertFalse(new PDFTextStripper().getText(document).contains("SECRET"));
		}
		verifyNoInteractions(diagnosticos, prescricoes);
	}

	@Test
	void consultaFinalizadaLegadaSemDiagnosticoNaoImpedeHistorico() throws Exception {
		when(diagnosticos.buscarDiagnosticosConfirmadosVeterinario(eq(63L), any())).thenReturn(List.of());
		mvc.perform(get("/api/animais/50/historico").with(user(principal(TipoUsuario.VETERINARIO))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.consultas[0].diagnosticoConfirmado").isEmpty());
		assertTrue(pdf().length > 0);
	}

	@Test
	void remotaSemClinicaNaoExibeEndereco() throws Exception {
		consulta.setModalidade("REMOTA"); consulta.setClinica(null);
		mvc.perform(get("/api/animais/50/historico").with(user(principal(TipoUsuario.VETERINARIO))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.consultas[0].endereco").isEmpty())
				.andExpect(jsonPath("$.consultas[0].clinicaNome").isEmpty());
		try (var document = Loader.loadPDF(pdf())) {
			assertFalse(new PDFTextStripper().getText(document).contains("Rua do Atendimento"));
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"historico", "historico-pdf"})
	void animalInexistenteRetorna404(String endpoint) throws Exception {
		mvc.perform(get("/api/animais/999/" + endpoint).with(user(principal(TipoUsuario.VETERINARIO))))
				.andExpect(status().isNotFound());
		verifyNoInteractions(consultas, diagnosticos, prescricoes, responsaveis);
	}

	@ParameterizedTest
	@ValueSource(strings = {"historico", "historico-pdf"})
	void acessoNaoAutenticadoRetorna401(String endpoint) throws Exception {
		mvc.perform(get("/api/animais/50/" + endpoint).accept("application/json"))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(animais, consultas, diagnosticos, prescricoes, responsaveis);
	}

	@ParameterizedTest
	@ValueSource(strings = {"historico", "historico-pdf"})
	void veterinarioSemAcessoAoPacienteRetorna403(String endpoint) throws Exception {
		animal.setVeterinarioCadastro(null);
		mvc.perform(get("/api/animais/50/" + endpoint).with(user(principal(TipoUsuario.VETERINARIO))))
				.andExpect(status().isForbidden());
		verify(consultas, never()).buscarHistoricoPorAnimal(any());
		verifyNoInteractions(diagnosticos, prescricoes, responsaveis);
	}

	@Test
	void acessoAoPacienteNaoExpoeConsultaDeOutroVeterinario() throws Exception {
		when(consultas.buscarHistoricoPorAnimal(50L)).thenReturn(List.of(consulta, outraConsulta()));
		mvc.perform(get("/api/animais/50/historico").with(user(principal(TipoUsuario.VETERINARIO))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.consultas.length()").value(1))
				.andExpect(content().string(not(containsString("SECRET_OUTRA_CONSULTA"))));
		try (var document = Loader.loadPDF(pdf())) {
			assertFalse(new PDFTextStripper().getText(document).contains("SECRET_OUTRA_CONSULTA"));
		}
		verify(diagnosticos, never()).buscarDiagnosticosConfirmadosVeterinario(eq(64L), any());
		verify(prescricoes, never()).buscarPorConsulta(64L);
	}

	@Test
	void pacienteAcessivelComTodasConsultasOcultasRetornaListaVazia() throws Exception {
		when(consultas.buscarHistoricoPorAnimal(50L)).thenReturn(List.of(outraConsulta()));
		mvc.perform(get("/api/animais/50/historico").with(user(principal(TipoUsuario.VETERINARIO))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.consultas").isEmpty());
		try (var document = Loader.loadPDF(pdf())) {
			assertTrue(new PDFTextStripper().getText(document).contains("Nenhuma consulta"));
		}
	}

	@Test
	void adminClinicaSoRecebeConsultasDaSuaClinica() throws Exception {
		Consulta outra = outraConsulta(); outra.setClinica(null);
		when(consultas.buscarHistoricoPorAnimal(50L)).thenReturn(List.of(consulta, outra));
		mvc.perform(get("/api/animais/50/historico").with(user(principal(TipoUsuario.ADMIN_CLINICA))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.consultas.length()").value(1));
	}

	@Test
	void sysadminRecebeTodasConsultasDoPaciente() throws Exception {
		when(consultas.buscarHistoricoPorAnimal(50L)).thenReturn(List.of(consulta, outraConsulta()));
		mvc.perform(get("/api/animais/50/historico").with(user(principal(TipoUsuario.SYSADMIN))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.consultas.length()").value(2));
	}

	@ParameterizedTest
	@ValueSource(strings = {"historico", "historico-pdf"})
	void tutorPrecisaDeVinculoAtual(String endpoint) throws Exception {
		mvc.perform(get("/api/animais/50/" + endpoint).with(user(principal(TipoUsuario.RESPONSAVEL))))
				.andExpect(status().isForbidden());
		when(responsaveis.existsVinculoAtivoVigente(eq(50L), eq(40L), any())).thenReturn(true);
		mvc.perform(get("/api/animais/50/" + endpoint).with(user(principal(TipoUsuario.RESPONSAVEL))))
				.andExpect(status().isOk());
	}

	private byte[] pdf() throws Exception {
		return mvc.perform(get("/api/animais/50/historico-pdf").with(user(principal(TipoUsuario.VETERINARIO))))
				.andExpect(status().isOk()).andExpect(content().contentType("application/pdf"))
				.andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(header().string("Content-Disposition", "attachment; filename=\"arkive-historico-50.pdf\""))
				.andReturn().getResponse().getContentAsByteArray();
	}

	private Consulta outraConsulta() {
		Veterinario vet = new Veterinario(); vet.setId(11L); vet.setNome("Outro Vet");
		Consulta outra = new Consulta(); outra.setId(64L); outra.setAnimal(animal); outra.setVeterinario(vet);
		outra.setClinica(consulta.getClinica()); outra.setStatus("FI"); outra.setModalidade("REMOTA");
		outra.setDataHora(consulta.getDataHora().minusDays(1)); outra.setMotivo("SECRET_OUTRA_CONSULTA");
		return outra;
	}

	private UsuarioPrincipal principal(TipoUsuario tipo) {
		return new UsuarioPrincipal(1L, "Usuario", "usuario@arkive.com", "hash", tipo, "S", false,
				tipo == TipoUsuario.RESPONSAVEL ? 40L : null,
				tipo == TipoUsuario.VETERINARIO ? 10L : null,
				tipo == TipoUsuario.ADMIN_CLINICA ? 30L : null);
	}
}
