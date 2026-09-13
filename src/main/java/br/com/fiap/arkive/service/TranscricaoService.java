package br.com.fiap.arkive.service;

import br.com.fiap.arkive.config.AzureSpeechProperties;
import br.com.fiap.arkive.domain.transcricao.IdiomaTranscricao;
import br.com.fiap.arkive.domain.transcricao.SupportedAudioFormat;
import br.com.fiap.arkive.dto.response.TranscricaoResponse;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.speech.SpeechTranscriptionGateway;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class TranscricaoService {

	private final SpeechTranscriptionGateway speechTranscriptionGateway;
	private final AzureSpeechProperties properties;

	public TranscricaoService(SpeechTranscriptionGateway speechTranscriptionGateway, AzureSpeechProperties properties) {
		this.speechTranscriptionGateway = speechTranscriptionGateway;
		this.properties = properties;
	}

	public TranscricaoResponse transcrever(MultipartFile audio, String idioma, UsuarioPrincipal principal) {
		exigirVeterinarioAutenticado(principal);
		IdiomaTranscricao idiomaTranscricao = IdiomaTranscricao.fromCodigoOrDefault(idioma);
		AudioUpload upload = validarAudio(audio);
		String transcricao = speechTranscriptionGateway.transcrever(upload.bytes(), upload.filename(), upload.contentType(), upload.format(), idiomaTranscricao);
		if (transcricao == null || transcricao.isBlank()) {
			throw new BusinessException("Nenhuma fala foi reconhecida. Tente novamente.", HttpStatus.UNPROCESSABLE_ENTITY);
		}
		return new TranscricaoResponse(transcricao.trim(), idiomaTranscricao.getCodigo());
	}

	private AudioUpload validarAudio(MultipartFile audio) {
		if (audio == null || audio.isEmpty()) {
			throw new BusinessException("Arquivo de audio deve ser informado.");
		}
		if (audio.getSize() > properties.getMaxUploadSize().toBytes()) {
			throw new BusinessException("Arquivo de audio excede o limite permitido.", HttpStatus.PAYLOAD_TOO_LARGE);
		}
		SupportedAudioFormat format = SupportedAudioFormat.resolve(audio.getOriginalFilename(), audio.getContentType());
		try {
			byte[] bytes = audio.getBytes();
			if (bytes.length == 0) {
				throw new BusinessException("Arquivo de audio deve ser informado.");
			}
			if (SupportedAudioFormat.WAV.equals(format)) {
				validarCabecalhoWav(bytes);
			}
			return new AudioUpload(bytes, safeFilename(audio.getOriginalFilename()), audio.getContentType() == null ? format.defaultContentType() : audio.getContentType(), format);
		} catch (IOException ex) {
			throw new BusinessException("Nao foi possivel ler o arquivo de audio.", HttpStatus.BAD_REQUEST);
		}
	}

	private void validarCabecalhoWav(byte[] bytes) {
		if (bytes.length < 12
				|| bytes[0] != 'R'
				|| bytes[1] != 'I'
				|| bytes[2] != 'F'
				|| bytes[3] != 'F'
				|| bytes[8] != 'W'
				|| bytes[9] != 'A'
				|| bytes[10] != 'V'
				|| bytes[11] != 'E') {
			throw new BusinessException("Arquivo de audio deve ser WAV valido.");
		}
	}

	private String safeFilename(String filename) {
		String cleanFilename = StringUtils.cleanPath(filename == null ? "audio" : filename);
		int slash = Math.max(cleanFilename.lastIndexOf('/'), cleanFilename.lastIndexOf('\\'));
		return slash >= 0 ? cleanFilename.substring(slash + 1) : cleanFilename;
	}

	private void exigirVeterinarioAutenticado(UsuarioPrincipal principal) {
		if (principal == null || !TipoUsuario.VETERINARIO.equals(principal.getTipoUsuario()) || principal.getVeterinarioId() == null) {
			throw new AccessDeniedException("Transcricao permitida apenas ao veterinario autenticado.");
		}
	}

	private record AudioUpload(byte[] bytes, String filename, String contentType, SupportedAudioFormat format) {
	}
}
