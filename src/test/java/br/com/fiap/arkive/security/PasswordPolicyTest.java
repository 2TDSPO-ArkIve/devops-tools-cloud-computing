package br.com.fiap.arkive.security;

import br.com.fiap.arkive.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyTest {

	private final PasswordPolicy passwordPolicy = new PasswordPolicy();

	@Test
	void aceitaSenhaComMinimoMaiusculaMinusculaENumero() {
		assertDoesNotThrow(() -> passwordPolicy.validar("Senha123"));
	}

	@Test
	void rejeitaSenhaFraca() {
		assertThrows(BusinessException.class, () -> passwordPolicy.validar("Curta1"));
		assertThrows(BusinessException.class, () -> passwordPolicy.validar("senhasemnumero"));
		assertThrows(BusinessException.class, () -> passwordPolicy.validar("SENHASEMNUMERO"));
		assertThrows(BusinessException.class, () -> passwordPolicy.validar("senha123"));
	}

	@Test
	void rejeitaConfirmacaoDiferente() {
		assertThrows(BusinessException.class, () -> passwordPolicy.validarConfirmacao("Senha123", "Senha124"));
	}
}
