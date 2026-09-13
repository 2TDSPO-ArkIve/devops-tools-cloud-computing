package br.com.fiap.arkive.controller.web;

import br.com.fiap.arkive.dto.request.AnimalRequest;
import br.com.fiap.arkive.dto.response.AnimalResponse;
import br.com.fiap.arkive.dto.response.AnimalResponsavelResponse;
import br.com.fiap.arkive.dto.response.ClinicaResponse;
import br.com.fiap.arkive.dto.response.ConsultaResponse;
import br.com.fiap.arkive.dto.response.EspecieResponse;
import br.com.fiap.arkive.dto.response.RacaResponse;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.AnimalResponsavelService;
import br.com.fiap.arkive.service.AnimalService;
import br.com.fiap.arkive.service.ClinicaService;
import br.com.fiap.arkive.service.ConsultaService;
import br.com.fiap.arkive.service.EspecieService;
import br.com.fiap.arkive.service.RacaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class AdminAnimalController {

	private static final int DEFAULT_SIZE = 20;
	private static final int OPTION_SIZE = 200;
	private static final int CONSULTA_DETAIL_SIZE = 5;

	private final ObjectProvider<AnimalService> animalService;
	private final ObjectProvider<AnimalResponsavelService> animalResponsavelService;
	private final ObjectProvider<ConsultaService> consultaService;
	private final ObjectProvider<EspecieService> especieService;
	private final ObjectProvider<RacaService> racaService;
	private final ObjectProvider<ClinicaService> clinicaService;

	public AdminAnimalController(
			ObjectProvider<AnimalService> animalService,
			ObjectProvider<AnimalResponsavelService> animalResponsavelService,
			ObjectProvider<ConsultaService> consultaService,
			ObjectProvider<EspecieService> especieService,
			ObjectProvider<RacaService> racaService,
			ObjectProvider<ClinicaService> clinicaService
	) {
		this.animalService = animalService;
		this.animalResponsavelService = animalResponsavelService;
		this.consultaService = consultaService;
		this.especieService = especieService;
		this.racaService = racaService;
		this.clinicaService = clinicaService;
	}

	@GetMapping("/admin/animais")
	public String listar(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(required = false) String nome,
			@RequestParam(required = false) Long especieId,
			@RequestParam(required = false) Long racaId,
			@RequestParam(required = false) Long clinicaId,
			@RequestParam(required = false) String ativo,
			Model model,
			Authentication authentication,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		Pageable pageable = PageRequest.of(Math.max(page, 0), DEFAULT_SIZE, Sort.by("nome").ascending().and(Sort.by("id").ascending()));
		model.addAttribute("pageTitle", "Animais");
		model.addAttribute("animais", listarAnimais(nome, especieId, racaId, clinicaId, ativo, pageable, principal));
		model.addAttribute("nome", nome);
		model.addAttribute("especieSelecionadaId", especieId);
		model.addAttribute("racaSelecionadaId", racaId);
		model.addAttribute("clinicaSelecionadaId", clinicaId);
		model.addAttribute("ativoSelecionado", ativo);
		adicionarOpcoes(model, principal);
		return "admin/animais/lista";
	}

	@GetMapping("/admin/animais/novo")
	public String novo(Model model, Authentication authentication, @AuthenticationPrincipal UsuarioPrincipal principal) {
		WebModelSupport.addUserAttributes(model, authentication);
		model.addAttribute("pageTitle", "Novo animal");
		model.addAttribute("animal", novoRequest());
		adicionarOpcoes(model, principal);
		return "admin/animais/formulario";
	}

	@PostMapping("/admin/animais")
	public String criar(
			@Valid @ModelAttribute("animal") AnimalRequest request,
			BindingResult bindingResult,
			Model model,
			Authentication authentication,
			@AuthenticationPrincipal UsuarioPrincipal principal,
			RedirectAttributes redirectAttributes
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		model.addAttribute("pageTitle", "Novo animal");
		if (bindingResult.hasErrors()) {
			adicionarOpcoes(model, principal);
			return "admin/animais/formulario";
		}
		try {
			animalService().criar(request, principal);
		} catch (BusinessException | ResourceNotFoundException | AccessDeniedException ex) {
			bindingResult.reject("animal.invalido", ex.getMessage());
			adicionarOpcoes(model, principal);
			return "admin/animais/formulario";
		}
		redirectAttributes.addFlashAttribute("sucesso", "Animal cadastrado com sucesso.");
		return "redirect:/admin/animais";
	}

	@GetMapping("/admin/animais/{id}")
	public String detalhe(
			@PathVariable Long id,
			Model model,
			Authentication authentication,
			@AuthenticationPrincipal UsuarioPrincipal principal,
			RedirectAttributes redirectAttributes
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		try {
			AnimalResponse animal = animalService().buscarPorIdAutorizado(id, principal);
			List<AnimalResponsavelResponse> responsaveis = listarResponsaveis(id, principal);
			model.addAttribute("pageTitle", "Animal");
			model.addAttribute("animal", animal);
			model.addAttribute("responsavelPrincipal", responsaveis.stream().filter(this::principalAtivo).findFirst().orElse(null));
			model.addAttribute("outrosResponsaveis", responsaveis.stream().filter(responsavel -> !principalAtivo(responsavel)).toList());
			model.addAttribute("consultas", listarConsultasRecentes(id, principal));
			return "admin/animais/detalhe";
		} catch (BusinessException | ResourceNotFoundException | AccessDeniedException ex) {
			redirectAttributes.addFlashAttribute("erro", ex.getMessage());
			return "redirect:/admin/animais";
		}
	}

	@GetMapping("/admin/animais/{id}/editar")
	public String editar(
			@PathVariable Long id,
			Model model,
			Authentication authentication,
			@AuthenticationPrincipal UsuarioPrincipal principal,
			RedirectAttributes redirectAttributes
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		try {
			AnimalResponse animal = animalService().buscarPorIdAutorizado(id, principal);
			model.addAttribute("pageTitle", "Editar animal");
			model.addAttribute("animalId", id);
			model.addAttribute("animal", request(animal));
			adicionarOpcoes(model, principal);
			return "admin/animais/editar";
		} catch (BusinessException | ResourceNotFoundException | AccessDeniedException ex) {
			redirectAttributes.addFlashAttribute("erro", ex.getMessage());
			return "redirect:/admin/animais";
		}
	}

	@PostMapping("/admin/animais/{id}/editar")
	public String atualizar(
			@PathVariable Long id,
			@Valid @ModelAttribute("animal") AnimalRequest request,
			BindingResult bindingResult,
			Model model,
			Authentication authentication,
			@AuthenticationPrincipal UsuarioPrincipal principal,
			RedirectAttributes redirectAttributes
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		model.addAttribute("pageTitle", "Editar animal");
		model.addAttribute("animalId", id);
		if (bindingResult.hasErrors()) {
			adicionarOpcoes(model, principal);
			return "admin/animais/editar";
		}
		try {
			animalService().atualizar(id, request, principal);
		} catch (BusinessException | ResourceNotFoundException | AccessDeniedException ex) {
			bindingResult.reject("animal.invalido", ex.getMessage());
			adicionarOpcoes(model, principal);
			return "admin/animais/editar";
		}
		redirectAttributes.addFlashAttribute("sucesso", "Animal atualizado com sucesso.");
		return "redirect:/admin/animais/" + id;
	}

	@PostMapping("/admin/animais/{id}/desativar")
	public String desativar(
			@PathVariable Long id,
			@AuthenticationPrincipal UsuarioPrincipal principal,
			RedirectAttributes redirectAttributes
	) {
		try {
			animalService().excluir(id, principal);
			redirectAttributes.addFlashAttribute("sucesso", "Animal desativado com sucesso.");
		} catch (BusinessException | ResourceNotFoundException | AccessDeniedException ex) {
			redirectAttributes.addFlashAttribute("erro", ex.getMessage());
		}
		return "redirect:/admin/animais/" + id;
	}

	@PostMapping("/admin/animais/{id}/ativar")
	public String ativar(
			@PathVariable Long id,
			@AuthenticationPrincipal UsuarioPrincipal principal,
			RedirectAttributes redirectAttributes
	) {
		try {
			AnimalResponse animal = animalService().buscarPorIdAutorizado(id, principal);
			animalService().atualizar(id, new AnimalRequest(
					animal.nome(),
					animal.especieId(),
					animal.racaId(),
					animal.sexo(),
					animal.castrado(),
					animal.clinicaId(),
					"S",
					animal.dataNascimento()
			), principal);
			redirectAttributes.addFlashAttribute("sucesso", "Animal reativado com sucesso.");
		} catch (BusinessException | ResourceNotFoundException | AccessDeniedException ex) {
			redirectAttributes.addFlashAttribute("erro", ex.getMessage());
		}
		return "redirect:/admin/animais/" + id;
	}

	private Page<AnimalResponse> listarAnimais(
			String nome,
			Long especieId,
			Long racaId,
			Long clinicaId,
			String ativo,
			Pageable pageable,
			UsuarioPrincipal principal
	) {
		AnimalService service = animalService.getIfAvailable();
		if (service == null || principal == null) {
			return Page.empty(pageable);
		}
		return service.listarAutorizado(nome, especieId, racaId, clinicaId, ativo, pageable, principal);
	}

	private List<AnimalResponsavelResponse> listarResponsaveis(Long animalId, UsuarioPrincipal principal) {
		AnimalResponsavelService service = animalResponsavelService.getIfAvailable();
		if (service == null) {
			return List.of();
		}
		return service.listarAtivosPorAnimal(animalId, principal);
	}

	private List<ConsultaResponse> listarConsultasRecentes(Long animalId, UsuarioPrincipal principal) {
		ConsultaService service = consultaService.getIfAvailable();
		if (service == null || principal == null) {
			return List.of();
		}
		Pageable pageable = PageRequest.of(0, CONSULTA_DETAIL_SIZE, Sort.by("dataHora").descending().and(Sort.by("id").descending()));
		return service.listarAutorizado(animalId, null, null, null, null, pageable, principal).getContent();
	}

	private void adicionarOpcoes(Model model, UsuarioPrincipal principal) {
		model.addAttribute("especies", especies());
		AnimalRequest animal = model.getAttribute("animal") instanceof AnimalRequest request ? request : null;
		Long especieId = animal == null ? (Long) model.getAttribute("especieSelecionadaId") : animal.especieId();
		model.addAttribute("racas", animal != null && especieId == null ? List.of() : racas(especieId));
		model.addAttribute("clinicas", clinicas());
		model.addAttribute("sysadmin", TipoUsuario.SYSADMIN.equals(tipo(principal)));
		model.addAttribute("adminClinica", TipoUsuario.ADMIN_CLINICA.equals(tipo(principal)));
		model.addAttribute("clinicaAutenticada", clinicaAutenticada(principal));
	}

	private List<EspecieResponse> especies() {
		EspecieService service = especieService.getIfAvailable();
		if (service == null) {
			return List.of();
		}
		return service.listar(null, PageRequest.of(0, OPTION_SIZE, Sort.by("nome").ascending())).getContent();
	}

	private List<RacaResponse> racas(Long especieId) {
		RacaService service = racaService.getIfAvailable();
		if (service == null) {
			return List.of();
		}
		return service.listar(null, especieId, PageRequest.of(0, OPTION_SIZE, Sort.by("nome").ascending())).getContent();
	}

	private List<ClinicaResponse> clinicas() {
		ClinicaService service = clinicaService.getIfAvailable();
		if (service == null) {
			return List.of();
		}
		return service.listar(null, "S", PageRequest.of(0, OPTION_SIZE, Sort.by("nome").ascending())).getContent();
	}

	private ClinicaResponse clinicaAutenticada(UsuarioPrincipal principal) {
		ClinicaService service = clinicaService.getIfAvailable();
		if (service == null || principal == null || principal.getClinicaId() == null) {
			return null;
		}
		try {
			return service.buscarPorId(principal.getClinicaId());
		} catch (BusinessException | ResourceNotFoundException ex) {
			return null;
		}
	}

	private boolean principalAtivo(AnimalResponsavelResponse responsavel) {
		return "S".equals(responsavel.principal());
	}

	private AnimalRequest novoRequest() {
		return new AnimalRequest("", null, null, null, "N", null, "S");
	}

	private AnimalRequest request(AnimalResponse animal) {
		return new AnimalRequest(
				animal.nome(),
				animal.especieId(),
				animal.racaId(),
				animal.sexo(),
				animal.castrado(),
				animal.clinicaId(),
				animal.ativo(),
				animal.dataNascimento()
		);
	}

	private TipoUsuario tipo(UsuarioPrincipal principal) {
		return principal == null ? null : principal.getTipoUsuario();
	}

	private AnimalService animalService() {
		AnimalService service = animalService.getIfAvailable();
		if (service == null) {
			throw new AccessDeniedException("Servico de animais indisponivel.");
		}
		return service;
	}

}
