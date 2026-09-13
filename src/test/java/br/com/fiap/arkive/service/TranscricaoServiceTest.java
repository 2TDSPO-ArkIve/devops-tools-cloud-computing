package br.com.fiap.arkive.service;

import br.com.fiap.arkive.config.AzureSpeechProperties;
import br.com.fiap.arkive.domain.transcricao.IdiomaTranscricao;
import br.com.fiap.arkive.domain.transcricao.SupportedAudioFormat;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import br.com.fiap.arkive.service.speech.SpeechTranscriptionGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TranscricaoServiceTest {

	private SpeechTranscriptionGateway gateway;
	private AzureSpeechProperties properties;
	private TranscricaoService service;

	@BeforeEach
	void setUp() {
		gateway = mock(SpeechTranscriptionGateway.class);
		properties = new AzureSpeechProperties();
		properties.setMaxUploadSize(DataSize.ofMegabytes(10));
		service = new TranscricaoService(gateway, properties);
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(strings = {"audio/webm", "audio/webm;codecs=opus"})
	void aceitaWebmDoNavegadorSemAlterarMime(String mime) {
		when(gateway.transcrever(any(), eq("consulta.webm"), eq(mime), eq(SupportedAudioFormat.WEBM), eq(IdiomaTranscricao.PT_BR))).thenReturn("Narrativa");
		var response = service.transcrever(new MockMultipartFile("audio", "consulta.webm", mime, new byte[] {1, 2}), null, veterinario());
		assertEquals("Narrativa", response.transcricao());
		verify(gateway).transcrever(any(), eq("consulta.webm"), eq(mime), eq(SupportedAudioFormat.WEBM), eq(IdiomaTranscricao.PT_BR));
	}

	@Test
	void transcrevePortuguesComoIdiomaPadrao() {
		when(gateway.transcrever(any(), eq("consulta.wav"), eq("audio/wav"), eq(SupportedAudioFormat.WAV), eq(IdiomaTranscricao.PT_BR)))
				.thenReturn("Paciente canino apresenta claudicacao.");

		var response = service.transcrever(wav("consulta.wav", "audio/wav"), null, veterinario());

		assertEquals("Paciente canino apresenta claudicacao.", response.transcricao());
		assertEquals("pt-BR", response.idioma());
	}

	@Test
	void transcreveInglesQuandoSolicitado() {
		when(gateway.transcrever(any(), eq("case.wav"), eq("audio/wav"), eq(SupportedAudioFormat.WAV), eq(IdiomaTranscricao.EN_US)))
				.thenReturn("Canine patient presents lameness.");

		var response = service.transcrever(wav("case.wav", "audio/wav"), "en-US", veterinario());

		assertEquals("Canine patient presents lameness.", response.transcricao());
		assertEquals("en-US", response.idioma());
	}

	@Test
	void rejeitaIdiomaNaoSuportado() {
		BusinessException exception = assertThrows(BusinessException.class,
				() -> service.transcrever(wav("consulta.wav", "audio/wav"), "es-ES", veterinario()));

		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
		verify(gateway, never()).transcrever(any(), any(), any(), any(), any());
	}

	@Test
	void rejeitaAudioAusenteOuVazio() {
		BusinessException exception = assertThrows(BusinessException.class,
				() -> service.transcrever(new MockMultipartFile("audio", new byte[0]), "pt-BR", veterinario()));

		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
		verify(gateway, never()).transcrever(any(), any(), any(), any(), any());
	}

	@Test
	void rejeitaArquivoAcimaDoLimite() {
		properties.setMaxUploadSize(DataSize.ofBytes(4));

		BusinessException exception = assertThrows(BusinessException.class,
				() -> service.transcrever(wav("consulta.wav", "audio/wav"), "pt-BR", veterinario()));

		assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, exception.getStatus());
		verify(gateway, never()).transcrever(any(), any(), any(), any(), any());
	}

	@Test
	void aceitaM4aDoExpo() {
		when(gateway.transcrever(any(), eq("consulta.m4a"), eq("audio/mp4"), eq(SupportedAudioFormat.M4A), eq(IdiomaTranscricao.PT_BR)))
				.thenReturn("Paciente canino apresenta claudicacao.");

		var response = service.transcrever(new MockMultipartFile("audio", "consulta.m4a", "audio/mp4", "m4a".getBytes()), "pt-BR", veterinario());

		assertEquals("Paciente canino apresenta claudicacao.", response.transcricao());
	}

	@Test
	void aceitaAacDoExpoQuandoEnviadoSemContainerM4a() {
		when(gateway.transcrever(any(), eq("consulta.aac"), eq("audio/aac"), eq(SupportedAudioFormat.AAC), eq(IdiomaTranscricao.PT_BR)))
				.thenReturn("Paciente canino apresenta claudicacao.");

		var response = service.transcrever(new MockMultipartFile("audio", "consulta.aac", "audio/aac", "aac".getBytes()), "pt-BR", veterinario());

		assertEquals("Paciente canino apresenta claudicacao.", response.transcricao());
	}

	@Test
	void rejeitaFormatoNaoSuportado() {
		BusinessException exception = assertThrows(BusinessException.class,
				() -> service.transcrever(new MockMultipartFile("audio", "consulta.mp3", "audio/mpeg", wavBytes()), "pt-BR", veterinario()));

		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
		verify(gateway, never()).transcrever(any(), any(), any(), any(), any());
	}

	@Test
	void rejeitaWavComCabecalhoInvalido() {
		BusinessException exception = assertThrows(BusinessException.class,
				() -> service.transcrever(new MockMultipartFile("audio", "consulta.wav", "audio/wav", "fake".getBytes()), "pt-BR", veterinario()));

		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
		verify(gateway, never()).transcrever(any(), any(), any(), any(), any());
	}

	@Test
	void noSpeechDoGatewayRetorna422() {
		when(gateway.transcrever(any(), any(), any(), any(), any())).thenReturn(" ");

		BusinessException exception = assertThrows(BusinessException.class,
				() -> service.transcrever(wav("consulta.wav", "audio/wav"), "pt-BR", veterinario()));

		assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
	}

	@Test
	void somenteVeterinarioPodeTranscrever() {
		assertThrows(AccessDeniedException.class,
				() -> service.transcrever(wav("consulta.wav", "audio/wav"), "pt-BR", responsavel()));

		verify(gateway, never()).transcrever(any(), any(), any(), any(), any());
	}

	private MockMultipartFile wav(String filename, String contentType) {
		return new MockMultipartFile("audio", filename, contentType, wavBytes());
	}

	private byte[] wavBytes() {
		return new byte[] { 'R', 'I', 'F', 'F', 36, 0, 0, 0, 'W', 'A', 'V', 'E', 'f', 'm', 't', ' ' };
	}

	private UsuarioPrincipal veterinario() {
		return new UsuarioPrincipal(1L, "Dra Vera", "vera@arkive.com", "$2a$10$hash", TipoUsuario.VETERINARIO, "S", false, null, 10L, null);
	}

	private UsuarioPrincipal responsavel() {
		return new UsuarioPrincipal(2L, "Joao", "joao@arkive.com", "$2a$10$hash", TipoUsuario.RESPONSAVEL, "S", false, 20L, null, null);
	}
}
