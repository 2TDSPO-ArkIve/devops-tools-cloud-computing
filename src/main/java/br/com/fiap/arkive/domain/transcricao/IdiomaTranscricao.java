package br.com.fiap.arkive.domain.transcricao;

import br.com.fiap.arkive.exception.BusinessException;

import java.util.Arrays;

public enum IdiomaTranscricao {
	PT_BR("pt-BR"),
	EN_US("en-US");

	private final String codigo;

	IdiomaTranscricao(String codigo) {
		this.codigo = codigo;
	}

	public String getCodigo() {
		return codigo;
	}

	public static IdiomaTranscricao fromCodigoOrDefault(String codigo) {
		if (codigo == null || codigo.isBlank()) {
			return PT_BR;
		}
		return Arrays.stream(values())
				.filter(idioma -> idioma.codigo.equals(codigo.trim()))
				.findFirst()
				.orElseThrow(() -> new BusinessException("Idioma de transcricao deve ser pt-BR ou en-US."));
	}
}
