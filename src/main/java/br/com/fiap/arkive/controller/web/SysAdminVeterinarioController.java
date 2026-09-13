package br.com.fiap.arkive.controller.web;

import br.com.fiap.arkive.dto.request.VeterinarioRequest;
import br.com.fiap.arkive.dto.response.VeterinarioResponse;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.service.UsuarioService;
import br.com.fiap.arkive.service.VeterinarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SysAdminVeterinarioController {

	private static final int DEFAULT_SIZE = 20;

	private final ObjectProvider<VeterinarioService> veterinarioService;
	private final ObjectProvider<UsuarioService> usuarioService;

	public SysAdminVeterinarioController(
			ObjectProvider<VeterinarioService> veterinarioService,
			ObjectProvider<UsuarioService> usuarioService
	) {
		this.veterinarioService = veterinarioService;
		this.usuarioService = usuarioService;
	}

	@GetMapping("/sysadmin/veterinarios")
	public String listar(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(required = false) String busca,
			@RequestParam(required = false) Long clinicaId,
			@RequestParam(required = false) String ativo,
			Model model,
			Authentication authentication
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		Pageable pageable = PageRequest.of(Math.max(page, 0), DEFAULT_SIZE, Sort.by("nome").ascending().and(Sort.by("id").ascending()));
		model.addAttribute("pageTitle", "Veterinários");
		model.addAttribute("veterinarios", veterinarioService().listarPorTexto(busca, clinicaId, ativo, pageable));
		model.addAttribute("clinicas", usuarioService().listarClinicasAtivas());
		model.addAttribute("busca", busca);
		model.addAttribute("clinicaSelecionadaId", clinicaId);
		model.addAttribute("ativoSelecionado", ativo);
		return "sysadmin/veterinarios/lista";
	}

	@GetMapping("/sysadmin/veterinarios/novo")
	public String novo(Model model, Authentication authentication) {
		WebModelSupport.addUserAttributes(model, authentication);
		model.addAttribute("pageTitle", "Novo veterinário");
		model.addAttribute("veterinario", novoRequest());
		adicionarOpcoesFormulario(model);
		return "sysadmin/veterinarios/formulario";
	}

	@PostMapping("/sysadmin/veterinarios")
	public String criar(
			@Valid @ModelAttribute("veterinario") VeterinarioRequest request,
			BindingResult bindingResult,
			Model model,
			Authentication authentication,
			RedirectAttributes redirectAttributes
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		model.addAttribute("pageTitle", "Novo veterinário");
		if (bindingResult.hasErrors()) {
			adicionarOpcoesFormulario(model);
			return "sysadmin/veterinarios/formulario";
		}
		try {
			veterinarioService().criar(request);
		} catch (BusinessException | ResourceNotFoundException ex) {
			bindingResult.reject("veterinario.invalido", ex.getMessage());
			adicionarOpcoesFormulario(model);
			return "sysadmin/veterinarios/formulario";
		}
		redirectAttributes.addFlashAttribute("sucesso", "Veterinário cadastrado com sucesso. A conta de acesso foi criada automaticamente.");
		redirectAttributes.addFlashAttribute("primeiroAcesso", "No primeiro acesso, utilize o e-mail cadastrado como login e senha inicial.");
		return "redirect:/sysadmin/veterinarios";
	}

	@GetMapping("/sysadmin/veterinarios/{id}/editar")
	public String editar(
			@PathVariable Long id,
			Model model,
			Authentication authentication,
			RedirectAttributes redirectAttributes
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		try {
			VeterinarioResponse veterinario = veterinarioService().buscarPorId(id);
			model.addAttribute("pageTitle", "Editar veterinário");
			model.addAttribute("veterinarioId", id);
			model.addAttribute("veterinario", editarRequest(veterinario));
			adicionarOpcoesFormulario(model);
			return "sysadmin/veterinarios/editar";
		} catch (BusinessException | ResourceNotFoundException ex) {
			redirectAttributes.addFlashAttribute("erro", ex.getMessage());
			return "redirect:/sysadmin/veterinarios";
		}
	}

	@PostMapping("/sysadmin/veterinarios/{id}/editar")
	public String atualizar(
			@PathVariable Long id,
			@Valid @ModelAttribute("veterinario") VeterinarioRequest request,
			BindingResult bindingResult,
			Model model,
			Authentication authentication,
			RedirectAttributes redirectAttributes
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		model.addAttribute("pageTitle", "Editar veterinário");
		model.addAttribute("veterinarioId", id);
		if (bindingResult.hasErrors()) {
			adicionarOpcoesFormulario(model);
			return "sysadmin/veterinarios/editar";
		}
		try {
			veterinarioService().atualizar(id, request);
		} catch (BusinessException | ResourceNotFoundException ex) {
			bindingResult.reject("veterinario.invalido", ex.getMessage());
			adicionarOpcoesFormulario(model);
			return "sysadmin/veterinarios/editar";
		}
		redirectAttributes.addFlashAttribute("sucesso", "Veterinário atualizado com sucesso.");
		return "redirect:/sysadmin/veterinarios";
	}

	@PostMapping("/sysadmin/veterinarios/{id}/desativar")
	public String desativar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		try {
			veterinarioService().excluir(id);
			redirectAttributes.addFlashAttribute("sucesso", "Veterinário desativado com sucesso.");
		} catch (BusinessException | ResourceNotFoundException ex) {
			redirectAttributes.addFlashAttribute("erro", ex.getMessage());
		}
		return "redirect:/sysadmin/veterinarios";
	}

	private void adicionarOpcoesFormulario(Model model) {
		model.addAttribute("clinicas", usuarioService().listarClinicasAtivas());
	}

	private VeterinarioRequest novoRequest() {
		return new VeterinarioRequest("", "", "", "", null, null);
	}

	private VeterinarioRequest editarRequest(VeterinarioResponse veterinario) {
		return new VeterinarioRequest(
				veterinario.nome(),
				veterinario.crmv(),
				veterinario.especialidade(),
				veterinario.email(),
				veterinario.clinicaId(),
				null
		);
	}

	private VeterinarioService veterinarioService() {
		return veterinarioService.getObject();
	}

	private UsuarioService usuarioService() {
		return usuarioService.getObject();
	}

}
