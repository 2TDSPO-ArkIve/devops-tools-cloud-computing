package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.request.ResponsavelRequest;
import br.com.fiap.arkive.dto.response.ResponsavelResponse;
import br.com.fiap.arkive.service.ResponsavelService;
import br.com.fiap.arkive.dto.response.ResponsavelLookupResponse;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.entity.TipoUsuario;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.PageImpl;
import java.util.List;
import java.util.Objects;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/responsaveis")
@Profile("!local-nodb")
public class ResponsavelController {

	private final ResponsavelService responsavelService;

	public ResponsavelController(ResponsavelService responsavelService) {
		this.responsavelService = responsavelService;
	}

	@PostMapping
	public ResponseEntity<ResponsavelResponse> criar(@Valid @RequestBody ResponsavelRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(responsavelService.criar(request));
	}

	@GetMapping
	public Page<ResponsavelResponse> listar(
			@RequestParam(required = false) String nome,
			@RequestParam(required = false) String documento,
			@RequestParam(required = false) String tipo,
			@RequestParam(required = false) String ativo,
			Pageable pageable,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		if (principal == null) {
			throw new AccessDeniedException("Usuario autenticado invalido.");
		}
		if (TipoUsuario.RESPONSAVEL.equals(principal.getTipoUsuario())) {
			return new PageImpl<>(List.of(buscarPorId(principal.getResponsavelId(), principal)));
		}
		return responsavelService.listar(nome, documento, tipo, ativo, pageable);
	}

	@GetMapping("/busca")
	public Page<ResponsavelLookupResponse> buscarParaVinculo(@RequestParam String busca, Pageable pageable) {
		return responsavelService.buscarParaVinculo(busca, pageable);
	}

	@GetMapping("/{id}")
	public ResponsavelResponse buscarPorId(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal principal) {
		if (principal == null || (TipoUsuario.RESPONSAVEL.equals(principal.getTipoUsuario())
				&& !Objects.equals(id, principal.getResponsavelId()))) {
			throw new AccessDeniedException("Responsavel nao autorizado.");
		}
		return responsavelService.buscarPorId(id);
	}

	@PutMapping("/{id}")
	public ResponsavelResponse atualizar(@PathVariable Long id, @Valid @RequestBody ResponsavelRequest request) {
		return responsavelService.atualizar(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@PathVariable Long id) {
		responsavelService.excluir(id);
		return ResponseEntity.noContent().build();
	}

}
