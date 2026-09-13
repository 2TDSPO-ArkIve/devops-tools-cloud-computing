package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.request.AnimalRequest;
import br.com.fiap.arkive.dto.response.AnimalResponse;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.AnimalService;
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
@RequestMapping("/api/animais")
@Profile("!local-nodb")
public class AnimalController {

	private final AnimalService animalService;

	public AnimalController(AnimalService animalService) {
		this.animalService = animalService;
	}

	@PostMapping
	public ResponseEntity<AnimalResponse> criar(
			@Valid @RequestBody AnimalRequest request,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return ResponseEntity.status(HttpStatus.CREATED).body(animalService.criar(request, principal));
	}

	@GetMapping
	public Page<AnimalResponse> listar(
			@RequestParam(required = false) String nome,
			@RequestParam(required = false) Long especieId,
			@RequestParam(required = false) Long racaId,
			@RequestParam(required = false) Long clinicaId,
			@RequestParam(required = false) String ativo,
			Pageable pageable,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return animalService.listarAutorizado(nome, especieId, racaId, clinicaId, ativo, pageable, principal);
	}

	@GetMapping("/clinica")
	public Page<AnimalResponse> listarPacientesClinica(
			@RequestParam(required = false) String nome,
			@RequestParam(required = false) Long especieId,
			@RequestParam(required = false) Long racaId,
			Pageable pageable,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return animalService.listarPacientesClinicaVeterinario(nome, especieId, racaId, pageable, principal);
	}

	@GetMapping("/me")
	public Page<AnimalResponse> listarMeusPacientes(
			@RequestParam(required = false) String nome,
			@RequestParam(required = false) Long especieId,
			@RequestParam(required = false) Long racaId,
			Pageable pageable,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return animalService.listarPacientesVeterinario(nome, especieId, racaId, pageable, principal);
	}

	@GetMapping("/{id}")
	public AnimalResponse buscarPorId(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return animalService.buscarPorIdAutorizado(id, principal);
	}

	@PutMapping("/{id}")
	public AnimalResponse atualizar(
			@PathVariable Long id,
			@Valid @RequestBody AnimalRequest request,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return animalService.atualizar(id, request, principal);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal principal) {
		animalService.excluir(id, principal);
		return ResponseEntity.noContent().build();
	}

}
