package br.com.fiap.arkive.service.speech;

import br.com.fiap.arkive.config.AzureSpeechProperties;
import br.com.fiap.arkive.domain.transcricao.IdiomaTranscricao;
import br.com.fiap.arkive.domain.transcricao.SupportedAudioFormat;
import br.com.fiap.arkive.exception.BusinessException;
import com.microsoft.cognitiveservices.speech.CancellationErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class AzureSpeechTranscriptionService implements SpeechTranscriptionGateway {

	private final AzureSpeechProperties properties;
	private final AudioConversionService audioConversionService;
	private final ContinuousSpeechRecognizerFactory recognizerFactory;
	private final Semaphore recognitionSlots;

	public AzureSpeechTranscriptionService(
			AzureSpeechProperties properties,
			AudioConversionService audioConversionService,
			ContinuousSpeechRecognizerFactory recognizerFactory
	) {
		this.properties = properties;
		this.audioConversionService = audioConversionService;
		this.recognizerFactory = recognizerFactory;
		this.recognitionSlots = new Semaphore(Math.max(1, properties.getMaxConcurrentRequests()));
	}

	@Override
	public String transcrever(byte[] audio, String filename, String contentType, SupportedAudioFormat format, IdiomaTranscricao idioma) {
		if (!properties.isConfigured()) {
			throw new BusinessException("Transcricao de audio indisponivel por configuracao do servidor.", HttpStatus.SERVICE_UNAVAILABLE);
		}
		try (PreparedAudio preparedAudio = audioConversionService.prepararWav(audio, format)) {
			return recognizeWithQuota(preparedAudio.wavFile(), idioma);
		}
	}

	private String recognizeWithQuota(Path wavFile, IdiomaTranscricao idioma) {
		if (!recognitionSlots.tryAcquire()) {
			throw new BusinessException("Limite do servico de transcricao atingido. Tente novamente mais tarde.", HttpStatus.TOO_MANY_REQUESTS);
		}
		try {
			return recognizeContinuous(wavFile, idioma);
		} finally {
			recognitionSlots.release();
		}
	}

	private String recognizeContinuous(Path wavFile, IdiomaTranscricao idioma) {
		StringBuilder transcript = new StringBuilder();
		CountDownLatch completed = new CountDownLatch(1);
		AtomicReference<BusinessException> failure = new AtomicReference<>();

		try (ContinuousSpeechRecognizer recognizer = recognizerFactory.create(wavFile, idioma)) {
			recognizer.onRecognized(segment -> appendFinalSegment(transcript, segment));
			recognizer.onCanceled(cancellation -> {
				if (cancellation.error()) {
					failure.compareAndSet(null, mapCancellationErrorCode(cancellation.errorCode()));
				}
				completed.countDown();
			});
			recognizer.onSessionStopped(completed::countDown);
			recognizer.start();

			boolean finished = awaitCompletion(completed);
			try {
				recognizer.stop();
			} catch (BusinessException ex) {
				if (finished) {
					throw ex;
				}
			}
			if (!finished) {
				throw new BusinessException("Servico de transcricao temporariamente indisponivel.", HttpStatus.SERVICE_UNAVAILABLE);
			}
			BusinessException sdkFailure = failure.get();
			if (sdkFailure != null) {
				throw sdkFailure;
			}
			String cleanTranscript = transcript.toString().trim();
			if (cleanTranscript.isBlank()) {
				throw noSpeech();
			}
			return cleanTranscript;
		} catch (BusinessException ex) {
			throw ex;
		} catch (RuntimeException ex) {
			throw new BusinessException("Servico de transcricao temporariamente indisponivel.", HttpStatus.SERVICE_UNAVAILABLE);
		}
	}

	private void appendFinalSegment(StringBuilder transcript, SpeechRecognitionSegment segment) {
		if (segment == null || !segment.recognizedSpeech() || segment.text() == null || segment.text().isBlank()) {
			return;
		}
		if (!transcript.isEmpty()) {
			transcript.append(' ');
		}
		transcript.append(segment.text().trim());
	}

	private boolean awaitCompletion(CountDownLatch completed) {
		try {
			return completed.await(properties.getRecognitionTimeout().toMillis(), TimeUnit.MILLISECONDS);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new BusinessException("Servico de transcricao temporariamente indisponivel.", HttpStatus.SERVICE_UNAVAILABLE);
		}
	}

	private BusinessException noSpeech() {
		return new BusinessException("Nenhuma fala foi reconhecida. Tente novamente.", HttpStatus.UNPROCESSABLE_ENTITY);
	}

	private static BusinessException mapCancellationErrorCode(CancellationErrorCode errorCode) {
		if (CancellationErrorCode.BadRequest.equals(errorCode)) {
			return new BusinessException("Audio invalido ou em formato nao suportado para transcricao.", HttpStatus.BAD_REQUEST);
		}
		if (CancellationErrorCode.AuthenticationFailure.equals(errorCode)) {
			return new BusinessException("Transcricao de audio indisponivel por configuracao do servidor.", HttpStatus.SERVICE_UNAVAILABLE);
		}
		if (CancellationErrorCode.Forbidden.equals(errorCode) || CancellationErrorCode.TooManyRequests.equals(errorCode)) {
			return new BusinessException("Limite do servico de transcricao atingido. Tente novamente mais tarde.", HttpStatus.TOO_MANY_REQUESTS);
		}
		if (CancellationErrorCode.ConnectionFailure.equals(errorCode)
				|| CancellationErrorCode.ServiceTimeout.equals(errorCode)
				|| CancellationErrorCode.ServiceUnavailable.equals(errorCode)) {
			return new BusinessException("Servico de transcricao temporariamente indisponivel.", HttpStatus.SERVICE_UNAVAILABLE);
		}
		return new BusinessException("Falha ao consultar o servico de transcricao.", HttpStatus.BAD_GATEWAY);
	}

	static String mapCancellationForTest(CancellationErrorCode errorCode) {
		return mapCancellationErrorCode(errorCode)
				.getStatus()
				.name();
	}
}
