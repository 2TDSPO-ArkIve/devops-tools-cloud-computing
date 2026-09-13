package br.com.fiap.arkive.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporaryPasswordGeneratorTest {

	private final PasswordPolicy passwordPolicy = new PasswordPolicy();
	private final TemporaryPasswordGenerator generator = new TemporaryPasswordGenerator(passwordPolicy);

	@Test
	void geraSenhaTemporariaForteENaoDeterministica() {
		String primeira = generator.gerar();
		String segunda = generator.gerar();

		assertDoesNotThrow(() -> passwordPolicy.validar(primeira));
		assertTrue(primeira.length() >= 8);
		assertNotEquals(primeira, segunda);
	}
}
