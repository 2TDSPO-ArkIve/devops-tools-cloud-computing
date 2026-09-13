package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.request.RegistrarAdesaoPrescricaoRequest;
import br.com.fiap.arkive.dto.response.AdesaoPrescricaoResponse;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.AdesaoPrescricaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/adesoes-prescricao")
@Profile("!local-nodb")
@Tag(name = "Adesao", description = "Adesao terapeutica registrada pelo responsavel vinculado ao animal.")
public class AdesaoPrescricaoController {

	private final AdesaoPrescricaoService adesaoPrescricaoService;

	public AdesaoPrescricaoController(AdesaoPrescricaoService adesaoPrescricaoService) {
		this.adesaoPrescricaoService = adesaoPrescricaoService;
	}

	@PostMapping
	@Operation(summary = "Registra adesao de prescricao", description = "Operacao exclusiva do responsavel vinculado ao animal. Responsavel, animal e dataRegistro sao derivados pelo servidor. Valores validos de tomou: S ou N.")
	public ResponseEntity<AdesaoPrescricaoResponse> criar(
			@Valid @RequestBody RegistrarAdesaoPrescricaoRequest request,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return ResponseEntity.status(HttpStatus.CREATED).body(adesaoPrescricaoService.registrar(request, principal));
	}

	@GetMapping
	@Operation(summary = "Lista adesoes de prescricao", description = "Lista registros de adesao dentro do escopo autorizado. Responsaveis veem apenas seus proprios registros.")
	public Page<AdesaoPrescricaoResponse> listar(
			@RequestParam(required = false) Long prescricaoId,
			@RequestParam(required = false) Long animalId,
			@RequestParam(required = false) Long responsavelId,
			@RequestParam(required = false) String tomou,
			Pageable pageable,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return adesaoPrescricaoService.listarAutorizado(prescricaoId, animalId, responsavelId, tomou, pageable, principal);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Busca adesao por id", description = "Retorna adesao somente quando o usuario autenticado possui escopo sobre a prescricao.")
	public AdesaoPrescricaoResponse buscarPorId(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return adesaoPrescricaoService.buscarPorIdAutorizado(id, principal);
	}

}
