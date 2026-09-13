package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.response.AnimalHistoricoResponse;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.AnimalHistoricoPdfService;
import br.com.fiap.arkive.service.AnimalHistoricoService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/animais")
@Profile("!local-nodb")
public class AnimalHistoricoController {
	private final AnimalHistoricoService historicoService;
	private final AnimalHistoricoPdfService pdfService;

	public AnimalHistoricoController(AnimalHistoricoService historicoService, AnimalHistoricoPdfService pdfService) {
		this.historicoService = historicoService;
		this.pdfService = pdfService;
	}

	@GetMapping("/{id}/historico")
	@Operation(summary = "Consulta historico clinico do paciente", description = "Consultas autorizadas, mais recentes primeiro. Dados clinicos finais apenas em consultas FI.")
	public ResponseEntity<AnimalHistoricoResponse> buscar(@PathVariable Long id,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(historicoService.buscar(id, principal));
	}

	@GetMapping(value = "/{id}/historico-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
	@Operation(summary = "Exporta historico clinico do paciente", description = "PDF privado com o mesmo conteudo autorizado do historico JSON.")
	public ResponseEntity<byte[]> exportar(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal principal) {
		var pdf = pdfService.gerar(id, principal);
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(pdf.filename()).build().toString())
				.cacheControl(CacheControl.noStore()).body(pdf.bytes());
	}
}
