package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.response.SysAdminDashboardView;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.repository.AnimalRepository;
import br.com.fiap.arkive.repository.ClinicaRepository;
import br.com.fiap.arkive.repository.ConsultaRepository;
import br.com.fiap.arkive.repository.ResponsavelRepository;
import br.com.fiap.arkive.repository.UsuarioRepository;
import br.com.fiap.arkive.repository.VeterinarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysAdminDashboardServiceTest {

	private ClinicaRepository clinicaRepository;
	private VeterinarioRepository veterinarioRepository;
	private ResponsavelRepository responsavelRepository;
	private AnimalRepository animalRepository;
	private ConsultaRepository consultaRepository;
	private UsuarioRepository usuarioRepository;
	private SysAdminDashboardService service;

	@BeforeEach
	void setUp() {
		clinicaRepository = mock(ClinicaRepository.class);
		veterinarioRepository = mock(VeterinarioRepository.class);
		responsavelRepository = mock(ResponsavelRepository.class);
		animalRepository = mock(AnimalRepository.class);
		consultaRepository = mock(ConsultaRepository.class);
		usuarioRepository = mock(UsuarioRepository.class);
		Clock clock = Clock.fixed(Instant.parse("2026-08-29T12:00:00Z"), ZoneId.of("America/Sao_Paulo"));
		service = new SysAdminDashboardService(
				clinicaRepository,
				veterinarioRepository,
				responsavelRepository,
				animalRepository,
				consultaRepository,
				usuarioRepository,
				clock
		);
		when(consultaRepository.contarPorMes(
				eq(LocalDateTime.of(2026, 3, 1, 0, 0)),
				eq(LocalDateTime.of(2026, 8, 29, 9, 0))
		)).thenReturn(List.of());
		when(animalRepository.contarAtivosPorEspecie("S")).thenReturn(List.of());
		when(usuarioRepository.contarPorPerfil()).thenReturn(List.of());
		when(consultaRepository.contarPorStatus()).thenReturn(List.of());
	}

	@Test
	void construtorDeProducaoEhExplicitoParaInjecaoSpring() throws Exception {
		Constructor<SysAdminDashboardService> constructor = SysAdminDashboardService.class.getConstructor(
				ClinicaRepository.class,
				VeterinarioRepository.class,
				ResponsavelRepository.class,
				AnimalRepository.class,
				ConsultaRepository.class,
				UsuarioRepository.class
		);

		assertTrue(constructor.isAnnotationPresent(Autowired.class));
	}

	@Test
	void carregaContadoresAtivosEConsultasDosUltimos30Dias() {
		when(clinicaRepository.countByAtivo("S")).thenReturn(2L);
		when(veterinarioRepository.countByAtivo("S")).thenReturn(3L);
		when(responsavelRepository.countByAtivo("S")).thenReturn(4L);
		when(animalRepository.countByAtivo("S")).thenReturn(5L);
		when(consultaRepository.countByDataHoraBetween(
				LocalDateTime.of(2026, 7, 30, 9, 0),
				LocalDateTime.of(2026, 8, 29, 9, 0)
		)).thenReturn(6L);
		when(usuarioRepository.countByAtivo("S")).thenReturn(7L);

		SysAdminDashboardView dashboard = service.carregar();

		assertEquals(2L, dashboard.clinicasAtivas());
		assertEquals(3L, dashboard.veterinariosAtivos());
		assertEquals(4L, dashboard.responsaveisAtivos());
		assertEquals(5L, dashboard.animaisAtivos());
		assertEquals(6L, dashboard.consultasUltimos30Dias());
		assertEquals(7L, dashboard.usuariosAtivos());
		verify(consultaRepository).countByDataHoraBetween(
				LocalDateTime.of(2026, 7, 30, 9, 0),
				LocalDateTime.of(2026, 8, 29, 9, 0)
		);
	}

	@Test
	void geraSeisMesesComLacunasPreenchidasComZero() {
		when(consultaRepository.contarPorMes(
				eq(LocalDateTime.of(2026, 3, 1, 0, 0)),
				eq(LocalDateTime.of(2026, 8, 29, 9, 0))
		)).thenReturn(List.of(
				new Object[]{2026, 3, 2L},
				new Object[]{2026, 5, 4L},
				new Object[]{2026, 8, 8L}
		));

		SysAdminDashboardView dashboard = service.carregar();

		assertEquals(List.of("Mar", "Abr", "Mai", "Jun", "Jul", "Ago"),
				dashboard.consultasPorMes().stream().map(SysAdminDashboardView.DashboardMetric::label).toList());
		assertEquals(List.of(2L, 0L, 4L, 0L, 0L, 8L),
				dashboard.consultasPorMes().stream().map(SysAdminDashboardView.DashboardMetric::value).toList());
	}

	@Test
	void agregaAnimaisAtivosPorEspecie() {
		when(animalRepository.contarAtivosPorEspecie("S")).thenReturn(List.of(
				new Object[]{"Canina", 8L},
				new Object[]{"Felina", 4L}
		));

		SysAdminDashboardView dashboard = service.carregar();

		assertEquals("Canina", dashboard.animaisPorEspecie().get(0).label());
		assertEquals(8L, dashboard.animaisPorEspecie().get(0).value());
		assertEquals(100, dashboard.animaisPorEspecie().get(0).percentage());
		assertEquals(50, dashboard.animaisPorEspecie().get(1).percentage());
	}

	@Test
	void agregaUsuariosPorPerfilComPerfisSemDadosZerados() {
		when(usuarioRepository.contarPorPerfil()).thenReturn(List.of(
				new Object[]{TipoUsuario.SYSADMIN, 1L},
				new Object[]{TipoUsuario.RESPONSAVEL, 3L}
		));

		SysAdminDashboardView dashboard = service.carregar();

		assertEquals(List.of("SysAdmin", "Administrador da Clínica", "Veterinário", "Responsável"),
				dashboard.usuariosPorPerfil().stream().map(SysAdminDashboardView.DashboardMetric::label).toList());
		assertEquals(List.of(1L, 0L, 0L, 3L),
				dashboard.usuariosPorPerfil().stream().map(SysAdminDashboardView.DashboardMetric::value).toList());
	}

	@Test
	void agregaConsultasPorStatusComRotulosAmigaveis() {
		when(consultaRepository.contarPorStatus()).thenReturn(List.of(
				new Object[]{"FI", 5L},
				new Object[]{"AG", 2L}
		));

		SysAdminDashboardView dashboard = service.carregar();

		assertEquals("Finalizada", dashboard.consultasPorStatus().get(0).label());
		assertEquals("Agendada", dashboard.consultasPorStatus().get(1).label());
	}

}
