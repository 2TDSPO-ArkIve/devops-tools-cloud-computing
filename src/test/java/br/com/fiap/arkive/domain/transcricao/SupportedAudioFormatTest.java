package br.com.fiap.arkive.domain.transcricao;

import br.com.fiap.arkive.exception.BusinessException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class SupportedAudioFormatTest {
	@ParameterizedTest
	@CsvSource({"audio.webm,audio/webm,WEBM", "audio.webm,audio/webm;codecs=opus,WEBM",
			"audio,audio/webm;codecs=opus,WEBM", "audio.wav,audio/wav,WAV",
			"audio.m4a,audio/mp4,M4A", "audio.mp4,video/mp4,M4A", "audio.aac,audio/aac,AAC"})
	void resolveFormatosSuportados(String nome, String mime, SupportedAudioFormat esperado) {
		assertEquals(esperado, SupportedAudioFormat.resolve(nome, mime));
	}

	@ParameterizedTest
	@CsvSource({"audio.webm,audio/mp4", "audio.m4a,audio/webm", "audio.wav,audio/webm;codecs=opus",
			"audio.webm,video/webm", "audio.ogg,audio/ogg"})
	void rejeitaFormatosIncompativeis(String nome, String mime) {
		assertThrows(BusinessException.class, () -> SupportedAudioFormat.resolve(nome, mime));
	}
}
