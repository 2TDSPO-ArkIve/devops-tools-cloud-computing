package br.com.fiap.arkive.security;

import br.com.fiap.arkive.exception.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {

	public void validar(String senha) {
		if (senha == null || senha.length() < 8) {
			throw new BusinessException("A senha deve ter pelo menos 8 caracteres.");
		}
		if (senha.length() > 72) {
			throw new BusinessException("A senha deve ter no maximo 72 caracteres.");
		}
		if (senha.chars().noneMatch(Character::isUpperCase)) {
			throw new BusinessException("A senha deve conter pelo menos uma letra maiuscula.");
		}
		if (senha.chars().noneMatch(Character::isLowerCase)) {
			throw new BusinessException("A senha deve conter pelo menos uma letra minuscula.");
		}
		if (senha.chars().noneMatch(Character::isDigit)) {
			throw new BusinessException("A senha deve conter pelo menos um numero.");
		}
	}

	public void validarConfirmacao(String novaSenha, String confirmacao) {
		if (confirmacao == null || !confirmacao.equals(novaSenha)) {
			throw new BusinessException("A confirmacao da senha nao confere.");
		}
	}
}
