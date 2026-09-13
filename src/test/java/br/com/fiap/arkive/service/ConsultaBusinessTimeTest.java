package br.com.fiap.arkive.service;

import br.com.fiap.arkive.config.BusinessTimeConfig;
import br.com.fiap.arkive.dto.request.ConsultaRequest;
import br.com.fiap.arkive.entity.*;
import br.com.fiap.arkive.exception.BusinessException;
import br.com.fiap.arkive.repository.*;
import br.com.fiap.arkive.security.UsuarioPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.*;
import java.util.Optional;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ResourceLock("java.util.TimeZone.default")
class ConsultaBusinessTimeTest {
	@ParameterizedTest
	@CsvSource({
			"2026-09-08T21:59:00,false", "2026-09-08T22:03:59.999999999,false",
			"2026-09-08T22:04:00,true", "2026-09-08T22:04:10,true",
			"2026-09-08T22:05:00,true", "2026-09-08T23:40:00,true",
			"2026-09-08T23:55:00,true", "2026-09-09T00:01:00,true"
	})
	void comparaMinutoDeSaoPauloIndependenteDoFusoDoServidor(String data, boolean permitido) {
		TimeZone original = TimeZone.getDefault();
		try {
			for (String hostZone : new String[]{"UTC", "Asia/Tokyo", "America/Sao_Paulo"}) {
				TimeZone.setDefault(TimeZone.getTimeZone(hostZone));
				Clock clock = Clock.fixed(Instant.parse("2026-09-09T01:04:59.900Z"),
						new BusinessTimeConfig().businessClock().getZone());
				assertEquals(LocalDateTime.parse("2026-09-08T22:04:59.900"), LocalDateTime.now(clock));
				var fixture = new Fixture(clock);
				LocalDateTime selected = LocalDateTime.parse(data);
				if (permitido) {
					assertEquals(selected, fixture.service.criar(request(selected), fixture.principal).dataHora());
				} else {
					BusinessException erro = assertThrows(BusinessException.class,
							() -> fixture.service.criar(request(selected), fixture.principal));
					assertEquals(HttpStatus.BAD_REQUEST, erro.getStatus());
					assertEquals("Data e hora da consulta nao podem estar no passado.", erro.getMessage());
					verify(fixture.consultas, never()).save(any());
				}
			}
		} finally {
			TimeZone.setDefault(original);
		}
	}

	@ParameterizedTest
	@CsvSource({"2026-09-08T23:58:59,false", "2026-09-08T23:59:00,true", "2026-09-09T00:00:00,true"})
	void fronteiraDaMeiaNoite(String data, boolean permitido) {
		var fixture = new Fixture(Clock.fixed(Instant.parse("2026-09-09T02:59:55Z"),
				new BusinessTimeConfig().businessClock().getZone()));
		if (permitido) assertDoesNotThrow(() -> fixture.service.criar(request(LocalDateTime.parse(data)), fixture.principal));
		else assertThrows(BusinessException.class, () -> fixture.service.criar(request(LocalDateTime.parse(data)), fixture.principal));
	}

	@Test
	void reagendamentoEEdicaoHistoricaMantemRegras() {
		var fixture = new Fixture(Clock.fixed(Instant.parse("2026-09-09T01:04:59Z"),
				new BusinessTimeConfig().businessClock().getZone()));
		Consulta consulta = new Consulta(); consulta.setId(1L); consulta.setAnimal(fixture.animal);
		consulta.setVeterinario(fixture.vet); consulta.setDataHora(LocalDateTime.parse("2026-09-01T10:00:35"));
		when(fixture.consultas.findById(1L)).thenReturn(Optional.of(consulta));
		assertEquals(consulta.getDataHora(), fixture.service.atualizar(1L, request(consulta.getDataHora()), fixture.principal).dataHora());
		assertThrows(BusinessException.class, () -> fixture.service.atualizar(1L,
				request(LocalDateTime.parse("2026-09-08T21:59:00")), fixture.principal));
		assertEquals(LocalDateTime.parse("2026-09-08T22:04:00"), fixture.service.atualizar(1L,
				request(LocalDateTime.parse("2026-09-08T22:04:00")), fixture.principal).dataHora());
	}

	@Test
	void springInjetaRelogioDeNegocioSemDependerDoHost() {
		TimeZone original = TimeZone.getDefault();
		try {
			TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
			try (var context = new AnnotationConfigApplicationContext()) {
				context.register(BusinessTimeConfig.class, ConsultaService.class);
				context.registerBean(ConsultaRepository.class, () -> mock(ConsultaRepository.class));
				context.registerBean(AnimalRepository.class, () -> mock(AnimalRepository.class));
				context.registerBean(VeterinarioRepository.class, () -> mock(VeterinarioRepository.class));
				context.registerBean(ClinicaRepository.class, () -> mock(ClinicaRepository.class));
				context.registerBean(EventoJornadaService.class, () -> mock(EventoJornadaService.class));
				context.registerBean(ClinicalAccessService.class, () -> mock(ClinicalAccessService.class));
				context.refresh();
				Clock clock = context.getBean("businessClock", Clock.class);
				assertEquals(ZoneId.of("America/Sao_Paulo"), clock.getZone());
				assertSame(clock, ReflectionTestUtils.getField(context.getBean(ConsultaService.class), "clock"));
			}
		} finally { TimeZone.setDefault(original); }
	}

	private static ConsultaRequest request(LocalDateTime data) {
		return new ConsultaRequest(data, "REMOTA", "Retorno", null, null, null, null, "AG", 1L, null, null);
	}

	private static class Fixture {
		final ConsultaRepository consultas = mock(ConsultaRepository.class);
		final Animal animal = new Animal();
		final Veterinario vet = new Veterinario();
		final UsuarioPrincipal principal = new UsuarioPrincipal(1L, "Vet", "vet@example.test", "hash",
				TipoUsuario.VETERINARIO, "S", false, null, 10L, null);
		final ConsultaService service;

		Fixture(Clock clock) {
			animal.setId(1L); animal.setNome("Paciente"); vet.setId(10L); animal.setVeterinarioCadastro(vet);
			AnimalRepository animais = mock(AnimalRepository.class);
			VeterinarioRepository veterinarios = mock(VeterinarioRepository.class);
			when(animais.findById(1L)).thenReturn(Optional.of(animal));
			when(veterinarios.findById(10L)).thenReturn(Optional.of(vet));
			when(consultas.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
			var acesso = new ClinicalAccessService(mock(AnimalResponsavelRepository.class), consultas, mock(VeterinarioService.class));
			service = new ConsultaService(consultas, animais, veterinarios, mock(ClinicaRepository.class),
					mock(EventoJornadaService.class), acesso, clock);
		}
	}
}
