package br.com.fiap.arkive.domain.consulta;

import br.com.fiap.arkive.exception.BusinessException;

import java.util.Arrays;

public enum StatusConsulta {
	AG("AG", "Agendada"),
	EP("EP", "Em Progresso"),
	AP("AP", "Aguardando Parecer"),
	FI("FI", "Finalizada"),
	CA("CA", "Cancelada");

	private final String codigo;
	private final String descricao;

	StatusConsulta(String codigo, String descricao) {
		this.codigo = codigo;
		this.descricao = descricao;
	}

	public String getCodigo() {
		return codigo;
	}

	public String getDescricao() {
		return descricao;
	}

	public static StatusConsulta fromCodigo(String codigo) {
		return Arrays.stream(values())
				.filter(status -> status.codigo.equals(codigo))
				.findFirst()
				.orElseThrow(() -> new BusinessException("Status deve ser AG, EP, AP, FI ou CA."));
	}

	public static void validarQuandoInformado(String codigo) {
		if (codigo != null && !codigo.isBlank()) {
			fromCodigo(codigo);
		}
	}
}
