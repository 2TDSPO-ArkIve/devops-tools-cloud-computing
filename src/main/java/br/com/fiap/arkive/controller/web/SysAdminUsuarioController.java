package br.com.fiap.arkive.controller.web;

import br.com.fiap.arkive.dto.request.UsuarioEditRequest;
import br.com.fiap.arkive.dto.request.UsuarioProvisioningRequest;
import br.com.fiap.arkive.dto.response.PasswordResetResult;
import br.com.fiap.arkive.dto.response.UsuarioResponse;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.exception.ResourceNotFoundException;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.AccountProvisioningService;
import br.com.fiap.arkive.service.UsuarioService;
import br.com.fiap.arkive.service.PasswordLifecycleService;
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

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class SysAdminUsuarioController {

	private static final int DEFAULT_SIZE = 20;
	private static final String DEFAULT_SORT_FIELD = "dataCadastro";
	private static final String DEFAULT_SORT_DIR = "desc";
	private static final Map<String, String> SORT_PROPERTIES = Map.of(
			"nome", "nome",
			"login", "login",
			"perfil", "tipo",
			"status", "ativo",
			"dataCadastro", "dataCadastro"
	);

	private final ObjectProvider<UsuarioService> usuarioService;
	private final ObjectProvider<PasswordLifecycleService> passwordLifecycleService;
	private final ObjectProvider<AccountProvisioningService> accountProvisioningService;

	public SysAdminUsuarioController(
			ObjectProvider<UsuarioService> usuarioService,
			ObjectProvider<PasswordLifecycleService> passwordLifecycleService,
			ObjectProvider<AccountProvisioningService> accountProvisioningService
	) {
		this.usuarioService = usuarioService;
		this.passwordLifecycleService = passwordLifecycleService;
		this.accountProvisioningService = accountProvisioningService;
	}

	@GetMapping("/sysadmin/usuarios")
	public String listar(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "TODOS") String perfil,
			@RequestParam(defaultValue = DEFAULT_SORT_FIELD) String sortField,
			@RequestParam(defaultValue = DEFAULT_SORT_DIR) String sortDir,
			Model model,
			Authentication authentication
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		int pagina = Math.max(page, 0);
		PerfilFiltro filtro = PerfilFiltro.from(perfil);
		String campoOrdenacao = normalizedSortField(sortField);
		Sort.Direction direcao = normalizedSortDirection(sortDir);
		Pageable pageable = PageRequest.of(pagina, DEFAULT_SIZE, sort(campoOrdenacao, direcao));
		model.addAttribute("pageTitle", "Usuários");
		model.addAttribute("usuarios", usuarioService().listarPorTipos(filtro.tipos(), pageable));
		model.addAttribute("currentUserId", currentUserId(authentication));
		model.addAttribute("filtrosPerfil", PerfilFiltro.values());
		model.addAttribute("perfilSelecionado", filtro.getValue());
		model.addAttribute("sortField", campoOrdenacao);
		model.addAttribute("sortDir", direcao.name().toLowerCase(Locale.ROOT));
		return "sysadmin/usuarios/lista";
	}

	@GetMapping("/sysadmin/usuarios/novo")
	public String novo(Model model, Authentication authentication) {
		WebModelSupport.addUserAttributes(model, authentication);
		model.addAttribute("pageTitle", "Novo usuário");
		model.addAttribute("usuario", novoRequest());
		adicionarOpcoesFormularioCriacao(model);
		return "sysadmin/usuarios/formulario";
	}

	@GetMapping("/sysadmin/usuarios/{id}/editar")
	public String editar(
			@PathVariable Long id,
			Model model,
			Authentication authentication,
			RedirectAttributes redirectAttributes
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		try {
			UsuarioResponse usuario = usuarioService().buscarPorId(id);
			model.addAttribute("pageTitle", "Editar usuário");
			model.addAttribute("usuario", editarRequest(usuario));
			model.addAttribute("usuarioId", id);
			adicionarOpcoesFormularioEdicao(model, id);
			return "sysadmin/usuarios/editar";
		} catch (BusinessException | ResourceNotFoundException ex) {
			redirectAttributes.addFlashAttribute("erro", ex.getMessage());
			return "redirect:/sysadmin/usuarios";
		}
	}

	@PostMapping("/sysadmin/usuarios")
	public String criar(
			@Valid @ModelAttribute("usuario") UsuarioProvisioningRequest request,
			BindingResult bindingResult,
			Model model,
			Authentication authentication,
			RedirectAttributes redirectAttributes
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		model.addAttribute("pageTitle", "Novo usuário");
		if (bindingResult.hasErrors()) {
			adicionarOpcoesFormularioCriacao(model);
			return "sysadmin/usuarios/formulario";
		}
		try {
			accountProvisioningService.getObject().provisionar(request);
		} catch (BusinessException | ResourceNotFoundException ex) {
			bindingResult.reject("usuario.invalido", ex.getMessage());
			adicionarOpcoesFormularioCriacao(model);
			return "sysadmin/usuarios/formulario";
		}
		redirectAttributes.addFlashAttribute("sucesso", "Usuario criado com sucesso. No primeiro acesso, o usuario devera utilizar o e-mail cadastrado como login e senha inicial.");
		redirectAttributes.addFlashAttribute("loginCriado", request.login() == null ? "" : request.login().trim());
		return "redirect:/sysadmin/usuarios";
	}

	@PostMapping("/sysadmin/usuarios/{id}/editar")
	public String atualizar(
			@PathVariable Long id,
			@Valid @ModelAttribute("usuario") UsuarioEditRequest request,
			BindingResult bindingResult,
			Model model,
			Authentication authentication,
			RedirectAttributes redirectAttributes
	) {
		WebModelSupport.addUserAttributes(model, authentication);
		model.addAttribute("pageTitle", "Editar usuário");
		model.addAttribute("usuarioId", id);
		if (bindingResult.hasErrors()) {
			adicionarOpcoesFormularioEdicao(model, id);
			return "sysadmin/usuarios/editar";
		}
		try {
			usuarioService().atualizar(id, request, currentUserId(authentication));
		} catch (BusinessException | ResourceNotFoundException ex) {
			bindingResult.reject("usuario.invalido", ex.getMessage());
			adicionarOpcoesFormularioEdicao(model, id);
			return "sysadmin/usuarios/editar";
		}
		redirectAttributes.addFlashAttribute("sucesso", "Usuário atualizado com sucesso.");
		return "redirect:/sysadmin/usuarios";
	}

	@PostMapping("/sysadmin/usuarios/{id}/desativar")
	public String desativar(
			@PathVariable Long id,
			Authentication authentication,
			RedirectAttributes redirectAttributes
	) {
		try {
			usuarioService().desativar(id, currentUserId(authentication));
			redirectAttributes.addFlashAttribute("sucesso", "Usuário desativado com sucesso.");
		} catch (BusinessException | ResourceNotFoundException ex) {
			redirectAttributes.addFlashAttribute("erro", ex.getMessage());
		}
		return "redirect:/sysadmin/usuarios";
	}

	@PostMapping("/sysadmin/usuarios/{id}/ativar")
	public String ativar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		try {
			usuarioService().ativar(id);
			redirectAttributes.addFlashAttribute("sucesso", "Usuário ativado com sucesso.");
		} catch (BusinessException | ResourceNotFoundException ex) {
			redirectAttributes.addFlashAttribute("erro", ex.getMessage());
		}
		return "redirect:/sysadmin/usuarios";
	}

	@PostMapping("/sysadmin/usuarios/{id}/resetar-senha")
	public String resetarSenha(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		try {
			PasswordResetResult result = passwordLifecycleService.getObject().resetarSenha(id);
			redirectAttributes.addFlashAttribute("sucesso", "Senha temporaria gerada. Informe ao usuario e solicite a troca no proximo acesso.");
			redirectAttributes.addFlashAttribute("senhaTemporaria", result.senhaTemporaria());
			redirectAttributes.addFlashAttribute("senhaTemporariaUsuarioId", result.usuarioId());
		} catch (BusinessException | ResourceNotFoundException ex) {
			redirectAttributes.addFlashAttribute("erro", ex.getMessage());
		}
		return "redirect:/sysadmin/usuarios";
	}

	private void adicionarOpcoesFormularioCriacao(Model model) {
		model.addAttribute("tipos", TipoUsuario.values());
		model.addAttribute("clinicas", usuarioService().listarClinicasAtivas());
		model.addAttribute("veterinarios", usuarioService().listarVeterinariosDisponiveisParaCriacao());
		model.addAttribute("responsaveis", usuarioService().listarResponsaveisDisponiveisParaCriacao());
	}

	private void adicionarOpcoesFormularioEdicao(Model model, Long usuarioId) {
		model.addAttribute("tipos", TipoUsuario.values());
		model.addAttribute("clinicas", usuarioService().listarClinicasAtivas());
		model.addAttribute("veterinarios", usuarioService().listarVeterinariosDisponiveisParaEdicao(usuarioId));
		model.addAttribute("responsaveis", usuarioService().listarResponsaveisDisponiveisParaEdicao(usuarioId));
	}

	private UsuarioProvisioningRequest novoRequest() {
		return new UsuarioProvisioningRequest("", null, "", null, null, null);
	}

	private UsuarioEditRequest editarRequest(UsuarioResponse usuario) {
		return new UsuarioEditRequest(
				usuario.nome(),
				usuario.tipo(),
				usuario.login(),
				usuario.responsavelId(),
				usuario.veterinarioId(),
				usuario.clinicaId()
		);
	}

	private Long currentUserId(Authentication authentication) {
		if (authentication != null && authentication.getPrincipal() instanceof UsuarioPrincipal usuarioPrincipal) {
			return usuarioPrincipal.getUsuarioId();
		}
		return null;
	}

	private UsuarioService usuarioService() {
		return usuarioService.getObject();
	}

	private String normalizedSortField(String sortField) {
		return SORT_PROPERTIES.containsKey(sortField) ? sortField : DEFAULT_SORT_FIELD;
	}

	private Sort.Direction normalizedSortDirection(String sortDir) {
		return "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
	}

	private Sort sort(String sortField, Sort.Direction direction) {
		Sort sort = Sort.by(direction, SORT_PROPERTIES.get(sortField));
		if (!DEFAULT_SORT_FIELD.equals(sortField)) {
			sort = sort.and(Sort.by(Sort.Direction.DESC, "dataCadastro"));
		}
		return sort.and(Sort.by(Sort.Direction.DESC, "id"));
	}

	private enum PerfilFiltro {
		TODOS("TODOS", "Todos", List.of()),
		ADMINISTRATIVOS("ADMINISTRATIVOS", "Administrativos", List.of(TipoUsuario.SYSADMIN, TipoUsuario.ADMIN_CLINICA)),
		SYSADMIN("SYSADMIN", "SysAdmin", List.of(TipoUsuario.SYSADMIN)),
		ADMIN_CLINICA("ADMIN_CLINICA", "Administrador da Clínica", List.of(TipoUsuario.ADMIN_CLINICA)),
		VETERINARIO("VETERINARIO", "Veterinário", List.of(TipoUsuario.VETERINARIO)),
		RESPONSAVEL("RESPONSAVEL", "Responsável", List.of(TipoUsuario.RESPONSAVEL));

		private final String value;
		private final String label;
		private final List<TipoUsuario> tipos;

		PerfilFiltro(String value, String label, List<TipoUsuario> tipos) {
			this.value = value;
			this.label = label;
			this.tipos = tipos;
		}

		static PerfilFiltro from(String value) {
			if (value == null || value.isBlank()) {
				return TODOS;
			}
			String normalized = value.toUpperCase(Locale.ROOT);
			for (PerfilFiltro filtro : values()) {
				if (filtro.value.equals(normalized)) {
					return filtro;
				}
			}
			return TODOS;
		}

		public String getValue() {
			return value;
		}

		public String getLabel() {
			return label;
		}

		List<TipoUsuario> tipos() {
			return tipos;
		}
	}

}
