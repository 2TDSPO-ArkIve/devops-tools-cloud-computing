package br.com.fiap.arkive.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Public self-registration payload for a veterinarian — deliberately a
 * narrower subset of {@link VeterinarioRequest} (no {@code especialidade},
 * {@code clinicaId} or {@code ativo}: an anonymous caller must not be able
 * to attach themselves to an arbitrary clinic or dictate their own active
 * flag). {@code email} is required here (unlike VeterinarioRequest, where
 * it's only enforced by {@code VeterinarioService.criar}'s business rule)
 * since it doubles as the account's login.
 */
public record RegistroVeterinarioRequest(
		@NotBlank
		@Size(max = 50)
		String nome,

		@NotBlank
		@Size(max = 10)
		String crmv,

		@NotBlank
		@Email
		@Size(max = 200)
		String email
) {
}
