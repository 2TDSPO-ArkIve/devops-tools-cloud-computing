package br.com.fiap.arkive.service.speech;

import br.com.fiap.arkive.domain.transcricao.IdiomaTranscricao;
import br.com.fiap.arkive.domain.transcricao.SupportedAudioFormat;

public interface SpeechTranscriptionGateway {

	String transcrever(byte[] audio, String filename, String contentType, SupportedAudioFormat format, IdiomaTranscricao idioma);
}
