package br.com.fiap.arkive.service.speech;

import br.com.fiap.arkive.config.AzureSpeechProperties;
import br.com.fiap.arkive.domain.transcricao.IdiomaTranscricao;
import br.com.fiap.arkive.domain.transcricao.SupportedAudioFormat;
import br.com.fiap.arkive.exception.BusinessException;
import com.microsoft.cognitiveservices.speech.CancellationErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AzureSpeechTranscriptionServiceTest {

	@TempDir
	private Path tempDir;

	private AzureSpeechProperties properties;
	private AudioConversionService audioConversionService;
	private FakeRecognizer recognizer;
	private AzureSpeechTranscriptionService service;

	@BeforeEach
	void setUp() {
		properties = new AzureSpeechProperties();
		properties.setEndpoint("https://eastus.api.cognitive.microsoft.com/");
		properties.setApiKey("test-key");
		properties.setRecognitionTimeout(Duration.ofSeconds(1));
		audioConversionService = mock(AudioConversionService.class);
		recognizer = new FakeRecognizer();
		service = new AzureSpeechTranscriptionService(properties, audioConversionService, (wavFile, idioma) -> recognizer);
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
	void webmUsaConversaoExistenteELimpaMesmoQuandoAzureFalha(boolean falha) throws Exception {
		Path wav = java.nio.file.Files.createFile(tempDir.resolve("webm-convertido.wav"));
		when(audioConversionService.prepararWav(any(), eq(SupportedAudioFormat.WEBM))).thenReturn(new PreparedAudio(wav, List.of(wav)));
		recognizer.onStart = () -> {
			if (falha) recognizer.emitCanceled(true, CancellationErrorCode.ServiceUnavailable);
			else { recognizer.emitRecognized(true, "Narrativa"); recognizer.emitSessionStopped(); }
		};
		if (falha) assertThrows(BusinessException.class, () -> service.transcrever(new byte[] {1}, "audio.webm", "audio/webm", SupportedAudioFormat.WEBM, IdiomaTranscricao.PT_BR));
		else assertEquals("Narrativa", service.transcrever(new byte[] {1}, "audio.webm", "audio/webm", SupportedAudioFormat.WEBM, IdiomaTranscricao.PT_BR));
		org.junit.jupiter.api.Assertions.assertFalse(java.nio.file.Files.exists(wav));
	}

	@Test
	void acumulaSegmentosFinaisNaOrdemAteSessionStopped() throws Exception {
		Path wav = tempDir.resolve("entrada.wav");
		when(audioConversionService.prepararWav(any(), eq(SupportedAudioFormat.WAV)))
				.thenReturn(new PreparedAudio(wav, List.of()));
		recognizer.onStart = () -> {
			recognizer.emitRecognized(true, "Paciente canino apresenta claudicacao.");
			recognizer.emitRecognized(false, "resultado parcial ignorado");
			recognizer.emitRecognized(true, "Tutor relata uso previo de prednisolona.");
			recognizer.emitSessionStopped();
		};

		String transcricao = service.transcrever(new byte[] { 1 }, "consulta.wav", "audio/wav", SupportedAudioFormat.WAV, IdiomaTranscricao.PT_BR);

		assertEquals("Paciente canino apresenta claudicacao. Tutor relata uso previo de prednisolona.", transcricao);
		assertTrue(recognizer.stopped);
	}

	@Test
	void cancelamentoComErroEncerraComStatusMapeado() throws Exception {
		Path wav = tempDir.resolve("entrada.wav");
		when(audioConversionService.prepararWav(any(), eq(SupportedAudioFormat.WAV)))
				.thenReturn(new PreparedAudio(wav, List.of()));
		recognizer.onStart = () -> recognizer.emitCanceled(true, CancellationErrorCode.ServiceTimeout);

		BusinessException exception = assertThrows(BusinessException.class,
				() -> service.transcrever(new byte[] { 1 }, "consulta.wav", "audio/wav", SupportedAudioFormat.WAV, IdiomaTranscricao.PT_BR));

		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
		assertTrue(recognizer.stopped);
	}

	@Test
	void timeoutSemEventoDeConclusaoRetorna503() throws Exception {
		properties.setRecognitionTimeout(Duration.ofMillis(10));
		Path wav = tempDir.resolve("entrada.wav");
		when(audioConversionService.prepararWav(any(), eq(SupportedAudioFormat.WAV)))
				.thenReturn(new PreparedAudio(wav, List.of()));

		BusinessException exception = assertThrows(BusinessException.class,
				() -> service.transcrever(new byte[] { 1 }, "consulta.wav", "audio/wav", SupportedAudioFormat.WAV, IdiomaTranscricao.PT_BR));

		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
		assertTrue(recognizer.stopped);
	}

	@Test
	void sessionStoppedSemTextoRetorna422() throws Exception {
		Path wav = tempDir.resolve("entrada.wav");
		when(audioConversionService.prepararWav(any(), eq(SupportedAudioFormat.WAV)))
				.thenReturn(new PreparedAudio(wav, List.of()));
		recognizer.onStart = recognizer::emitSessionStopped;

		BusinessException exception = assertThrows(BusinessException.class,
				() -> service.transcrever(new byte[] { 1 }, "consulta.wav", "audio/wav", SupportedAudioFormat.WAV, IdiomaTranscricao.PT_BR));

		assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
	}

	@Test
	void mapeiaFalhasConhecidasDoSdk() {
		assertEquals(HttpStatus.BAD_REQUEST.name(), AzureSpeechTranscriptionService.mapCancellationForTest(CancellationErrorCode.BadRequest));
		assertEquals(HttpStatus.SERVICE_UNAVAILABLE.name(), AzureSpeechTranscriptionService.mapCancellationForTest(CancellationErrorCode.AuthenticationFailure));
		assertEquals(HttpStatus.TOO_MANY_REQUESTS.name(), AzureSpeechTranscriptionService.mapCancellationForTest(CancellationErrorCode.TooManyRequests));
		assertEquals(HttpStatus.SERVICE_UNAVAILABLE.name(), AzureSpeechTranscriptionService.mapCancellationForTest(CancellationErrorCode.ServiceTimeout));
		assertEquals(HttpStatus.BAD_GATEWAY.name(), AzureSpeechTranscriptionService.mapCancellationForTest(CancellationErrorCode.ServiceError));
	}

	private static class FakeRecognizer implements ContinuousSpeechRecognizer {

		private Consumer<SpeechRecognitionSegment> recognizedListener = segment -> {
		};
		private Consumer<SpeechRecognitionCancellation> canceledListener = cancellation -> {
		};
		private Runnable sessionStoppedListener = () -> {
		};
		private Runnable onStart = () -> {
		};
		private boolean stopped;

		@Override
		public void onRecognized(Consumer<SpeechRecognitionSegment> listener) {
			this.recognizedListener = listener;
		}

		@Override
		public void onCanceled(Consumer<SpeechRecognitionCancellation> listener) {
			this.canceledListener = listener;
		}

		@Override
		public void onSessionStopped(Runnable listener) {
			this.sessionStoppedListener = listener;
		}

		@Override
		public void start() {
			onStart.run();
		}

		@Override
		public void stop() {
			stopped = true;
		}

		@Override
		public void close() {
		}

		private void emitRecognized(boolean finalResult, String text) {
			recognizedListener.accept(new SpeechRecognitionSegment(finalResult, text));
		}

		private void emitCanceled(boolean error, CancellationErrorCode errorCode) {
			canceledListener.accept(new SpeechRecognitionCancellation(error, errorCode));
		}

		private void emitSessionStopped() {
			sessionStoppedListener.run();
		}
	}
}
