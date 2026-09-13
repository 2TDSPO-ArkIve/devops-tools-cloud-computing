package br.com.fiap.arkive.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
		LocalDateTime timestamp,
		int status,
		String error,
		String message,
		String path,
		Map<String, String> fields
) {
	public ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path) {
		this(timestamp, status, error, message, path, null);
	}
}
