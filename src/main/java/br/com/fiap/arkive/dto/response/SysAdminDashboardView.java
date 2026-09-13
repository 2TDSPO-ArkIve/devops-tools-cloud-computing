package br.com.fiap.arkive.dto.response;

import java.util.List;

public record SysAdminDashboardView(
		long clinicasAtivas,
		long veterinariosAtivos,
		long responsaveisAtivos,
		long animaisAtivos,
		long consultasUltimos30Dias,
		long usuariosAtivos,
		List<DashboardMetric> consultasPorMes,
		List<DashboardMetric> animaisPorEspecie,
		List<DashboardMetric> usuariosPorPerfil,
		List<DashboardMetric> consultasPorStatus
) {

	public static SysAdminDashboardView empty() {
		return new SysAdminDashboardView(
				0,
				0,
				0,
				0,
				0,
				0,
				List.of(),
				List.of(),
				List.of(),
				List.of()
		);
	}

	public record DashboardMetric(String label, long value, int percentage) {
	}

	public boolean hasConsultasPorMes() {
		return hasAnyValue(consultasPorMes);
	}

	public boolean hasAnimaisPorEspecie() {
		return hasAnyValue(animaisPorEspecie);
	}

	public boolean hasUsuariosPorPerfil() {
		return hasAnyValue(usuariosPorPerfil);
	}

	public boolean hasConsultasPorStatus() {
		return hasAnyValue(consultasPorStatus);
	}

	private boolean hasAnyValue(List<DashboardMetric> metrics) {
		return metrics != null && metrics.stream().anyMatch(metric -> metric.value() > 0);
	}

}
