package br.com.fiap.arkive.controller;

import br.com.fiap.arkive.dto.response.TranscricaoResponse;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.TranscricaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/transcricoes")
@Profile("!local-nodb")
@Tag(name = "Transcricoes", description = "Transcricao estateless de audio clinico em texto editavel.")
public class TranscricaoController {

	private final TranscricaoService transcricaoService;

	public TranscricaoController(TranscricaoService transcricaoService) {
		this.transcricaoService = transcricaoService;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Transcreve audio clinico", description = "Recebe um arquivo de audio e retorna apenas o texto transcrito, sem persistir narrativa ou acionar IA clinica.")
	public TranscricaoResponse transcrever(
			@RequestPart(required = false) MultipartFile audio,
			@RequestParam(required = false) String idioma,
			@AuthenticationPrincipal UsuarioPrincipal principal
	) {
		return transcricaoService.transcrever(audio, idioma, principal);
	}
}
