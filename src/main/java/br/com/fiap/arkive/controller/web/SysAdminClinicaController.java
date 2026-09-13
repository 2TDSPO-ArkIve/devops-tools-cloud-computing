package br.com.fiap.arkive.controller.web;

import br.com.fiap.arkive.dto.request.ClinicaRequest;
import br.com.fiap.arkive.dto.response.ClinicaResponse;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.service.ClinicaService;
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
public class SysAdminClinicaController {

	private static final int DEFAULT_SIZE = 20;

	private final ObjectProvider<ClinicaService> clinicaService;

	public SysAdminClinicaController(ObjectProvider<ClinicaService> clinicaService) {
		this.clinicaService = clinicaService;
	}

	@GetMapping("/sysadmin/clinicas")
	public String listar(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(required = false) String busca,
			@RequestParam(required = false) String ativo,
			Model model,
			Authentication authentication
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		Pageable pageable = PageRequest.of(Math.max(page, 0), DEFAULT_SIZE, Sort.by("nome").ascending().and(Sort.by("id").ascending()));
		model.addAttribute("pageTitle", "Clínicas");
		model.addAttribute("clinicas", clinicaService().listarPorTexto(busca, ativo, pageable));
		model.addAttribute("busca", busca);
		model.addAttribute("ativoSelecionado", ativo);
		return "sysadmin/clinicas/lista";
	}

	@GetMapping("/sysadmin/clinicas/nova")
	public String nova(Model model, Authentication authentication) {
		WebModelSupport.addUserAttributes(model, authentication);
		model.addAttribute("pageTitle", "Nova clínica");
		model.addAttribute("clinica", novaRequest());
		return "sysadmin/clinicas/formulario";
	}

	@PostMapping("/sysadmin/clinicas")
	public String criar(
			@Valid @ModelAttribute("clinica") ClinicaRequest request,
			BindingResult bindingResult,
			Model model,
			Authentication authentication,
			RedirectAttributes redirectAttributes
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		model.addAttribute("pageTitle", "Nova clínica");
		if (bindingResult.hasErrors()) {
			return "sysadmin/clinicas/formulario";
		}
		try {
			clinicaService().criar(request);
		} catch (BusinessException | ResourceNotFoundException ex) {
			bindingResult.reject("clinica.invalida", ex.getMessage());
			return "sysadmin/clinicas/formulario";
		}
		redirectAttributes.addFlashAttribute("sucesso", "Clínica cadastrada com sucesso. A conta administrativa foi criada automaticamente.");
		redirectAttributes.addFlashAttribute("primeiroAcesso", "No primeiro acesso, utilize o e-mail cadastrado como login e senha inicial.");
		return "redirect:/sysadmin/clinicas";
	}

	@GetMapping("/sysadmin/clinicas/{id}/editar")
	public String editar(
			@PathVariable Long id,
			Model model,
			Authentication authentication,
			RedirectAttributes redirectAttributes
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		try {
			ClinicaResponse clinica = clinicaService().buscarPorId(id);
			model.addAttribute("pageTitle", "Editar clínica");
			model.addAttribute("clinicaId", id);
			model.addAttribute("clinica", editarRequest(clinica));
			return "sysadmin/clinicas/editar";
		} catch (BusinessException | ResourceNotFoundException ex) {
			redirectAttributes.addFlashAttribute("erro", ex.getMessage());
			return "redirect:/sysadmin/clinicas";
		}
	}

	@PostMapping("/sysadmin/clinicas/{id}/editar")
	public String atualizar(
			@PathVariable Long id,
			@Valid @ModelAttribute("clinica") ClinicaRequest request,
			BindingResult bindingResult,
			Model model,
			Authentication authentication,
			RedirectAttributes redirectAttributes
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		model.addAttribute("pageTitle", "Editar clínica");
		model.addAttribute("clinicaId", id);
		if (bindingResult.hasErrors()) {
			return "sysadmin/clinicas/editar";
		}
		try {
			clinicaService().atualizar(id, request);
		} catch (BusinessException | ResourceNotFoundException ex) {
			bindingResult.reject("clinica.invalida", ex.getMessage());
			return "sysadmin/clinicas/editar";
		}
		redirectAttributes.addFlashAttribute("sucesso", "Clínica atualizada com sucesso.");
		return "redirect:/sysadmin/clinicas";
	}

	@PostMapping("/sysadmin/clinicas/{id}/desativar")
	public String desativar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		try {
			clinicaService().excluir(id);
			redirectAttributes.addFlashAttribute("sucesso", "Clínica desativada com sucesso.");
		} catch (BusinessException | ResourceNotFoundException ex) {
			redirectAttributes.addFlashAttribute("erro", ex.getMessage());
		}
		return "redirect:/sysadmin/clinicas";
	}

	private ClinicaRequest novaRequest() {
		return new ClinicaRequest("", "", "", "", "", null);
	}

	private ClinicaRequest editarRequest(ClinicaResponse clinica) {
		return new ClinicaRequest(
				clinica.nome(),
				clinica.cnpj(),
				clinica.endereco(),
				clinica.telefone(),
				clinica.email(),
				null
		);
	}

	private ClinicaService clinicaService() {
		return clinicaService.getObject();
	}

}
