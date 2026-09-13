package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.request.PrescricaoRequest;
import br.com.fiap.arkive.dto.response.PrescricaoResponse;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.PrescricaoService;
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
@RequestMapping("/api/prescricoes")
@Profile("!local-nodb")
@Tag(name = "Prescricoes", description = "Prescricoes veterinarias criadas somente apos consulta finalizada.")
public class PrescricaoController {

	private final PrescricaoService prescricaoService;

	public PrescricaoController(PrescricaoService prescricaoService) {
		this.prescricaoService = prescricaoService;
	}

	@PostMapping
	@Operation(summary = "Cria prescricao", description = "Operacao exclusiva do veterinario responsavel por consulta FI. A IA do ArkIve nunca prescreve medicamentos.")
	public ResponseEntity<PrescricaoResponse> criar(
			@Valid @RequestBody PrescricaoRequest request,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return ResponseEntity.status(HttpStatus.CREATED).body(prescricaoService.criar(request, principal));
	}

	@GetMapping
	@Operation(summary = "Lista prescricoes", description = "Lista prescricoes dentro do escopo autorizado do usuario autenticado.")
	public Page<PrescricaoResponse> listar(
			@RequestParam(required = false) Long consultaId,
			@RequestParam(required = false) String medicamento,
			Pageable pageable,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return prescricaoService.listarAutorizado(consultaId, medicamento, pageable, principal);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Busca prescricao por id", description = "Retorna a prescricao somente quando o usuario possui escopo sobre a consulta ou animal.")
	public PrescricaoResponse buscarPorId(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return prescricaoService.buscarPorIdAutorizado(id, principal);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Atualiza prescricao", description = "Permitido apenas ao veterinario dono da consulta. A consulta associada e imutavel e a alteracao e bloqueada quando ja existe adesao.")
	public PrescricaoResponse atualizar(
			@PathVariable Long id,
			@Valid @RequestBody PrescricaoRequest request,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return prescricaoService.atualizar(id, request, principal);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Remove prescricao", description = "Permitido apenas ao veterinario dono da consulta e bloqueado quando ja existe adesao.")
	public ResponseEntity<Void> excluir(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal principal) {
		prescricaoService.excluir(id, principal);
		return ResponseEntity.noContent().build();
	}

}
