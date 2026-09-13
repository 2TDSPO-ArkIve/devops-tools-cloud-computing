package br.com.fiap.arkive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record AnimalRequest(
		@NotBlank
		@Size(max = 50)
		String nome,

		@NotNull
		Long especieId,

		Long racaId,

		@Size(max = 1)
		String sexo,

		@Size(max = 1)
		String castrado,

		Long clinicaId,

		@Size(max = 1)
		String ativo,

		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
		LocalDate dataNascimento
) {
	public AnimalRequest(String nome, Long especieId, Long racaId, String sexo, String castrado,
			Long clinicaId, String ativo) {
		this(nome, especieId, racaId, sexo, castrado, clinicaId, ativo, null);
	}
}
