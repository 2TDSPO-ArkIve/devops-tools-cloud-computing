package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.service.HealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Verificacao publica de disponibilidade da API.")
public class HealthController {

	private final HealthService healthService;

	public HealthController(HealthService healthService) {
		this.healthService = healthService;
	}

	@GetMapping
	@SecurityRequirements
	@Operation(summary = "Health check", description = "Endpoint publico para verificar se a API esta ativa.")
	public Map<String, String> health() {
		return healthService.getHealth();
	}

}
