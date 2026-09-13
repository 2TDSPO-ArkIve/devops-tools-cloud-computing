package br.com.fiap.arkive.service.speech;

import br.com.fiap.arkive.config.AzureSpeechProperties;
import br.com.fiap.arkive.domain.transcricao.IdiomaTranscricao;
import br.com.fiap.arkive.exception.BusinessException;
import com.microsoft.cognitiveservices.speech.CancellationReason;
import com.microsoft.cognitiveservices.speech.OutputFormat;
import com.microsoft.cognitiveservices.speech.PhraseListGrammar;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Component
public class AzureContinuousSpeechRecognizerFactory implements ContinuousSpeechRecognizerFactory {

	private final AzureSpeechProperties properties;
	private final List<String> veterinaryPhrases;

	public AzureContinuousSpeechRecognizerFactory(
			AzureSpeechProperties properties,
			@Qualifier("azureSpeechVeterinaryPhrases") List<String> veterinaryPhrases
	) {
		this.properties = properties;
		this.veterinaryPhrases = veterinaryPhrases;
	}

	@Override
	public ContinuousSpeechRecognizer create(Path wavFile, IdiomaTranscricao idioma) {
		SpeechConfig speechConfig = null;
		AudioConfig audioConfig = null;
		SpeechRecognizer recognizer = null;
		PhraseListGrammar phraseList = null;
		try {
			speechConfig = SpeechConfig.fromEndpoint(URI.create(properties.getEndpoint()), properties.getApiKey());
			speechConfig.setSpeechRecognitionLanguage(idioma.getCodigo());
			speechConfig.setOutputFormat(OutputFormat.Simple);
			audioConfig = AudioConfig.fromWavFileInput(wavFile.toString());
			recognizer = new SpeechRecognizer(speechConfig, idioma.getCodigo(), audioConfig);
			phraseList = PhraseListGrammar.fromRecognizer(recognizer);
			applyPhraseList(phraseList);
			return new AzureContinuousSpeechRecognizer(properties, speechConfig, audioConfig, recognizer, phraseList);
		} catch (IllegalArgumentException ex) {
			closeQuietly(phraseList, recognizer, audioConfig, speechConfig);
			throw new BusinessException("Configuracao do servico de transcricao invalida.", HttpStatus.SERVICE_UNAVAILABLE);
		} catch (RuntimeException ex) {
			closeQuietly(phraseList, recognizer, audioConfig, speechConfig);
			throw new BusinessException("Servico de transcricao temporariamente indisponivel.", HttpStatus.SERVICE_UNAVAILABLE);
		}
	}

	private void closeQuietly(AutoCloseable... resources) {
		for (AutoCloseable resource : resources) {
			if (resource == null) {
				continue;
			}
			try {
				resource.close();
			} catch (Exception ignored) {
				// Best-effort cleanup only. Do not log SDK configuration or audio paths.
			}
		}
	}

	private void applyPhraseList(PhraseListGrammar phraseList) {
		for (String phrase : veterinaryPhrases) {
			phraseList.addPhrase(phrase);
		}
		phraseList.setWeight(properties.getPhraseListBiasingWeight());
	}

	private static final class AzureContinuousSpeechRecognizer implements ContinuousSpeechRecognizer {

		private final AzureSpeechProperties properties;
		private final SpeechConfig speechConfig;
		private final AudioConfig audioConfig;
		private final SpeechRecognizer recognizer;
		private final PhraseListGrammar phraseList;

		private AzureContinuousSpeechRecognizer(
				AzureSpeechProperties properties,
				SpeechConfig speechConfig,
				AudioConfig audioConfig,
				SpeechRecognizer recognizer,
				PhraseListGrammar phraseList
		) {
			this.properties = properties;
			this.speechConfig = speechConfig;
			this.audioConfig = audioConfig;
			this.recognizer = recognizer;
			this.phraseList = phraseList;
		}

		@Override
		public void onRecognized(Consumer<SpeechRecognitionSegment> listener) {
			recognizer.recognized.addEventListener((sender, event) -> {
				SpeechRecognitionResult result = event.getResult();
				if (result == null) {
					return;
				}
				try {
					boolean recognized = ResultReason.RecognizedSpeech.equals(result.getReason());
					listener.accept(new SpeechRecognitionSegment(recognized, result.getText()));
				} finally {
					result.close();
				}
			});
		}

		@Override
		public void onCanceled(Consumer<SpeechRecognitionCancellation> listener) {
			recognizer.canceled.addEventListener((sender, event) ->
					listener.accept(new SpeechRecognitionCancellation(
							CancellationReason.Error.equals(event.getReason()),
							event.getErrorCode()
					))
			);
		}

		@Override
		public void onSessionStopped(Runnable listener) {
			recognizer.sessionStopped.addEventListener((sender, event) -> listener.run());
		}

		@Override
		public void start() {
			waitForSdkOperation(recognizer.startContinuousRecognitionAsync());
		}

		@Override
		public void stop() {
			waitForSdkOperation(recognizer.stopContinuousRecognitionAsync());
		}

		@Override
		public void close() {
			phraseList.close();
			recognizer.close();
			audioConfig.close();
			speechConfig.close();
		}

		private void waitForSdkOperation(Future<Void> future) {
			try {
				future.get(properties.getSdkOperationTimeout().toMillis(), TimeUnit.MILLISECONDS);
			} catch (TimeoutException ex) {
				future.cancel(true);
				throw new BusinessException("Servico de transcricao temporariamente indisponivel.", HttpStatus.SERVICE_UNAVAILABLE);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new BusinessException("Servico de transcricao temporariamente indisponivel.", HttpStatus.SERVICE_UNAVAILABLE);
			} catch (ExecutionException ex) {
				throw new BusinessException("Servico de transcricao temporariamente indisponivel.", HttpStatus.SERVICE_UNAVAILABLE);
			}
		}
	}
}
