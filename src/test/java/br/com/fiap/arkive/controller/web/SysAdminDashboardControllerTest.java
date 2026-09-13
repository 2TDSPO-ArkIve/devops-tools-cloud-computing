package br.com.fiap.arkive.controller.web;

import br.com.fiap.arkive.dto.response.SysAdminDashboardView;
import br.com.fiap.arkive.dto.response.SysAdminDashboardView.DashboardMetric;
import br.com.fiap.arkive.service.SysAdminDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local-nodb")
class SysAdminDashboardControllerTest {

	private final MockMvc mockMvc;

	@MockitoBean
	private SysAdminDashboardService dashboardService;

	@Autowired
	SysAdminDashboardControllerTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	@WithMockUser(roles = "SYSADMIN")
	void dashboardRenderizaMetricasReaisSemCardsPlaceholder() throws Exception {
		when(dashboardService.carregar()).thenReturn(dashboard());

		mockMvc.perform(get("/sysadmin/dashboard"))
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("dashboard"))
				.andExpect(content().string(containsString("Clínicas ativas")))
				.andExpect(content().string(containsString("Veterinários ativos")))
				.andExpect(content().string(containsString("Consultas nos últimos 6 meses")))
				.andExpect(content().string(containsString("Animais por espécie")))
				.andExpect(content().string(containsString("Usuários por perfil")))
				.andExpect(content().string(containsString("Consultas por status")))
				.andExpect(content().string(containsString("/images/favicon.png")))
				.andExpect(content().string(not(containsString("Gestão de Acessos"))))
				.andExpect(content().string(not(containsString("Espaço reservado"))))
				.andExpect(content().string(not(containsString("Base web para acompanhar"))));
	}

	@Test
	void dashboardAnonimoRedirecionaParaLogin() throws Exception {
		mockMvc.perform(get("/sysadmin/dashboard"))
				.andExpect(status().is3xxRedirection());
	}

	@Test
	@WithMockUser(roles = "ADMIN_CLINICA")
	void dashboardAdminClinicaPermaneceProibido() throws Exception {
		mockMvc.perform(get("/sysadmin/dashboard"))
				.andExpect(status().isForbidden());
	}

	private SysAdminDashboardView dashboard() {
		return new SysAdminDashboardView(
				2,
				3,
				4,
				5,
				6,
				7,
				List.of(new DashboardMetric("Ago", 6, 100)),
				List.of(new DashboardMetric("Canina", 5, 100)),
				List.of(new DashboardMetric("SysAdmin", 1, 100)),
				List.of(new DashboardMetric("Agendada", 2, 100))
		);
	}

}
