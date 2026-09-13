package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.request.DiagnosticoRequest;
import br.com.fiap.arkive.entity.Animal;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Diagnostico;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.entity.Veterinario;
import br.com.fiap.arkive.repository.DiagnosticoRepository;
import br.com.fiap.arkive.repository.DoencaRepository;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DiagnosticoAuthorizationServiceTest {

	private DiagnosticoRepository diagnosticoRepository;
	private ConsultaService consultaService;
	private ClinicalAccessService clinicalAccessService;
	private DiagnosticoService diagnosticoService;

	@BeforeEach
	void setUp() {
		diagnosticoRepository = mock(DiagnosticoRepository.class);
		consultaService = mock(ConsultaService.class);
		clinicalAccessService = mock(ClinicalAccessService.class);
		diagnosticoService = new DiagnosticoService(
				diagnosticoRepository,
				consultaService,
				mock(DoencaRepository.class),
				clinicalAccessService
		);
		when(diagnosticoRepository.save(any(Diagnostico.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void veterinarioCriaDiagnosticoSomenteAposValidarConsulta() {
		Consulta consulta = consulta(10L);
		UsuarioPrincipal principal = veterinario(10L);
		when(consultaService.buscarEntidade(1L)).thenReturn(consulta);

		diagnosticoService.criar(request(1L), principal);

		verify(clinicalAccessService).exigirEscritaDiagnosticoVeterinario(eq(principal), any(Diagnostico.class));
		verify(diagnosticoRepository).save(any(Diagnostico.class));
	}

	@Test
	void diagnosticoGenericoNaoAceitaCamposControladosPeloServidor() {
		DiagnosticoRequest request = new DiagnosticoRequest("Diagnostico", "LEVE", "S", "Insight", null, "S", 1L, null);

		BusinessException exception = assertThrows(BusinessException.class, () -> diagnosticoService.criar(request, veterinario(10L)));

		assertEquals("Campos de IA, confianca, confirmacao e validacao veterinaria sao controlados pelo servidor.", exception.getMessage());
	}

	@Test
	void diagnosticoGenericoCriadoPeloClientePermaneceNaoConfirmado() {
		Consulta consulta = consulta(10L);
		when(consultaService.buscarEntidade(1L)).thenReturn(consulta);

		diagnosticoService.criar(request(1L), veterinario(10L));

		verify(diagnosticoRepository).save(org.mockito.ArgumentMatchers.argThat(diagnostico ->
				"N".equals(diagnostico.getConfirmado())
						&& "N".equals(diagnostico.getValidacaoVet())
						&& diagnostico.getInsightIa() == null
						&& diagnostico.getConfianca() == null
		));
	}

	@Test
	void criaSuporteClinicoIaComoProvisorioNaoConfirmado() {
		Consulta consulta = consulta(10L);

		Diagnostico diagnostico = diagnosticoService.criarSuporteClinicoIa(consulta, "Hipotese", "MODERADA", "Insight", 65);

		assertEquals("Hipotese", diagnostico.getDiagnostico());
		assertEquals("MODERADA", diagnostico.getSeveridade());
		assertEquals("Insight", diagnostico.getInsightIa());
		assertEquals("65", diagnostico.getConfianca().toPlainString());
		assertNull(diagnostico.getFontesIaJson());
		assertEquals("N", diagnostico.getConfirmado());
		assertEquals("N", diagnostico.getValidacaoVet());
		assertEquals(consulta, diagnostico.getConsulta());
	}

	@Test
	void criaSuporteClinicoIaPersisteJsonDeFontesQuandoInformado() {
		Consulta consulta = consulta(10L);

		Diagnostico diagnostico = diagnosticoService.criarSuporteClinicoIa(
				consulta,
				"Hipotese",
				"MODERADA",
				"Insight",
				65,
				"[\"https://source-one.example\"]"
		);

		assertEquals("[\"https://source-one.example\"]", diagnostico.getFontesIaJson());
	}

	@Test
	void atualizacaoDeDiagnosticoValidaConsultaResultante() {
		Diagnostico diagnostico = diagnostico(consulta(10L));
		UsuarioPrincipal principal = veterinario(10L);
		when(diagnosticoRepository.findById(5L)).thenReturn(Optional.of(diagnostico));
		when(consultaService.buscarEntidade(1L)).thenReturn(diagnostico.getConsulta());

		diagnosticoService.atualizar(5L, request(1L), principal);

		verify(clinicalAccessService).exigirEscritaDiagnosticoVeterinario(principal, diagnostico);
		verify(diagnosticoRepository).save(diagnostico);
	}

	@Test
	void leituraPorIdValidaEscopoDaConsulta() {
		Diagnostico diagnostico = diagnostico(consulta(10L));
		UsuarioPrincipal principal = veterinario(10L);
		when(diagnosticoRepository.findById(5L)).thenReturn(Optional.of(diagnostico));

		diagnosticoService.buscarPorIdAutorizado(5L, principal);

		verify(clinicalAccessService).exigirLeituraDiagnostico(principal, diagnostico);
	}

	@Test
	void listagemDeVeterinarioUsaConsultaDoVeterinarioAutenticado() {
		UsuarioPrincipal principal = veterinario(10L);
		when(diagnosticoRepository.buscarParaVeterinario(eq(10L), eq(null), eq(null), eq(null), eq(null), any(Pageable.class)))
				.thenReturn(Page.empty());

		diagnosticoService.listarAutorizado(null, null, null, null, Pageable.unpaged(), principal);

		verify(diagnosticoRepository).buscarParaVeterinario(eq(10L), eq(null), eq(null), eq(null), eq(null), any(Pageable.class));
	}

	private DiagnosticoRequest request(Long consultaId) {
		return new DiagnosticoRequest("Diagnostico", "LEVE", null, null, null, null, consultaId, null);
	}

	private Diagnostico diagnostico(Consulta consulta) {
		Diagnostico diagnostico = new Diagnostico();
		diagnostico.setId(5L);
		diagnostico.setDiagnostico("Diagnostico");
		diagnostico.setConsulta(consulta);
		return diagnostico;
	}

	private Consulta consulta(Long veterinarioId) {
		Animal animal = new Animal();
		animal.setId(50L);
		Veterinario veterinario = new Veterinario();
		veterinario.setId(veterinarioId);
		Consulta consulta = new Consulta();
		consulta.setId(1L);
		consulta.setAnimal(animal);
		consulta.setVeterinario(veterinario);
		return consulta;
	}

	private UsuarioPrincipal veterinario(Long veterinarioId) {
		return new UsuarioPrincipal(1L, "Dra", "vet@arkive.com", "$2a$10$hash", TipoUsuario.VETERINARIO, "S", false, null, veterinarioId, null);
	}

}
