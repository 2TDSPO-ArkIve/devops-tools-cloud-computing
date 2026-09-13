package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.request.AtualizarNarrativaConsultaRequest;
import br.com.fiap.arkive.dto.request.CancelarConsultaRequest;
import br.com.fiap.arkive.dto.request.FinalizarConsultaRequest;
import br.com.fiap.arkive.dto.response.ClinicalSupportResponse;
import br.com.fiap.arkive.dto.response.ConsultaWorkflowResponse;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.ClinicalSupportService;
import br.com.fiap.arkive.service.ConsultaResumoPdfService;
import br.com.fiap.arkive.service.ConsultaWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consultas")
@Profile("!local-nodb")
@Tag(name = "Consultas", description = "Fluxo de consulta: AG agendada, EP em progresso, AP aguardando parecer, FI finalizada e CA cancelada.")
public class ConsultaWorkflowController {

	private final ConsultaWorkflowService consultaWorkflowService;
	private final ClinicalSupportService clinicalSupportService;
	private final ConsultaResumoPdfService consultaResumoPdfService;

	public ConsultaWorkflowController(
			ConsultaWorkflowService consultaWorkflowService,
			ClinicalSupportService clinicalSupportService,
			ConsultaResumoPdfService consultaResumoPdfService
	) {
		this.consultaWorkflowService = consultaWorkflowService;
		this.clinicalSupportService = clinicalSupportService;
		this.consultaResumoPdfService = consultaResumoPdfService;
	}

	@PostMapping("/{id}/iniciar")
	@Operation(summary = "Inicia uma consulta", description = "Operacao de dominio do veterinario responsavel. Transicao esperada: AG -> EP.")
	public ConsultaWorkflowResponse iniciar(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return consultaWorkflowService.iniciar(id, principal);
	}

	@PatchMapping("/{id}/narrativa")
	@Operation(summary = "Atualiza a narrativa clinica", description = "Armazena texto clinico bruto em consultas EP ou AP, sem analise automatica por IA.")
	public ConsultaWorkflowResponse atualizarNarrativa(
			@PathVariable Long id,
			@Valid @RequestBody AtualizarNarrativaConsultaRequest request,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return consultaWorkflowService.atualizarNarrativa(id, request, principal);
	}

	@PostMapping("/{id}/finalizar")
	@Operation(summary = "Finaliza uma consulta", description = "Cria a conclusao confirmada pelo veterinario, marca confirmado = S e validacaoVet = S, e faz EP/AP -> FI.")
	public ConsultaWorkflowResponse finalizar(
			@PathVariable Long id,
			@Valid @RequestBody FinalizarConsultaRequest request,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return consultaWorkflowService.finalizar(id, request, principal);
	}

	@PostMapping("/{id}/cancelar")
	@Operation(summary = "Cancela uma consulta", description = "Operacao de dominio que cancela consultas AG, EP ou AP. Transicao esperada: AG/EP/AP -> CA.")
	public ConsultaWorkflowResponse cancelar(
			@PathVariable Long id,
			@Valid @RequestBody CancelarConsultaRequest request,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return consultaWorkflowService.cancelar(id, request, principal);
	}

	@PostMapping("/{id}/suporte-clinico")
	@Operation(tags = "Suporte Clinico", summary = "Gera suporte clinico", description = "Solicita suporte investigativo por IA para consulta EP. Em sucesso, persiste diagnostico provisorio nao confirmado e faz EP -> AP. A IA nao prescreve medicamentos.")
	public ClinicalSupportResponse gerarSuporteClinico(
			@PathVariable Long id,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return clinicalSupportService.gerarSuporte(id, principal);
	}

	@GetMapping("/{id}/suporte-clinico")
	@Operation(tags = "Suporte Clinico", summary = "Consulta suporte clinico persistido", description = "Retorna o suporte clinico previamente gerado sem chamar o motor externo. O resultado e apoio clinico, nao diagnostico veterinario confirmado.")
	public ClinicalSupportResponse buscarSuporteClinico(
			@PathVariable Long id,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return clinicalSupportService.buscarSuporte(id, principal);
	}

	@GetMapping(value = "/{id}/resumo-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
	@Operation(summary = "Exporta resumo do atendimento veterinario", description = "Gera PDF privado do resumo final do atendimento veterinario, somente para consultas finalizadas.")
	public ResponseEntity<byte[]> exportarResumoPdf(
			@PathVariable Long id,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		ConsultaResumoPdfService.ConsultaResumoPdf pdf = consultaResumoPdfService.gerarResumo(id, principal);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(pdf.filename()).build().toString())
				.cacheControl(CacheControl.noStore())
				.body(pdf.bytes());
	}
}
