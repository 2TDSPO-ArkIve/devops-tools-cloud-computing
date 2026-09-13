package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.AvaliacaoBemEstar;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AvaliacaoBemEstarResponse(
		Long id,
		Long animalId,
		String animalNome,
		Long responsavelId,
		String responsavelNome,
		Long veterinarioId,
		String veterinarioNome,
		Long consultaId,
		LocalDateTime dataAvaliacao,
		BigDecimal peso,
		BigDecimal idade,
		String apetite,
		String atividade,
		String comportamento,
		String observacao
) {
	public static AvaliacaoBemEstarResponse fromEntity(AvaliacaoBemEstar avaliacao) {
		Long responsavelId = avaliacao.getResponsavel() == null ? null : avaliacao.getResponsavel().getId();
		String responsavelNome = avaliacao.getResponsavel() == null ? null : avaliacao.getResponsavel().getNome();
		Long veterinarioId = avaliacao.getVeterinario() == null ? null : avaliacao.getVeterinario().getId();
		String veterinarioNome = avaliacao.getVeterinario() == null ? null : avaliacao.getVeterinario().getNome();
		Long consultaId = avaliacao.getConsulta() == null ? null : avaliacao.getConsulta().getId();
		return new AvaliacaoBemEstarResponse(
				avaliacao.getId(),
				avaliacao.getAnimal().getId(),
				avaliacao.getAnimal().getNome(),
				responsavelId,
				responsavelNome,
				veterinarioId,
				veterinarioNome,
				consultaId,
				avaliacao.getDataAvaliacao(),
				avaliacao.getPeso(),
				avaliacao.getIdade(),
				avaliacao.getApetite(),
				avaliacao.getAtividade(),
				avaliacao.getComportamento(),
				avaliacao.getObservacao()
		);
	}
}
