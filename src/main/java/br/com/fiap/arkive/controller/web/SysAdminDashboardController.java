package br.com.fiap.arkive.controller.web;

import br.com.fiap.arkive.dto.response.SysAdminDashboardView;
import br.com.fiap.arkive.service.SysAdminDashboardService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SysAdminDashboardController {

	private final ObjectProvider<SysAdminDashboardService> dashboardService;

	public SysAdminDashboardController(ObjectProvider<SysAdminDashboardService> dashboardService) {
		this.dashboardService = dashboardService;
	}

	@GetMapping("/sysadmin/dashboard")
	public String dashboard(Model model, Authentication authentication) {
		WebModelSupport.addUserAttributes(model, authentication);
		model.addAttribute("pageTitle", "Visão Geral");
		model.addAttribute("sectionTitle", "Administração Global");
		SysAdminDashboardService service = dashboardService.getIfAvailable();
		model.addAttribute("dashboard", service == null ? SysAdminDashboardView.empty() : service.carregar());
		return "sysadmin/dashboard";
	}

}
