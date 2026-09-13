package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.domain.consulta.StatusConsulta;
import br.com.fiap.arkive.entity.Consulta;
import br.com.fiap.arkive.entity.Diagnostico;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Objects;

@Schema(description = "Suporte clinico persistido em terminologia ArkIve. Nao expõe o contrato bruto do provedor externo.")
public record ClinicalSupportResponse(
		@Schema(example = "42")
		Long consultaId,
		@Schema(description = "Status atual da consulta.", example = "AP")
		String statusConsulta,
		@Schema(example = "Aguardando Parecer")
		String statusDescricao,
		@Schema(description = "Hipotese diagnostica gerada como apoio clinico, ainda nao confirmada.", example = "Otite externa")
		String hipoteseDiagnostica,
		@Schema(example = "MODERADA")
		String severidadeSugerida,
		@Schema(description = "Insight clinico investigativo retornado pelo motor externo.", example = "Avaliar ouvido externo e historico de prurido.")
		String insightClinico,
		@Schema(description = "Confianca informada pelo motor clinico em percentual.", example = "65")
		Integer confianca,
		@Schema(description = "Fontes consultadas pelo motor clinico externo.")
		List<String> fontesPesquisadas
) {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final TypeReference<List<String>> FONTES_TYPE = new TypeReference<>() {
	};

	public ClinicalSupportResponse {
		fontesPesquisadas = fontesPesquisadas == null
				? List.of()
				: fontesPesquisadas.stream().filter(Objects::nonNull).toList();
	}

	public ClinicalSupportResponse(
			Long consultaId,
			String statusConsulta,
			String statusDescricao,
			String hipoteseDiagnostica,
			String severidadeSugerida,
			String insightClinico,
			Integer confianca
	) {
		this(consultaId, statusConsulta, statusDescricao, hipoteseDiagnostica, severidadeSugerida, insightClinico, confianca, List.of());
	}

	public static ClinicalSupportResponse fromEntities(Consulta consulta, Diagnostico diagnostico) {
		StatusConsulta status = StatusConsulta.fromCodigo(consulta.getStatus());
		Integer confianca = diagnostico.getConfianca() == null ? null : diagnostico.getConfianca().intValue();
		return new ClinicalSupportResponse(
				consulta.getId(),
				consulta.getStatus(),
				status.getDescricao(),
				diagnostico.getDiagnostico(),
				diagnostico.getSeveridade(),
				diagnostico.getInsightIa(),
				confianca,
				lerFontes(diagnostico.getFontesIaJson())
		);
	}

	private static List<String> lerFontes(String fontesIaJson) {
		if (fontesIaJson == null || fontesIaJson.isBlank()) {
			return List.of();
		}
		try {
			List<String> fontes = OBJECT_MAPPER.readValue(fontesIaJson, FONTES_TYPE);
			return fontes == null ? List.of() : fontes.stream().filter(Objects::nonNull).toList();
		} catch (JsonProcessingException | IllegalArgumentException ex) {
			return List.of();
		}
	}
}
