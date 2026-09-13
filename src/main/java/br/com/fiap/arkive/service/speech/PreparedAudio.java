package br.com.fiap.arkive.service.speech;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class PreparedAudio implements AutoCloseable {

	private final Path wavFile;
	private final List<Path> temporaryFiles;

	public PreparedAudio(Path wavFile, List<Path> temporaryFiles) {
		this.wavFile = wavFile;
		this.temporaryFiles = temporaryFiles;
	}

	public Path wavFile() {
		return wavFile;
	}

	@Override
	public void close() {
		for (Path temporaryFile : temporaryFiles) {
			try {
				Files.deleteIfExists(temporaryFile);
			} catch (IOException ignored) {
				// Best-effort cleanup only. Temporary paths and audio content are not logged.
			}
		}
	}
}
