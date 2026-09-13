package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.Responsavel;

public record ResponsavelLookupResponse(Long id, String nome, String email) {
	public static ResponsavelLookupResponse fromEntity(Responsavel responsavel) {
		return new ResponsavelLookupResponse(responsavel.getId(), responsavel.getNome(), responsavel.getEmail());
	}
}
