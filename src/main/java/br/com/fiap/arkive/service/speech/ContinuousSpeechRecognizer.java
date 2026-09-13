package br.com.fiap.arkive.service.speech;

import java.util.function.Consumer;

public interface ContinuousSpeechRecognizer extends AutoCloseable {

	void onRecognized(Consumer<SpeechRecognitionSegment> listener);

	void onCanceled(Consumer<SpeechRecognitionCancellation> listener);

	void onSessionStopped(Runnable listener);

	void start();

	void stop();

	@Override
	void close();
}
