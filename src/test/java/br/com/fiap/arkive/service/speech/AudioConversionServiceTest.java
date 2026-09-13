package br.com.fiap.arkive.service.speech;

import br.com.fiap.arkive.config.AzureSpeechProperties;
import br.com.fiap.arkive.domain.transcricao.SupportedAudioFormat;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioConversionServiceTest {

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.EnumSource(value = SupportedAudioFormat.class, names = {"WEBM", "M4A", "AAC"})
	void converteComArgumentosFixosELimpaTemporarios(SupportedAudioFormat formato) throws Exception {
		Process process = org.mockito.Mockito.mock(Process.class);
		org.mockito.Mockito.when(process.waitFor(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any())).thenReturn(true);
		java.util.List<Path> caminhos = new java.util.ArrayList<>();
		AudioConversionService service = new AudioConversionService(new AzureSpeechProperties()) {
			@Override
			Process iniciarProcesso(ProcessBuilder builder) throws java.io.IOException {
				var args = builder.command();
				Path entrada = Path.of(args.get(args.indexOf("-i") + 1));
				Path saida = Path.of(args.get(args.size() - 1));
				caminhos.addAll(java.util.List.of(entrada, saida));
				assertTrue(entrada.toString().endsWith(formato.tempFileExtension()));
				org.junit.jupiter.api.Assertions.assertEquals("16000", args.get(args.indexOf("-ar") + 1));
				org.junit.jupiter.api.Assertions.assertEquals("1", args.get(args.indexOf("-ac") + 1));
				org.junit.jupiter.api.Assertions.assertEquals("pcm_s16le", args.get(args.indexOf("-c:a") + 1));
				Files.write(saida, new byte[] {'R', 'I', 'F', 'F'});
				return process;
			}
		};
		try (PreparedAudio prepared = service.prepararWav(new byte[] {1, 2}, formato)) {
			assertTrue(Files.exists(prepared.wavFile()));
			assertTrue(prepared.wavFile().toString().endsWith(".wav"));
		}
		caminhos.forEach(path -> assertFalse(Files.exists(path)));
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(strings = {"corrupto", "timeout", "interrompido", "indisponivel"})
	void falhasWebmSaoSegurasELimpamArquivos(String cenario) throws Exception {
		Process process = org.mockito.Mockito.mock(Process.class);
		org.mockito.Mockito.when(process.waitFor(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any())).thenReturn(!cenario.equals("timeout"));
		org.mockito.Mockito.when(process.exitValue()).thenReturn(1);
		org.mockito.Mockito.when(process.isAlive()).thenReturn(cenario.equals("timeout") || cenario.equals("interrompido"));
		if (cenario.equals("interrompido")) {
			org.mockito.Mockito.when(process.waitFor(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any()))
					.thenThrow(new InterruptedException()).thenReturn(true);
		}
		java.util.List<Path> caminhos = new java.util.ArrayList<>();
		AudioConversionService service = new AudioConversionService(new AzureSpeechProperties()) {
			@Override
			Process iniciarProcesso(ProcessBuilder builder) throws java.io.IOException {
				var args = builder.command();
				caminhos.add(Path.of(args.get(args.indexOf("-i") + 1)));
				caminhos.add(Path.of(args.get(args.size() - 1)));
				if (cenario.equals("indisponivel")) throw new java.io.IOException("INTERNAL_SYSTEM_PATH");
				return process;
			}
		};
		try {
			var ex = org.junit.jupiter.api.Assertions.assertThrows(br.com.fiap.arkive.exception.BusinessException.class,
					() -> service.prepararWav(new byte[] {1}, SupportedAudioFormat.WEBM));
			org.junit.jupiter.api.Assertions.assertEquals(cenario.equals("corrupto") || cenario.equals("timeout") ? 400 : 503, ex.getStatus().value());
			assertFalse(ex.getMessage().contains("INTERNAL_SYSTEM_PATH"));
			caminhos.forEach(path -> assertFalse(Files.exists(path)));
			if (cenario.equals("interrompido") || cenario.equals("timeout")) {
				org.mockito.Mockito.verify(process, org.mockito.Mockito.atLeastOnce()).destroyForcibly();
			}
			if (cenario.equals("interrompido")) assertTrue(Thread.currentThread().isInterrupted());
		} finally {
			Thread.interrupted();
		}
	}

	@Test
	void wavTemporarioEhRemovidoAoFecharPreparedAudio() throws Exception {
		AudioConversionService service = new AudioConversionService(new AzureSpeechProperties());
		byte[] audio = { 'R', 'I', 'F', 'F', 36, 0, 0, 0, 'W', 'A', 'V', 'E' };
		Path wavPath;

		try (PreparedAudio preparedAudio = service.prepararWav(audio, SupportedAudioFormat.WAV)) {
			wavPath = preparedAudio.wavFile();
			assertTrue(Files.exists(wavPath));
			assertArrayEquals(audio, Files.readAllBytes(wavPath));
		}

		assertFalse(Files.exists(wavPath));
	}
}
