package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.request.AnimalResponsavelRequest;
import br.com.fiap.arkive.dto.response.AnimalResponsavelResponse;
import br.com.fiap.arkive.service.AnimalResponsavelService;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/animais-responsaveis")
@Profile("!local-nodb")
public class AnimalResponsavelController {

	private final AnimalResponsavelService animalResponsavelService;

	public AnimalResponsavelController(AnimalResponsavelService animalResponsavelService) {
		this.animalResponsavelService = animalResponsavelService;
	}

	@PostMapping
	public ResponseEntity<AnimalResponsavelResponse> criar(@Valid @RequestBody AnimalResponsavelRequest request, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return ResponseEntity.status(HttpStatus.CREATED).body(animalResponsavelService.criar(request, principal));
	}

	@GetMapping
	public Page<AnimalResponsavelResponse> listar(
			@RequestParam(required = false) Long animalId,
			@RequestParam(required = false) Long responsavelId,
			@RequestParam(required = false) String tipoVinculo,
			@RequestParam(required = false) String ativo,
			Pageable pageable,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return animalResponsavelService.listar(animalId, responsavelId, tipoVinculo, ativo, pageable, principal);
	}

	@GetMapping("/animal/{animalId}")
	public List<AnimalResponsavelResponse> listarAtivosPorAnimal(@PathVariable Long animalId, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return animalResponsavelService.listarAtivosPorAnimal(animalId, principal);
	}

	@GetMapping("/responsavel/{responsavelId}")
	public Page<AnimalResponsavelResponse> listarPorResponsavel(@PathVariable Long responsavelId, Pageable pageable, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return animalResponsavelService.listarPorResponsavel(responsavelId, pageable, principal);
	}

	@PutMapping
	public AnimalResponsavelResponse atualizar(@Valid @RequestBody AnimalResponsavelRequest request, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return animalResponsavelService.atualizar(request, principal);
	}

	@PatchMapping("/encerrar")
	public AnimalResponsavelResponse encerrar(@Valid @RequestBody AnimalResponsavelRequest request, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return animalResponsavelService.encerrar(request, principal);
	}

	@DeleteMapping
	public ResponseEntity<Void> excluir(
			@RequestParam Long animalId,
			@RequestParam Long responsavelId,
			@RequestParam LocalDate dataInicio,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		animalResponsavelService.excluir(animalId, responsavelId, dataInicio, principal);
		return ResponseEntity.noContent().build();
	}

}
