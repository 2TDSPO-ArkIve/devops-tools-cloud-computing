package br.com.fiap.arkive.controller.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

	@GetMapping("/")
	public String home(Authentication authentication) {
		return "redirect:" + WebModelSupport.roleLandingPath(authentication);
	}

}
