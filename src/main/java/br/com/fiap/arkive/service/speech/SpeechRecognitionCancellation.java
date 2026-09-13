package br.com.fiap.arkive.service.speech;

import com.microsoft.cognitiveservices.speech.CancellationErrorCode;

public record SpeechRecognitionCancellation(
		boolean error,
		CancellationErrorCode errorCode
) {
}
