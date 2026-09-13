package br.com.fiap.arkive.controller.web;

import br.com.fiap.arkive.dto.request.ResponsavelRequest;
import br.com.fiap.arkive.dto.response.ResponsavelResponse;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.service.ResponsavelService;
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
public class SysAdminResponsavelController {

	private static final int DEFAULT_SIZE = 20;

	private final ObjectProvider<ResponsavelService> responsavelService;

	public SysAdminResponsavelController(ObjectProvider<ResponsavelService> responsavelService) {
		this.responsavelService = responsavelService;
	}

	@GetMapping("/sysadmin/responsaveis")
	public String listar(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(required = false) String busca,
			@RequestParam(required = false) String ativo,
			Model model,
			Authentication authentication
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		Pageable pageable = PageRequest.of(Math.max(page, 0), DEFAULT_SIZE, Sort.by("nome").ascending().and(Sort.by("id").ascending()));
		model.addAttribute("pageTitle", "Responsáveis");
		model.addAttribute("responsaveis", responsavelService().listarPorTexto(busca, ativo, pageable));
		model.addAttribute("busca", busca);
		model.addAttribute("ativoSelecionado", ativo);
		return "sysadmin/responsaveis/lista";
	}

	@GetMapping("/sysadmin/responsaveis/novo")
	public String novo(Model model, Authentication authentication) {
		WebModelSupport.addUserAttributes(model, authentication);
		model.addAttribute("pageTitle", "Novo responsável");
		model.addAttribute("responsavel", novoRequest());
		adicionarTipos(model);
		return "sysadmin/responsaveis/formulario";
	}

	@PostMapping("/sysadmin/responsaveis")
	public String criar(
			@Valid @ModelAttribute("responsavel") ResponsavelRequest request,
			BindingResult bindingResult,
			Model model,
			Authentication authentication,
			RedirectAttributes redirectAttributes
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		model.addAttribute("pageTitle", "Novo responsável");
		if (bindingResult.hasErrors()) {
			adicionarTipos(model);
			return "sysadmin/responsaveis/formulario";
		}
		try {
			responsavelService().criar(request);
		} catch (BusinessException | ResourceNotFoundException ex) {
			bindingResult.reject("responsavel.invalido", ex.getMessage());
			adicionarTipos(model);
			return "sysadmin/responsaveis/formulario";
		}
		redirectAttributes.addFlashAttribute("sucesso", "Responsável cadastrado com sucesso.");
		return "redirect:/sysadmin/responsaveis";
	}

	@GetMapping("/sysadmin/responsaveis/{id}/editar")
	public String editar(
			@PathVariable Long id,
			Model model,
			Authentication authentication,
			RedirectAttributes redirectAttributes
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		try {
			ResponsavelResponse responsavel = responsavelService().buscarPorId(id);
			model.addAttribute("pageTitle", "Editar responsável");
			model.addAttribute("responsavelId", id);
			model.addAttribute("responsavel", editarRequest(responsavel));
			adicionarTipos(model);
			return "sysadmin/responsaveis/editar";
		} catch (BusinessException | ResourceNotFoundException ex) {
			redirectAttributes.addFlashAttribute("erro", ex.getMessage());
			return "redirect:/sysadmin/responsaveis";
		}
	}

	@PostMapping("/sysadmin/responsaveis/{id}/editar")
	public String atualizar(
			@PathVariable Long id,
			@Valid @ModelAttribute("responsavel") ResponsavelRequest request,
			BindingResult bindingResult,
			Model model,
			Authentication authentication,
			RedirectAttributes redirectAttributes
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		model.addAttribute("pageTitle", "Editar responsável");
		model.addAttribute("responsavelId", id);
		if (bindingResult.hasErrors()) {
			adicionarTipos(model);
			return "sysadmin/responsaveis/editar";
		}
		try {
			responsavelService().atualizar(id, request);
		} catch (BusinessException | ResourceNotFoundException ex) {
			bindingResult.reject("responsavel.invalido", ex.getMessage());
			adicionarTipos(model);
			return "sysadmin/responsaveis/editar";
		}
		redirectAttributes.addFlashAttribute("sucesso", "Responsável atualizado com sucesso.");
		return "redirect:/sysadmin/responsaveis";
	}

	@PostMapping("/sysadmin/responsaveis/{id}/desativar")
	public String desativar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		try {
			responsavelService().excluir(id);
			redirectAttributes.addFlashAttribute("sucesso", "Responsável desativado com sucesso.");
		} catch (BusinessException | ResourceNotFoundException ex) {
			redirectAttributes.addFlashAttribute("erro", ex.getMessage());
		}
		return "redirect:/sysadmin/responsaveis";
	}

	private void adicionarTipos(Model model) {
		model.addAttribute("tiposResponsavel", responsavelService().listarTipos());
	}

	private ResponsavelRequest novoRequest() {
		return new ResponsavelRequest("", "", "", "", "", null, null, null);
	}

	private ResponsavelRequest editarRequest(ResponsavelResponse responsavel) {
		return new ResponsavelRequest(
				responsavel.nome(),
				responsavel.documento(),
				responsavel.email(),
				responsavel.telefone(),
				responsavel.tipo(),
				null,
				null,
				null
		);
	}

	private ResponsavelService responsavelService() {
		return responsavelService.getObject();
	}

}
