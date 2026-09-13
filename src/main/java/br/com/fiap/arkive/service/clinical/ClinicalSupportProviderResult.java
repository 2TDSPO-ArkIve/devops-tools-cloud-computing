package br.com.fiap.arkive.service.clinical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record ClinicalSupportProviderResult(
		String diagnostico,
		String severidade,
		String insightIa,
		Integer confianca,
		List<String> fontesPesquisadas
) {
	public ClinicalSupportProviderResult {
		fontesPesquisadas = fontesPesquisadas == null
				? List.of()
				: Collections.unmodifiableList(new ArrayList<>(fontesPesquisadas));
	}

	public ClinicalSupportProviderResult(String diagnostico, String severidade, String insightIa, Integer confianca) {
		this(diagnostico, severidade, insightIa, confianca, List.of());
	}
}
