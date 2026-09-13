package br.com.fiap.arkive.service.speech;

import br.com.fiap.arkive.domain.transcricao.IdiomaTranscricao;

import java.nio.file.Path;

public interface ContinuousSpeechRecognizerFactory {

	ContinuousSpeechRecognizer create(Path wavFile, IdiomaTranscricao idioma);
}
