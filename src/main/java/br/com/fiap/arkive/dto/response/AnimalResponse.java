package br.com.fiap.arkive.dto.response;

import br.com.fiap.arkive.entity.Animal;
import java.time.LocalDate;

public record AnimalResponse(
		Long id,
		String nome,
		Long especieId,
		String especieNome,
		Long racaId,
		String racaNome,
		String sexo,
		String castrado,
		Long clinicaId,
		String clinicaNome,
		String ativo,
		LocalDate dataNascimento
) {
	public AnimalResponse(Long id, String nome, Long especieId, String especieNome, Long racaId,
			String racaNome, String sexo, String castrado, Long clinicaId, String clinicaNome, String ativo) {
		this(id, nome, especieId, especieNome, racaId, racaNome, sexo, castrado, clinicaId, clinicaNome, ativo, null);
	}
	public static AnimalResponse fromEntity(Animal animal) {
		Long racaId = animal.getRaca() == null ? null : animal.getRaca().getId();
		String racaNome = animal.getRaca() == null ? null : animal.getRaca().getNome();
		Long clinicaId = animal.getClinica() == null ? null : animal.getClinica().getId();
		String clinicaNome = animal.getClinica() == null ? null : animal.getClinica().getNome();
		return new AnimalResponse(
				animal.getId(),
				animal.getNome(),
				animal.getEspecie().getId(),
				animal.getEspecie().getNome(),
				racaId,
				racaNome,
				animal.getSexo(),
				animal.getCastrado(),
				clinicaId,
				clinicaNome,
				animal.getAtivo(),
				animal.getDataNascimento()
		);
	}
}
