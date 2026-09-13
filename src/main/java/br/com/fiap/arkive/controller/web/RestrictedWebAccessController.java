package br.com.fiap.arkive.controller.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RestrictedWebAccessController {

	@GetMapping("/acesso-web-restrito")
	public String restricted(Model model, Authentication authentication) {
		WebModelSupport.addUserAttributes(model, authentication);
		model.addAttribute("pageTitle", "Acesso Web Restrito");
		return "acesso-web-restrito";
	}

}
