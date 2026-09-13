package br.com.fiap.arkive.controller.web;

import br.com.fiap.arkive.dto.response.AdesaoPrescricaoResponse;
import br.com.fiap.arkive.dto.response.ConsultaResponse;
import br.com.fiap.arkive.dto.response.DiagnosticoResponse;
import br.com.fiap.arkive.dto.response.PrescricaoResponse;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.AdesaoPrescricaoService;
import br.com.fiap.arkive.service.ConsultaService;
import br.com.fiap.arkive.service.DiagnosticoService;
import br.com.fiap.arkive.service.PrescricaoService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class AdminClinicaController {

	private static final int DEFAULT_SIZE = 20;
	private static final int OPTION_SIZE = 200;

	private final ObjectProvider<ConsultaService> consultaService;
	private final ObjectProvider<DiagnosticoService> diagnosticoService;
	private final ObjectProvider<PrescricaoService> prescricaoService;
	private final ObjectProvider<AdesaoPrescricaoService> adesaoPrescricaoService;

	public AdminClinicaController(
			ObjectProvider<ConsultaService> consultaService,
			ObjectProvider<DiagnosticoService> diagnosticoService,
			ObjectProvider<PrescricaoService> prescricaoService,
			ObjectProvider<AdesaoPrescricaoService> adesaoPrescricaoService
	) {
		this.consultaService = consultaService;
		this.diagnosticoService = diagnosticoService;
		this.prescricaoService = prescricaoService;
		this.adesaoPrescricaoService = adesaoPrescricaoService;
	}

	@GetMapping("/admin/consultas")
	public String listarConsultas(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(required = false) String status,
			Model model,
			Authentication authentication,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		Pageable pageable = PageRequest.of(Math.max(page, 0), DEFAULT_SIZE, Sort.by("dataHora").descending().and(Sort.by("id").descending()));
		model.addAttribute("pageTitle", "Consultas");
		model.addAttribute("consultas", listarConsultas(status, pageable, principal));
		model.addAttribute("statusSelecionado", status);
		return "admin/consultas/lista";
	}

	@GetMapping("/admin/consultas/{id}")
	public String detalheConsulta(
			@PathVariable Long id,
			Model model,
			Authentication authentication,
			@AuthenticationPrincipal UsuarioPrincipal principal,
			RedirectAttributes redirectAttributes
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		try {
			ConsultaResponse consulta = consultaService().buscarPorIdAutorizado(id, principal);
			List<DiagnosticoResponse> diagnosticos = listarDiagnosticosConsulta(id, principal);
			List<PrescricaoResponse> prescricoes = listarPrescricoesConsulta(id, principal);
			model.addAttribute("pageTitle", "Consulta");
			model.addAttribute("consulta", consulta);
			model.addAttribute("statusSteps", statusSteps(consulta.status()));
			model.addAttribute("apoiosIa", diagnosticos.stream().filter(this::apoioClinicoIa).toList());
			model.addAttribute("pareceresVeterinarios", diagnosticos.stream().filter(this::parecerVeterinario).toList());
			model.addAttribute("prescricoes", prescricoes);
			model.addAttribute("adesoes", prescricoes.stream()
					.flatMap(prescricao -> listarAdesoesPrescricao(prescricao.id(), principal).stream())
					.toList());
			return "admin/consultas/detalhe";
		} catch (BusinessException | ResourceNotFoundException ex) {
			redirectAttributes.addFlashAttribute("erro", ex.getMessage());
			return "redirect:/admin/consultas";
		}
	}

	@GetMapping("/admin/prescricoes")
	public String listarPrescricoes(
			@RequestParam(defaultValue = "0") int page,
			Model model,
			Authentication authentication,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		Pageable pageable = PageRequest.of(Math.max(page, 0), DEFAULT_SIZE, Sort.by("id").descending());
		model.addAttribute("pageTitle", "Prescrições");
		model.addAttribute("prescricoes", listarPrescricoes(pageable, principal));
		return "admin/prescricoes/lista";
	}

	@GetMapping("/admin/prescricoes/{id}")
	public String detalhePrescricao(
			@PathVariable Long id,
			Model model,
			Authentication authentication,
			@AuthenticationPrincipal UsuarioPrincipal principal,
			RedirectAttributes redirectAttributes
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		try {
			model.addAttribute("pageTitle", "Prescrição");
			model.addAttribute("prescricao", prescricaoService().buscarPorIdAutorizado(id, principal));
			return "admin/prescricoes/detalhe";
		} catch (BusinessException | ResourceNotFoundException ex) {
			redirectAttributes.addFlashAttribute("erro", ex.getMessage());
			return "redirect:/admin/prescricoes";
		}
	}

	@GetMapping("/admin/adesoes")
	public String listarAdesoes(
			@RequestParam(defaultValue = "0") int page,
			Model model,
			Authentication authentication,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		Pageable pageable = PageRequest.of(Math.max(page, 0), DEFAULT_SIZE, Sort.by("dataRegistro").descending().and(Sort.by("id").descending()));
		model.addAttribute("pageTitle", "Adesão");
		model.addAttribute("adesoes", listarAdesoes(pageable, principal));
		return "admin/adesoes/lista";
	}

	@GetMapping("/admin/adesoes/{id}")
	public String detalheAdesao(
			@PathVariable Long id,
			Model model,
			Authentication authentication,
			@AuthenticationPrincipal UsuarioPrincipal principal,
			RedirectAttributes redirectAttributes
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		try {
			model.addAttribute("pageTitle", "Adesão");
			model.addAttribute("adesao", adesaoPrescricaoService().buscarPorIdAutorizado(id, principal));
			return "admin/adesoes/detalhe";
		} catch (BusinessException | ResourceNotFoundException ex) {
			redirectAttributes.addFlashAttribute("erro", ex.getMessage());
			return "redirect:/admin/adesoes";
		}
	}

	private Page<ConsultaResponse> listarConsultas(String status, Pageable pageable, UsuarioPrincipal principal) {
		ConsultaService service = consultaService.getIfAvailable();
		if (service == null || principal == null) {
			return Page.empty(pageable);
		}
		return service.listarAutorizado(null, null, null, vazioParaNulo(status), null, pageable, principal);
	}

	private List<DiagnosticoResponse> listarDiagnosticosConsulta(Long consultaId, UsuarioPrincipal principal) {
		DiagnosticoService service = diagnosticoService.getIfAvailable();
		if (service == null || principal == null) {
			return List.of();
		}
		Pageable pageable = PageRequest.of(0, OPTION_SIZE, Sort.by("id").ascending());
		return service.listarAutorizado(consultaId, null, null, null, pageable, principal).getContent();
	}

	private List<PrescricaoResponse> listarPrescricoesConsulta(Long consultaId, UsuarioPrincipal principal) {
		PrescricaoService service = prescricaoService.getIfAvailable();
		if (service == null || principal == null) {
			return List.of();
		}
		Pageable pageable = PageRequest.of(0, OPTION_SIZE, Sort.by("id").descending());
		return service.listarAutorizado(consultaId, null, pageable, principal).getContent();
	}

	private List<AdesaoPrescricaoResponse> listarAdesoesPrescricao(Long prescricaoId, UsuarioPrincipal principal) {
		AdesaoPrescricaoService service = adesaoPrescricaoService.getIfAvailable();
		if (service == null || principal == null) {
			return List.of();
		}
		Pageable pageable = PageRequest.of(0, OPTION_SIZE, Sort.by("dataRegistro").descending().and(Sort.by("id").descending()));
		return service.listarAutorizado(prescricaoId, null, null, null, pageable, principal).getContent();
	}

	private boolean apoioClinicoIa(DiagnosticoResponse diagnostico) {
		return "N".equals(diagnostico.confirmado())
				&& "N".equals(diagnostico.validacaoVet())
				&& diagnostico.insightIa() != null
				&& !diagnostico.insightIa().isBlank();
	}

	private boolean parecerVeterinario(DiagnosticoResponse diagnostico) {
		return "S".equals(diagnostico.confirmado()) && "S".equals(diagnostico.validacaoVet());
	}

	private List<StatusStep> statusSteps(String statusAtual) {
		List<StatusStep> steps = List.of(
				new StatusStep("AG", "Agendada"),
				new StatusStep("EP", "Em progresso"),
				new StatusStep("AP", "Aguardando parecer"),
				new StatusStep("FI", "Finalizada")
		);
		if ("CA".equals(statusAtual)) {
			return List.of(new StatusStep("CA", "Cancelada", true, true));
		}
		int atual = -1;
		for (int i = 0; i < steps.size(); i++) {
			if (steps.get(i).codigo().equals(statusAtual)) {
				atual = i;
				break;
			}
		}
		int indiceAtual = atual;
		return steps.stream()
				.map(step -> new StatusStep(step.codigo(), step.label(), steps.indexOf(step) <= indiceAtual, step.codigo().equals(statusAtual)))
				.toList();
	}

	private Page<PrescricaoResponse> listarPrescricoes(Pageable pageable, UsuarioPrincipal principal) {
		PrescricaoService service = prescricaoService.getIfAvailable();
		if (service == null || principal == null) {
			return Page.empty(pageable);
		}
		return service.listarAutorizado(null, null, pageable, principal);
	}

	private Page<AdesaoPrescricaoResponse> listarAdesoes(Pageable pageable, UsuarioPrincipal principal) {
		AdesaoPrescricaoService service = adesaoPrescricaoService.getIfAvailable();
		if (service == null || principal == null) {
			return Page.empty(pageable);
		}
		return service.listarAutorizado(null, null, null, null, pageable, principal);
	}

	private String vazioParaNulo(String valor) {
		return valor == null || valor.isBlank() ? null : valor;
	}

	private ConsultaService consultaService() {
		ConsultaService service = consultaService.getIfAvailable();
		if (service == null) {
			throw new AccessDeniedException("Servico de consultas indisponivel.");
		}
		return service;
	}

	private PrescricaoService prescricaoService() {
		PrescricaoService service = prescricaoService.getIfAvailable();
		if (service == null) {
			throw new AccessDeniedException("Servico de prescricoes indisponivel.");
		}
		return service;
	}

	private AdesaoPrescricaoService adesaoPrescricaoService() {
		AdesaoPrescricaoService service = adesaoPrescricaoService.getIfAvailable();
		if (service == null) {
			throw new AccessDeniedException("Servico de adesoes indisponivel.");
		}
		return service;
	}

	public record StatusStep(String codigo, String label, boolean done, boolean current) {

		StatusStep(String codigo, String label) {
			this(codigo, label, false, false);
		}

	}

}
