package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.AtualizarNarrativaConsultaRequest;
import br.com.fiap.arkive.dto.request.ConsultaRequest;
import br.com.fiap.arkive.dto.request.FinalizarConsultaRequest;
import br.com.fiap.arkive.dto.request.PrescricaoRequest;
import br.com.fiap.arkive.dto.request.RegistrarAdesaoPrescricaoRequest;
import br.com.fiap.arkive.entity.AdesaoPrescricao;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Clinica;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Diagnostico;
import br.com.fiap.arkive.entity.Prescricao;
import br.com.fiap.arkive.entity.Responsavel;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.repository.AdesaoPrescricaoRepository;
import br.com.fiap.arkive.repository.AnimalRepository;
import br.com.fiap.arkive.repository.AnimalResponsavelRepository;
import br.com.fiap.arkive.repository.ClinicaRepository;
import br.com.fiap.arkive.repository.ConsultaRepository;
import br.com.fiap.arkive.repository.DiagnosticoRepository;
import br.com.fiap.arkive.repository.DoencaRepository;
import br.com.fiap.arkive.repository.PrescricaoRepository;
import br.com.fiap.arkive.repository.ResponsavelRepository;
import br.com.fiap.arkive.repository.VeterinarioRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.clinical.ClinicalSupportProvider;
import br.com.fiap.arkive.service.clinical.ClinicalSupportProviderResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClinicalWorkflowEndToEndServiceTest {

	private final AtomicLong ids = new AtomicLong(100);
	private final List<Diagnostico> diagnosticos = new ArrayList<>();

	private ConsultaRepository consultaRepository;
	private PrescricaoRepository prescricaoRepository;
	private AdesaoPrescricaoRepository adesaoPrescricaoRepository;
	private ClinicalSupportProvider clinicalSupportProvider;
	private EventoJornadaService eventoJornadaService;
	private ConsultaService consultaService;
	private ConsultaWorkflowService consultaWorkflowService;
	private ClinicalSupportService clinicalSupportService;
	private PrescricaoService prescricaoService;
	private AdesaoPrescricaoService adesaoPrescricaoService;

	private Consulta consultaAtual;
	private Prescricao prescricaoAtual;
	private AdesaoPrescricao adesaoAtual;
	private Animal animal;
	private Veterinario veterinario;
	private Clinica clinica;
	private Responsavel responsavel;

	@BeforeEach
	void setUp() {
		consultaRepository = mock(ConsultaRepository.class);
		AnimalRepository animalRepository = mock(AnimalRepository.class);
		VeterinarioRepository veterinarioRepository = mock(VeterinarioRepository.class);
		ClinicaRepository clinicaRepository = mock(ClinicaRepository.class);
		DiagnosticoRepository diagnosticoRepository = mock(DiagnosticoRepository.class);
		DoencaRepository doencaRepository = mock(DoencaRepository.class);
		prescricaoRepository = mock(PrescricaoRepository.class);
		adesaoPrescricaoRepository = mock(AdesaoPrescricaoRepository.class);
		ResponsavelRepository responsavelRepository = mock(ResponsavelRepository.class);
		AnimalResponsavelRepository animalResponsavelRepository = mock(AnimalResponsavelRepository.class);
		VeterinarioService veterinarioService = mock(VeterinarioService.class);
		eventoJornadaService = mock(EventoJornadaService.class);
		clinicalSupportProvider = mock(ClinicalSupportProvider.class);

		animal = animal();
		veterinario = veterinario();
		clinica = clinica();
		responsavel = responsavel();

		ClinicalAccessService clinicalAccessService = new ClinicalAccessService(animalResponsavelRepository, consultaRepository, veterinarioService);
		consultaService = new ConsultaService(consultaRepository, animalRepository, veterinarioRepository, clinicaRepository,
				eventoJornadaService, clinicalAccessService, new br.com.fiap.arkive.config.BusinessTimeConfig().businessClock());
		DiagnosticoService diagnosticoService = new DiagnosticoService(diagnosticoRepository, consultaService, doencaRepository, clinicalAccessService);
		consultaWorkflowService = new ConsultaWorkflowService(consultaService, consultaRepository, diagnosticoService, eventoJornadaService, clinicalAccessService);
		ClinicalSupportPersistenceService persistenceService = new ClinicalSupportPersistenceService(consultaService, consultaRepository, diagnosticoService, clinicalAccessService, new ObjectMapper());
		clinicalSupportService = new ClinicalSupportService(consultaService, diagnosticoService, clinicalAccessService, clinicalSupportProvider, persistenceService);
		prescricaoService = new PrescricaoService(prescricaoRepository, adesaoPrescricaoRepository, consultaService, eventoJornadaService, clinicalAccessService);
		adesaoPrescricaoService = new AdesaoPrescricaoService(adesaoPrescricaoRepository, prescricaoService, animalRepository, responsavelRepository, eventoJornadaService, clinicalAccessService);

		when(animalRepository.findById(50L)).thenReturn(Optional.of(animal));
		when(veterinarioRepository.findById(10L)).thenReturn(Optional.of(veterinario));
		when(clinicaRepository.findById(30L)).thenReturn(Optional.of(clinica));
		when(veterinarioService.buscarClinicaId(10L)).thenReturn(30L);
		when(responsavelRepository.findById(40L)).thenReturn(Optional.of(responsavel));
		when(animalResponsavelRepository.existsVinculoAtivoVigente(eq(50L), eq(40L), any(LocalDate.class))).thenReturn(true);
		when(eventoJornadaService.criarPayload(any(), any(), any())).thenReturn("{\"audit\":\"mock\"}");
		when(clinicalSupportProvider.gerarSuporte(100L)).thenReturn(new ClinicalSupportProviderResult(
				"Entorse leve",
				"LEVE",
				"Avaliar apoio, dor e amplitude articular.",
				72,
				List.of("https://source-one.example", "https://source-two.example")
		));

		when(consultaRepository.save(any(Consulta.class))).thenAnswer(invocation -> {
			Consulta consulta = invocation.getArgument(0);
			if (consulta.getId() == null) {
				consulta.setId(100L);
			}
			consultaAtual = consulta;
			return consulta;
		});
		when(consultaRepository.findById(100L)).thenAnswer(invocation -> Optional.ofNullable(consultaAtual));

		when(diagnosticoRepository.save(any(Diagnostico.class))).thenAnswer(invocation -> {
			Diagnostico diagnostico = invocation.getArgument(0);
			if (diagnostico.getId() == null) {
				diagnostico.setId(ids.incrementAndGet());
			}
			diagnosticos.add(diagnostico);
			return diagnostico;
		});
		when(diagnosticoRepository.buscarSuportesClinicos(eq(100L), any(Pageable.class))).thenAnswer(invocation -> diagnosticos.stream()
				.filter(diagnostico -> diagnostico.getConsulta() != null && Long.valueOf(100L).equals(diagnostico.getConsulta().getId()))
				.filter(diagnostico -> "N".equals(diagnostico.getConfirmado()) && "N".equals(diagnostico.getValidacaoVet()))
				.filter(diagnostico -> diagnostico.getInsightIa() != null)
				.sorted(Comparator.comparing(Diagnostico::getId).reversed())
				.toList());

		when(prescricaoRepository.save(any(Prescricao.class))).thenAnswer(invocation -> {
			Prescricao prescricao = invocation.getArgument(0);
			prescricao.setId(300L);
			prescricaoAtual = prescricao;
			return prescricao;
		});
		when(prescricaoRepository.findById(300L)).thenAnswer(invocation -> Optional.ofNullable(prescricaoAtual));

		when(adesaoPrescricaoRepository.save(any(AdesaoPrescricao.class))).thenAnswer(invocation -> {
			AdesaoPrescricao adesao = invocation.getArgument(0);
			adesao.setId(400L);
			adesaoAtual = adesao;
			return adesao;
		});
		when(adesaoPrescricaoRepository.findById(400L)).thenAnswer(invocation -> Optional.ofNullable(adesaoAtual));
	}

	@Test
	void percorreFluxoClinicoCompletoComProviderMockadoEPersistenciaReutilizada() {
		UsuarioPrincipal principalVeterinario = veterinarioPrincipal(10L);
		UsuarioPrincipal principalResponsavel = responsavelPrincipal(40L);

		var consultaCriada = consultaService.criar(consultaRequest(), principalVeterinario);
		assertEquals(100L, consultaCriada.id());
		assertEquals("AG", consultaCriada.status());

		var iniciada = consultaWorkflowService.iniciar(consultaCriada.id(), principalVeterinario);
		assertEquals("EP", iniciada.status());

		var comNarrativa = consultaWorkflowService.atualizarNarrativa(
				consultaCriada.id(),
				new AtualizarNarrativaConsultaRequest("Claudicacao leve em membro posterior direito."),
				principalVeterinario
		);
		assertEquals("EP", comNarrativa.status());
		assertEquals("Claudicacao leve em membro posterior direito.", consultaAtual.getTranscricao());

		var suporteGerado = clinicalSupportService.gerarSuporte(consultaCriada.id(), principalVeterinario);
		assertEquals("AP", suporteGerado.statusConsulta());
		assertEquals("Entorse leve", suporteGerado.hipoteseDiagnostica());
		assertEquals("LEVE", suporteGerado.severidadeSugerida());
		assertEquals(72, suporteGerado.confianca());
		assertEquals(List.of("https://source-one.example", "https://source-two.example"), suporteGerado.fontesPesquisadas());
		assertEquals("AP", consultaAtual.getStatus());

		Diagnostico suporteIa = diagnosticos.get(0);
		assertEquals("N", suporteIa.getConfirmado());
		assertEquals("N", suporteIa.getValidacaoVet());
		assertEquals("[\"https://source-one.example\",\"https://source-two.example\"]", suporteIa.getFontesIaJson());

		clinicalSupportService.gerarSuporte(consultaCriada.id(), principalVeterinario);
		clinicalSupportService.buscarSuporte(consultaCriada.id(), principalVeterinario);
		verify(clinicalSupportProvider, times(1)).gerarSuporte(consultaCriada.id());

		var finalizada = consultaWorkflowService.finalizar(
				consultaCriada.id(),
				new FinalizarConsultaRequest("Entorse confirmada pelo veterinario", "LEVE", null, "Alta com observacao."),
				principalVeterinario
		);
		assertEquals("FI", finalizada.status());
		Diagnostico diagnosticoVeterinario = diagnosticos.get(diagnosticos.size() - 1);
		assertEquals("S", diagnosticoVeterinario.getConfirmado());
		assertEquals("S", diagnosticoVeterinario.getValidacaoVet());
		assertEquals("N", suporteIa.getConfirmado());
		assertEquals("N", suporteIa.getValidacaoVet());

		var prescricao = prescricaoService.criar(prescricaoRequest(consultaCriada.id()), principalVeterinario);
		assertEquals(300L, prescricao.id());
		assertEquals(consultaCriada.id(), prescricao.consultaId());

		var adesao = adesaoPrescricaoService.registrar(
				new RegistrarAdesaoPrescricaoRequest(prescricao.id(), "S", "Registro de teste do fluxo ArkIve."),
				principalResponsavel
		);
		assertEquals(400L, adesao.id());
		assertEquals(40L, adesao.responsavelId());
		assertEquals(50L, adesao.animalId());
		assertEquals("S", adesao.tomou());
		assertNotNull(adesao.dataRegistro());

		var leituraVeterinario = adesaoPrescricaoService.buscarPorIdAutorizado(adesao.id(), principalVeterinario);
		assertEquals(adesao.id(), leituraVeterinario.id());

		verify(eventoJornadaService).registrarEvento(eq("CONSULTA_CRIADA"), eq("VETERINARIO"), eq(null), eq(10L), eq(50L), eq(30L), eq("Consulta criada."), any());
		verify(eventoJornadaService).registrarEvento(eq("CONSULTA_INICIADA"), eq("VETERINARIO"), eq(null), eq(10L), eq(50L), eq(30L), eq("Consulta iniciada."), any());
		verify(eventoJornadaService).registrarEvento(eq("CONSULTA_FINALIZADA"), eq("VETERINARIO"), eq(null), eq(10L), eq(50L), eq(30L), eq("Consulta finalizada com diagnostico confirmado pelo veterinario."), any());
		verify(eventoJornadaService).registrarEvento(eq("PRESCRICAO_CRIADA"), eq("VETERINARIO"), eq(null), eq(10L), eq(50L), eq(30L), eq("Prescricao criada."), any());
		verify(eventoJornadaService).registrarEvento(eq("ADESAO_REGISTRADA"), eq("RESPONSAVEL"), eq(40L), eq(null), eq(50L), eq(null), eq("Adesao de prescricao registrada."), any());
		assertTrue(diagnosticos.size() >= 2);
	}

	private ConsultaRequest consultaRequest() {
		return new ConsultaRequest(
				LocalDateTime.now().plusDays(1),
				"PRESENCIAL",
				"Avaliacao ortopedica de demonstracao.",
				"Claudicacao leve.",
				null,
				null,
				null,
				"AG",
				50L,
				10L,
				30L
		);
	}

	private PrescricaoRequest prescricaoRequest(Long consultaId) {
		return new PrescricaoRequest(
				"Medicamento de teste",
				"Dose de demonstracao",
				"12/12h",
				"ORAL",
				LocalDate.now(),
				LocalDate.now().plusDays(3),
				"Uso demonstrativo para validacao do fluxo.",
				consultaId
		);
	}

	private Animal animal() {
		Animal animal = new Animal();
		animal.setId(50L);
		animal.setNome("Bilu");
		animal.setAtivo("S");
		animal.setClinica(clinica());
		return animal;
	}

	private Veterinario veterinario() {
		Veterinario veterinario = new Veterinario();
		veterinario.setId(10L);
		veterinario.setNome("Dra Vera");
		veterinario.setAtivo("S");
		veterinario.setClinica(clinica());
		return veterinario;
	}

	private Clinica clinica() {
		Clinica clinica = new Clinica();
		clinica.setId(30L);
		clinica.setNome("Clinica ArkIve");
		clinica.setAtivo("S");
		return clinica;
	}

	private Responsavel responsavel() {
		Responsavel responsavel = new Responsavel();
		responsavel.setId(40L);
		responsavel.setNome("Tutor Teste");
		responsavel.setAtivo("S");
		return responsavel;
	}

	private UsuarioPrincipal veterinarioPrincipal(Long veterinarioId) {
		return new UsuarioPrincipal(1L, "Dra Vera", "vera@arkive.com", "$2a$10$hash", TipoUsuario.VETERINARIO, "S", false, null, veterinarioId, null);
	}

	private UsuarioPrincipal responsavelPrincipal(Long responsavelId) {
		return new UsuarioPrincipal(2L, "Tutor Teste", "tutor@arkive.com", "$2a$10$hash", TipoUsuario.RESPONSAVEL, "S", false, responsavelId, null, null);
	}
}
