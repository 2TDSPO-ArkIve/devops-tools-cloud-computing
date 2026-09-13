package br.com.fiap.arkive.service.speech;

public record SpeechRecognitionSegment(
		boolean recognizedSpeech,
		String text
) {
}
