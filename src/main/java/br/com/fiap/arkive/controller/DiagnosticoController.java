package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.request.SalvarDiagnosticoRequest;
import br.com.fiap.arkive.dto.response.DiagnosticoResponse;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.DiagnosticoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/diagnosticos")
@Profile("!local-nodb")
@Tag(name = "Diagnosticos", description = "Diagnosticos clinicos registrados ou confirmados pelo veterinario.")
public class DiagnosticoController {

	private final DiagnosticoService diagnosticoService;

	public DiagnosticoController(DiagnosticoService diagnosticoService) {
		this.diagnosticoService = diagnosticoService;
	}

	@PostMapping
	@Operation(summary = "Cria diagnostico", description = "Cria diagnostico cliente-editavel sem permitir campos controlados por IA ou confirmacao veterinaria.")
	public ResponseEntity<DiagnosticoResponse> criar(
			@Valid @RequestBody SalvarDiagnosticoRequest request,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return ResponseEntity.status(HttpStatus.CREATED).body(diagnosticoService.criar(request.toDiagnosticoRequest(), principal));
	}

	@GetMapping
	@Operation(summary = "Lista diagnosticos", description = "Lista diagnosticos dentro do escopo autorizado do usuario autenticado.")
	public Page<DiagnosticoResponse> listar(
			@RequestParam(required = false) Long consultaId,
			@RequestParam(required = false) Long doencaId,
			@RequestParam(required = false) String severidade,
			@RequestParam(required = false) String confirmado,
			Pageable pageable,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return diagnosticoService.listarAutorizado(consultaId, doencaId, severidade, confirmado, pageable, principal);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Busca diagnostico por id", description = "Retorna diagnostico somente quando o usuario autenticado possui escopo sobre a consulta.")
	public DiagnosticoResponse buscarPorId(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return diagnosticoService.buscarPorIdAutorizado(id, principal);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Atualiza diagnostico", description = "Atualiza campos cliente-editaveis sem permitir fabricacao de IA, confianca ou confirmacao veterinaria.")
	public DiagnosticoResponse atualizar(
			@PathVariable Long id,
			@Valid @RequestBody SalvarDiagnosticoRequest request,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return diagnosticoService.atualizar(id, request.toDiagnosticoRequest(), principal);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Remove diagnostico", description = "Remove diagnostico somente quando o veterinario autenticado possui escrita clinica sobre a consulta.")
	public ResponseEntity<Void> excluir(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal principal) {
		diagnosticoService.excluir(id, principal);
		return ResponseEntity.noContent().build();
	}

}
