package br.com.fiap.arkive.service.speech;

import br.com.fiap.arkive.config.AzureSpeechProperties;
import br.com.fiap.arkive.domain.transcricao.SupportedAudioFormat;
import br.com.fiap.arkive.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class AudioConversionService {

	private final AzureSpeechProperties properties;

	public AudioConversionService(AzureSpeechProperties properties) {
		this.properties = properties;
	}

	public PreparedAudio prepararWav(byte[] audio, SupportedAudioFormat format) {
		Path inputFile = null;
		Path outputFile = null;
		List<Path> temporaryFiles = new ArrayList<>();
		try {
			inputFile = Files.createTempFile("arkive-transcricao-in-", format.tempFileExtension());
			Files.write(inputFile, audio);
			temporaryFiles.add(inputFile);
			if (!format.isConversionRequired()) {
				return new PreparedAudio(inputFile, temporaryFiles);
			}
			outputFile = Files.createTempFile("arkive-transcricao-out-", ".wav");
			temporaryFiles.add(outputFile);
			converterParaPcmWav(inputFile, outputFile);
			return new PreparedAudio(outputFile, temporaryFiles);
		} catch (BusinessException ex) {
			deleteIfNotTracked(inputFile, temporaryFiles);
			deleteIfNotTracked(outputFile, temporaryFiles);
			deleteAll(temporaryFiles);
			throw ex;
		} catch (IOException ex) {
			deleteIfNotTracked(inputFile, temporaryFiles);
			deleteIfNotTracked(outputFile, temporaryFiles);
			deleteAll(temporaryFiles);
			throw new BusinessException("Nao foi possivel preparar o audio para transcricao.", HttpStatus.BAD_REQUEST);
		}
	}

	private void converterParaPcmWav(Path inputFile, Path outputFile) {
		ProcessBuilder processBuilder = new ProcessBuilder(
				"ffmpeg",
				"-hide_banner",
				"-loglevel", "error",
				"-y",
				"-nostdin",
				"-i", inputFile.toString(),
				"-vn",
				"-c:a", "pcm_s16le",
				"-ac", "1",
				"-ar", "16000",
				"-sample_fmt", "s16",
				outputFile.toString()
		);
		processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
		processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);
		Process process = null;
		try {
			process = iniciarProcesso(processBuilder);
			boolean finished = process.waitFor(properties.getConversionTimeout().toMillis(), TimeUnit.MILLISECONDS);
			if (!finished) {
				process.destroyForcibly();
				throw new BusinessException("Tempo limite excedido ao converter audio para transcricao.", HttpStatus.BAD_REQUEST);
			}
			if (process.exitValue() != 0) {
				throw new BusinessException("Audio invalido ou em formato nao suportado para transcricao.", HttpStatus.BAD_REQUEST);
			}
		} catch (IOException ex) {
			throw new BusinessException("Servico de conversao de audio indisponivel.", HttpStatus.SERVICE_UNAVAILABLE);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new BusinessException("Servico de conversao de audio temporariamente indisponivel.", HttpStatus.SERVICE_UNAVAILABLE);
		} finally {
			if (process != null && process.isAlive()) {
				process.destroyForcibly();
				boolean interrupted = Thread.interrupted();
				try {
					process.waitFor(5, TimeUnit.SECONDS);
				} catch (InterruptedException ex) {
					interrupted = true;
				} finally {
					if (interrupted) {
						Thread.currentThread().interrupt();
					}
				}
			}
		}
	}

	Process iniciarProcesso(ProcessBuilder processBuilder) throws IOException {
		return processBuilder.start();
	}

	private void deleteIfNotTracked(Path file, List<Path> temporaryFiles) {
		if (file != null && !temporaryFiles.contains(file)) {
			deleteQuietly(file);
		}
	}

	private void deleteAll(List<Path> temporaryFiles) {
		for (Path temporaryFile : temporaryFiles) {
			deleteQuietly(temporaryFile);
		}
	}

	private void deleteQuietly(Path temporaryFile) {
		try {
			Files.deleteIfExists(temporaryFile);
		} catch (IOException ignored) {
			// Best-effort cleanup only. Do not log audio temporary paths.
		}
	}
}
