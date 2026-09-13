package br.com.fiap.arkive.domain.transcricao;

import br.com.fiap.arkive.exception.BusinessException;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public enum SupportedAudioFormat {
	WAV(Set.of("wav"), Set.of("audio/wav", "audio/x-wav", "audio/wave", "audio/vnd.wave"), ".wav", false),
	M4A(Set.of("m4a", "mp4"), Set.of("audio/mp4", "audio/m4a", "audio/x-m4a", "video/mp4"), ".m4a", true),
	AAC(Set.of("aac"), Set.of("audio/aac", "audio/aacp", "audio/x-aac"), ".aac", true),
	WEBM(Set.of("webm"), Set.of("audio/webm"), ".webm", true);

	private static final Set<String> GENERIC_CONTENT_TYPES = Set.of("application/octet-stream", "binary/octet-stream");

	private final Set<String> extensions;
	private final Set<String> contentTypes;
	private final String tempFileExtension;
	private final boolean conversionRequired;

	SupportedAudioFormat(Set<String> extensions, Set<String> contentTypes, String tempFileExtension, boolean conversionRequired) {
		this.extensions = extensions;
		this.contentTypes = contentTypes;
		this.tempFileExtension = tempFileExtension;
		this.conversionRequired = conversionRequired;
	}

	public String defaultContentType() {
		return contentTypes.iterator().next();
	}

	public String tempFileExtension() {
		return tempFileExtension;
	}

	public boolean isConversionRequired() {
		return conversionRequired;
	}

	public static SupportedAudioFormat resolve(String filename, String contentType) {
		Optional<SupportedAudioFormat> byExtension = fromExtension(filename);
		Optional<SupportedAudioFormat> byContentType = fromContentType(contentType);
		String normalizedContentType = normalizeContentType(contentType);

		if (byExtension.isPresent() && byContentType.isPresent() && byExtension.get() != byContentType.get()) {
			throw new BusinessException("Extensao e tipo de conteudo do audio sao incompativeis.");
		}
		if (byExtension.isPresent()) {
			if (hasSpecificContentType(normalizedContentType) && byContentType.isEmpty()) {
				throw new BusinessException("Tipo de conteudo do audio nao suportado.");
			}
			return byExtension.get();
		}
		return byContentType.orElseThrow(() -> new BusinessException("Formato de audio nao suportado para transcricao. Envie arquivo WAV, M4A/MP4, AAC ou WebM."));
	}

	private static Optional<SupportedAudioFormat> fromExtension(String filename) {
		String extension = extension(filename);
		if (extension.isBlank()) {
			return Optional.empty();
		}
		return Arrays.stream(values())
				.filter(format -> format.extensions.contains(extension))
				.findFirst();
	}

	private static Optional<SupportedAudioFormat> fromContentType(String contentType) {
		String normalized = normalizeContentType(contentType);
		if (normalized.isBlank() || GENERIC_CONTENT_TYPES.contains(normalized)) {
			return Optional.empty();
		}
		return Arrays.stream(values())
				.filter(format -> format.contentTypes.contains(normalized))
				.findFirst();
	}

	private static String extension(String filename) {
		if (filename == null || filename.isBlank()) {
			return "";
		}
		int index = filename.lastIndexOf('.');
		if (index < 0 || index == filename.length() - 1) {
			return "";
		}
		return filename.substring(index + 1).toLowerCase(Locale.ROOT);
	}

	private static String normalizeContentType(String contentType) {
		if (contentType == null || contentType.isBlank()) {
			return "";
		}
		return contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
	}

	private static boolean hasSpecificContentType(String contentType) {
		return !contentType.isBlank() && !GENERIC_CONTENT_TYPES.contains(contentType);
	}
}
