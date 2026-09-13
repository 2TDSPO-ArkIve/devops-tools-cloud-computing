package br.com.fiap.arkive.service;

import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.AnimalResponsavel;
import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Diagnostico;
import br.com.fiap.arkive.entity.Especie;
import br.com.fiap.arkive.entity.Prescricao;
import br.com.fiap.arkive.entity.Raca;
import br.com.fiap.arkive.entity.Responsavel;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.repository.AnimalResponsavelRepository;
import br.com.fiap.arkive.repository.DiagnosticoRepository;
import br.com.fiap.arkive.repository.PrescricaoRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultaResumoPdfServiceTest {

	private static final String TRANSCRIPT_SECRET = "TRANSCRIPT_SECRET_SHOULD_NOT_APPEAR";
	private static final String AI_INSIGHT_SECRET = "AI_INSIGHT_SECRET_SHOULD_NOT_APPEAR";
	private static final String AI_HYPOTHESIS_SECRET = "AI_HYPOTHESIS_SECRET_SHOULD_NOT_APPEAR";
	private static final String AI_SOURCE_SECRET = "https://ai-source-secret.example/should-not-appear";

	private ConsultaService consultaService;
	private ClinicalAccessService clinicalAccessService;
	private DiagnosticoRepository diagnosticoRepository;
	private PrescricaoRepository prescricaoRepository;
	private AnimalResponsavelRepository animalResponsavelRepository;
	private ConsultaResumoPdfService service;

	@BeforeEach
	void setUp() {
		consultaService = mock(ConsultaService.class);
		clinicalAccessService = mock(ClinicalAccessService.class);
		diagnosticoRepository = mock(DiagnosticoRepository.class);
		prescricaoRepository = mock(PrescricaoRepository.class);
		animalResponsavelRepository = mock(AnimalResponsavelRepository.class);
		service = new ConsultaResumoPdfService(
				consultaService,
				clinicalAccessService,
				diagnosticoRepository,
				prescricaoRepository,
				animalResponsavelRepository
		);
	}

	@Test
	void geraPdfValidoComDadosFinaisAprovadosESemVazamentoDeCamposInternos() throws Exception {
		Consulta consulta = consultaFinalizada("Conduta com retorno em 7 dias e hidratação contínua.");
		Diagnostico confirmado = diagnosticoConfirmado(consulta);
		Prescricao prescricao = prescricao(consulta, "Medicamento Único", "1 comprimido", "a cada 12 horas", "ORAL", "Administrar com alimento.");
		when(consultaService.buscarEntidade(63L)).thenReturn(consulta);
		when(diagnosticoRepository.buscarDiagnosticosConfirmadosVeterinario(eq(63L), any(Pageable.class))).thenReturn(List.of(confirmado));
		when(prescricaoRepository.buscarPorConsulta(63L)).thenReturn(List.of(prescricao));
		when(animalResponsavelRepository.buscarResponsaveisPrincipaisAtivosVigentes(eq(50L), any(LocalDate.class)))
				.thenReturn(List.of(responsavelPrincipal()));

		ConsultaResumoPdfService.ConsultaResumoPdf pdf = service.gerarResumo(63L, veterinario());
		String texto = extrairTexto(pdf.bytes());

		assertTrue(pdf.bytes().length > 0);
		assertEquals("%PDF", new String(pdf.bytes(), 0, 4, StandardCharsets.US_ASCII));
		assertEquals("arkive-consulta-bilu-acu-63.pdf", pdf.filename());
		assertTrue(texto.contains("Bilu Açú"));
		assertTrue(texto.contains("Cão"));
		assertTrue(texto.contains("Golden Retriever"));
		assertTrue(texto.contains("Fêmea"));
		assertTrue(texto.contains("Dra. Érica São José"));
		assertTrue(texto.contains("SP12345"));
		assertTrue(texto.contains("Diagnóstico confirmado com çãáéíóúÊ"));
		assertTrue(texto.contains("Conduta com retorno em 7 dias"));
		assertTrue(texto.contains("Medicamento Único"));
		assertTrue(texto.contains("1 comprimido"));
		assertTrue(texto.contains("a cada 12 horas"));
		assertTrue(texto.contains("Tutora Ana"));
		assertFalse(texto.contains(TRANSCRIPT_SECRET));
		assertFalse(texto.contains(AI_HYPOTHESIS_SECRET));
		assertFalse(texto.contains(AI_INSIGHT_SECRET));
		assertFalse(texto.contains(AI_SOURCE_SECRET));
		assertFalse(texto.contains("Confiança"));
		assertFalse(texto.contains("87"));
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.CsvSource(value = {"PRESENCIAL|Rua do Atendimento|true", "PRESENCIAL|NULL|false", "PRESENCIAL|'   '|false", "REMOTA|Rua do Atendimento|false"}, delimiter = '|', nullValues = "NULL")
	void pdfIncluiEnderecoSomentePresencialPreenchido(String modalidade, String endereco, boolean incluir) throws Exception {
		Consulta consulta = consultaFinalizada("Retorno");
		consulta.setModalidade(modalidade); consulta.setEndereco(endereco);
		when(consultaService.buscarEntidade(63L)).thenReturn(consulta);
		when(diagnosticoRepository.buscarDiagnosticosConfirmadosVeterinario(eq(63L), any())).thenReturn(List.of(diagnosticoConfirmado(consulta)));
		String texto = extrairTexto(service.gerarResumo(63L, veterinario()).bytes());
		assertEquals(incluir, texto.contains("Endere\u00e7o"));
		assertEquals(incluir, texto.contains("Rua do Atendimento"));
	}

	@Test
	void rejeitaConsultaNaoFinalizada() {
		Consulta consulta = consultaFinalizada("Conclusão");
		consulta.setStatus("AP");
		when(consultaService.buscarEntidade(63L)).thenReturn(consulta);

		BusinessException exception = assertThrows(BusinessException.class, () -> service.gerarResumo(63L, veterinario()));

		assertEquals(HttpStatus.CONFLICT, exception.getStatus());
		verify(diagnosticoRepository, never()).buscarDiagnosticosConfirmadosVeterinario(any(), any());
	}

	@Test
	void rejeitaVeterinarioNaoAutorizado() {
		Consulta consulta = consultaFinalizada("Conclusão");
		UsuarioPrincipal principal = veterinario();
		when(consultaService.buscarEntidade(63L)).thenReturn(consulta);
		doThrow(new AccessDeniedException("Usuario nao autorizado para esta consulta."))
				.when(clinicalAccessService).exigirLeituraConsulta(principal, consulta);

		assertThrows(AccessDeniedException.class, () -> service.gerarResumo(63L, principal));

		verify(diagnosticoRepository, never()).buscarDiagnosticosConfirmadosVeterinario(any(), any());
	}

	@Test
	void rejeitaConsultaFinalizadaSemDiagnosticoConfirmado() {
		Consulta consulta = consultaFinalizada("Conclusão");
		when(consultaService.buscarEntidade(63L)).thenReturn(consulta);
		when(diagnosticoRepository.buscarDiagnosticosConfirmadosVeterinario(eq(63L), any(Pageable.class))).thenReturn(List.of());

		BusinessException exception = assertThrows(BusinessException.class, () -> service.gerarResumo(63L, veterinario()));

		assertEquals(HttpStatus.CONFLICT, exception.getStatus());
	}

	@Test
	void funcionaSemPrescricoesEOmiteSecaoDeMedicacoes() throws Exception {
		Consulta consulta = consultaFinalizada("Conclusão sem medicamentos.");
		when(consultaService.buscarEntidade(63L)).thenReturn(consulta);
		when(diagnosticoRepository.buscarDiagnosticosConfirmadosVeterinario(eq(63L), any(Pageable.class))).thenReturn(List.of(diagnosticoConfirmado(consulta)));
		when(prescricaoRepository.buscarPorConsulta(63L)).thenReturn(List.of());
		when(animalResponsavelRepository.buscarResponsaveisPrincipaisAtivosVigentes(eq(50L), any(LocalDate.class))).thenReturn(List.of());

		String texto = extrairTexto(service.gerarResumo(63L, veterinario()).bytes());

		assertTrue(texto.contains("Diagnóstico confirmado"));
		assertFalse(texto.contains("Medicações Registradas"));
	}

	@Test
	void funcionaComMultiplasPrescricoes() throws Exception {
		Consulta consulta = consultaFinalizada("Conclusão com dois tratamentos.");
		when(consultaService.buscarEntidade(63L)).thenReturn(consulta);
		when(diagnosticoRepository.buscarDiagnosticosConfirmadosVeterinario(eq(63L), any(Pageable.class))).thenReturn(List.of(diagnosticoConfirmado(consulta)));
		when(prescricaoRepository.buscarPorConsulta(63L)).thenReturn(List.of(
				prescricao(consulta, "Medicamento Alfa", "5 mg", "a cada 8 horas", "ORAL", "Instrução alfa."),
				prescricao(consulta, "Medicamento Beta", "2 gotas", "a cada 24 horas", "OCULAR", "Instrução beta.")
		));
		when(animalResponsavelRepository.buscarResponsaveisPrincipaisAtivosVigentes(eq(50L), any(LocalDate.class))).thenReturn(List.of());

		String texto = extrairTexto(service.gerarResumo(63L, veterinario()).bytes());

		assertTrue(texto.contains("Medicamento Alfa"));
		assertTrue(texto.contains("5 mg"));
		assertTrue(texto.contains("a cada 8 horas"));
		assertTrue(texto.contains("Medicamento Beta"));
		assertTrue(texto.contains("2 gotas"));
		assertTrue(texto.contains("a cada 24 horas"));
	}

	@Test
	void conteudoLongoGeraMultiplasPaginasSemPerderTexto() throws Exception {
		Consulta consulta = consultaFinalizada(("Conclusão longa com çãáéíóúÊ e acompanhamento semanal. ").repeat(120)
				+ "FINAL_LONG_TEXT_EXTRACTABLE");
		when(consultaService.buscarEntidade(63L)).thenReturn(consulta);
		when(diagnosticoRepository.buscarDiagnosticosConfirmadosVeterinario(eq(63L), any(Pageable.class))).thenReturn(List.of(diagnosticoConfirmado(consulta)));
		when(prescricaoRepository.buscarPorConsulta(63L)).thenReturn(List.of(
				prescricao(consulta, "Medicamento Longo 1", "1 ml", "a cada 12 horas", "ORAL", "Instruções longas ".repeat(60)),
				prescricao(consulta, "Medicamento Longo 2", "2 ml", "a cada 24 horas", "ORAL", "Continuar observação clínica ".repeat(60)),
				prescricao(consulta, "Medicamento Longo 3", "3 ml", "a cada 48 horas", "TOPICO", "Aplicar suavemente ".repeat(60))
		));
		when(animalResponsavelRepository.buscarResponsaveisPrincipaisAtivosVigentes(eq(50L), any(LocalDate.class))).thenReturn(List.of());

		byte[] bytes = service.gerarResumo(63L, veterinario()).bytes();

		try (PDDocument document = Loader.loadPDF(bytes)) {
			String texto = new PDFTextStripper().getText(document);
			assertTrue(document.getNumberOfPages() > 1);
			assertTrue(texto.contains("FINAL_LONG_TEXT_EXTRACTABLE"));
			assertTrue(texto.contains("Medicamento Longo 3"));
		}
	}

	private String extrairTexto(byte[] bytes) throws IOException {
		try (PDDocument document = Loader.loadPDF(bytes)) {
			return new PDFTextStripper().getText(document);
		}
	}

	private Consulta consultaFinalizada(String conclusao) {
		Consulta consulta = new Consulta();
		consulta.setId(63L);
		consulta.setDataHora(LocalDateTime.parse("2026-09-05T14:30:00"));
		consulta.setModalidade("PRESENCIAL");
		consulta.setMotivo("Avaliação de claudicação com histórico informado pelo tutor.");
		consulta.setObservacao(conclusao);
		consulta.setTranscricao(TRANSCRIPT_SECRET);
		consulta.setStatus("FI");
		consulta.setAnimal(animal());
		consulta.setVeterinario(veterinarioEntidade());
		consulta.setClinica(clinica());
		return consulta;
	}

	private Animal animal() {
		Especie especie = new Especie();
		especie.setNome("Cão");
		Raca raca = new Raca();
		raca.setNome("Golden Retriever");
		Animal animal = new Animal();
		animal.setId(50L);
		animal.setNome("Bilu Açú");
		animal.setEspecie(especie);
		animal.setRaca(raca);
		animal.setSexo("F");
		animal.setCastrado("S");
		return animal;
	}

	private Veterinario veterinarioEntidade() {
		Veterinario veterinario = new Veterinario();
		veterinario.setId(10L);
		veterinario.setNome("Dra. Érica São José");
		veterinario.setCrmv("SP12345");
		return veterinario;
	}

	private Clinica clinica() {
		Clinica clinica = new Clinica();
		clinica.setId(30L);
		clinica.setNome("Clínica São Francisco");
		return clinica;
	}

	private Diagnostico diagnosticoConfirmado(Consulta consulta) {
		Diagnostico diagnostico = new Diagnostico();
		diagnostico.setId(70L);
		diagnostico.setDiagnostico("Diagnóstico confirmado com çãáéíóúÊ");
		diagnostico.setSeveridade("MODERADA");
		diagnostico.setConfirmado("S");
		diagnostico.setValidacaoVet("S");
		diagnostico.setInsightIa(AI_INSIGHT_SECRET);
		diagnostico.setConfianca(BigDecimal.valueOf(87));
		diagnostico.setFontesIaJson("[\"" + AI_SOURCE_SECRET + "\"]");
		diagnostico.setConsulta(consulta);
		return diagnostico;
	}

	private Prescricao prescricao(
			Consulta consulta,
			String medicamento,
			String dosagem,
			String frequencia,
			String viaAdministracao,
			String instrucoes
	) {
		Prescricao prescricao = new Prescricao();
		prescricao.setId(80L);
		prescricao.setMedicamento(medicamento);
		prescricao.setDosagem(dosagem);
		prescricao.setFrequencia(frequencia);
		prescricao.setViaAdministracao(viaAdministracao);
		prescricao.setDataInicio(LocalDate.parse("2026-09-05"));
		prescricao.setDataFim(LocalDate.parse("2026-09-12"));
		prescricao.setInstrucoes(instrucoes);
		prescricao.setConsulta(consulta);
		return prescricao;
	}

	private AnimalResponsavel responsavelPrincipal() {
		Responsavel responsavel = new Responsavel();
		responsavel.setId(40L);
		responsavel.setNome("Tutora Ana");
		AnimalResponsavel vinculo = new AnimalResponsavel();
		vinculo.setResponsavel(responsavel);
		vinculo.setPrincipal("S");
		vinculo.setAtivo("S");
		return vinculo;
	}

	private UsuarioPrincipal veterinario() {
		return new UsuarioPrincipal(1L, "Dra", "vet@arkive.com", "$2a$10$hash", TipoUsuario.VETERINARIO, "S", false, null, 10L, null);
	}
}
