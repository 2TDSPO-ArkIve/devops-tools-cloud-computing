package br.com.fiap.arkive.service;

import br.com.fiap.arkive.dto.response.SysAdminDashboardView;
import br.com.fiap.arkive.dto.response.SysAdminDashboardView.DashboardMetric;
import br.com.fiap.arkive.entity.TipoUsuario;
import br.com.fiap.arkive.repository.AnimalRepository;
import br.com.fiap.arkive.repository.ClinicaRepository;
import br.com.fiap.arkive.repository.ConsultaRepository;
import br.com.fiap.arkive.repository.ResponsavelRepository;
import br.com.fiap.arkive.repository.UsuarioRepository;
import br.com.fiap.arkive.repository.VeterinarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Profile("!local-nodb")
public class SysAdminDashboardService {

	private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
	private static final List<TipoUsuario> PERFIS = List.of(
			TipoUsuario.SYSADMIN,
			TipoUsuario.ADMIN_CLINICA,
			TipoUsuario.VETERINARIO,
			TipoUsuario.RESPONSAVEL
	);
	private static final Map<String, String> STATUS_LABELS = Map.of(
			"AG", "Agendada",
			"EP", "Em progresso",
			"AP", "Aguardando Parecer",
			"FI", "Finalizada",
			"CA", "Cancelada"
	);

	private final ClinicaRepository clinicaRepository;
	private final VeterinarioRepository veterinarioRepository;
	private final ResponsavelRepository responsavelRepository;
	private final AnimalRepository animalRepository;
	private final ConsultaRepository consultaRepository;
	private final UsuarioRepository usuarioRepository;
	private final Clock clock;

	@Autowired
	public SysAdminDashboardService(
			ClinicaRepository clinicaRepository,
			VeterinarioRepository veterinarioRepository,
			ResponsavelRepository responsavelRepository,
			AnimalRepository animalRepository,
			ConsultaRepository consultaRepository,
			UsuarioRepository usuarioRepository
	) {
		this(
				clinicaRepository,
				veterinarioRepository,
				responsavelRepository,
				animalRepository,
				consultaRepository,
				usuarioRepository,
				Clock.systemDefaultZone()
		);
	}

	SysAdminDashboardService(
			ClinicaRepository clinicaRepository,
			VeterinarioRepository veterinarioRepository,
			ResponsavelRepository responsavelRepository,
			AnimalRepository animalRepository,
			ConsultaRepository consultaRepository,
			UsuarioRepository usuarioRepository,
			Clock clock
	) {
		this.clinicaRepository = clinicaRepository;
		this.veterinarioRepository = veterinarioRepository;
		this.responsavelRepository = responsavelRepository;
		this.animalRepository = animalRepository;
		this.consultaRepository = consultaRepository;
		this.usuarioRepository = usuarioRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public SysAdminDashboardView carregar() {
		LocalDate hoje = LocalDate.now(clock);
		LocalDateTime agora = LocalDateTime.now(clock);
		LocalDateTime inicioUltimos30Dias = agora.minusDays(30);
		YearMonth mesAtual = YearMonth.from(hoje);
		YearMonth primeiroMes = mesAtual.minusMonths(5);
		LocalDateTime inicioSeisMeses = primeiroMes.atDay(1).atStartOfDay();

		return new SysAdminDashboardView(
				clinicaRepository.countByAtivo("S"),
				veterinarioRepository.countByAtivo("S"),
				responsavelRepository.countByAtivo("S"),
				animalRepository.countByAtivo("S"),
				consultaRepository.countByDataHoraBetween(inicioUltimos30Dias, agora),
				usuarioRepository.countByAtivo("S"),
				consultasPorMes(primeiroMes, consultaRepository.contarPorMes(inicioSeisMeses, agora)),
				distribution(animalRepository.contarAtivosPorEspecie("S")),
				usuariosPorPerfil(usuarioRepository.contarPorPerfil()),
				distributionComStatus(consultaRepository.contarPorStatus())
		);
	}

	private List<DashboardMetric> consultasPorMes(YearMonth primeiroMes, List<Object[]> rows) {
		Map<YearMonth, Long> valores = new LinkedHashMap<>();
		for (int i = 0; i < 6; i++) {
			valores.put(primeiroMes.plusMonths(i), 0L);
		}
		for (Object[] row : rows) {
			YearMonth mes = YearMonth.of(asInt(row[0]), asInt(row[1]));
			if (valores.containsKey(mes)) {
				valores.put(mes, asLong(row[2]));
			}
		}
		long max = valores.values().stream().mapToLong(Long::longValue).max().orElse(0);
		return valores.entrySet().stream()
				.map(entry -> new DashboardMetric(monthLabel(entry.getKey()), entry.getValue(), percentage(entry.getValue(), max)))
				.toList();
	}

	private List<DashboardMetric> usuariosPorPerfil(List<Object[]> rows) {
		Map<TipoUsuario, Long> valores = new LinkedHashMap<>();
		PERFIS.forEach(tipo -> valores.put(tipo, 0L));
		for (Object[] row : rows) {
			if (row[0] instanceof TipoUsuario tipo && valores.containsKey(tipo)) {
				valores.put(tipo, asLong(row[1]));
			}
		}
		long max = valores.values().stream().mapToLong(Long::longValue).max().orElse(0);
		return valores.entrySet().stream()
				.map(entry -> new DashboardMetric(friendlyTipo(entry.getKey()), entry.getValue(), percentage(entry.getValue(), max)))
				.toList();
	}

	private List<DashboardMetric> distribution(List<Object[]> rows) {
		long max = rows.stream()
				.mapToLong(row -> asLong(row[1]))
				.max()
				.orElse(0);
		return rows.stream()
				.map(row -> new DashboardMetric(String.valueOf(row[0]), asLong(row[1]), percentage(asLong(row[1]), max)))
				.toList();
	}

	private List<DashboardMetric> distributionComStatus(List<Object[]> rows) {
		List<DashboardMetric> metricas = new ArrayList<>();
		long max = rows.stream()
				.mapToLong(row -> asLong(row[1]))
				.max()
				.orElse(0);
		for (Object[] row : rows) {
			String status = String.valueOf(row[0]);
			long value = asLong(row[1]);
			metricas.add(new DashboardMetric(STATUS_LABELS.getOrDefault(status, status), value, percentage(value, max)));
		}
		metricas.sort(Comparator.comparing(DashboardMetric::value).reversed().thenComparing(DashboardMetric::label));
		return metricas;
	}

	private String monthLabel(YearMonth month) {
		String label = month.getMonth().getDisplayName(TextStyle.SHORT, PT_BR).replace(".", "");
		return label.substring(0, 1).toUpperCase(PT_BR) + label.substring(1);
	}

	private String friendlyTipo(TipoUsuario tipo) {
		return switch (tipo) {
			case SYSADMIN -> "SysAdmin";
			case ADMIN_CLINICA -> "Administrador da Clínica";
			case VETERINARIO -> "Veterinário";
			case RESPONSAVEL -> "Responsável";
		};
	}

	private int percentage(long value, long max) {
		if (max <= 0) {
			return 0;
		}
		return Math.max(2, (int) Math.round((value * 100.0) / max));
	}

	private int asInt(Object value) {
		return ((Number) value).intValue();
	}

	private long asLong(Object value) {
		return ((Number) value).longValue();
	}

}
