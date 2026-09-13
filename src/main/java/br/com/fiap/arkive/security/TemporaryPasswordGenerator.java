package br.com.fiap.arkive.security;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.SecureRandom;

@Component
public class TemporaryPasswordGenerator {

	private static final char[] UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
	private static final char[] LOWER = "abcdefghijkmnopqrstuvwxyz".toCharArray();
	private static final char[] DIGITS = "23456789".toCharArray();
	private static final char[] SYMBOLS = "!@#$%&*?".toCharArray();
	private static final char[] ALL = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%&*?".toCharArray();
	private static final int LENGTH = 14;

	private final SecureRandom secureRandom;
	private final PasswordPolicy passwordPolicy;

	@Autowired
	public TemporaryPasswordGenerator(PasswordPolicy passwordPolicy) {
		this(new SecureRandom(), passwordPolicy);
	}

	TemporaryPasswordGenerator(SecureRandom secureRandom, PasswordPolicy passwordPolicy) {
		this.secureRandom = secureRandom;
		this.passwordPolicy = passwordPolicy;
	}

	public String gerar() {
		while (true) {
			char[] password = new char[LENGTH];
			password[0] = randomChar(UPPER);
			password[1] = randomChar(LOWER);
			password[2] = randomChar(DIGITS);
			password[3] = randomChar(SYMBOLS);
			for (int i = 4; i < password.length; i++) {
				password[i] = randomChar(ALL);
			}
			shuffle(password);
			String senha = new String(password);
			try {
				passwordPolicy.validar(senha);
				return senha;
			} catch (RuntimeException ignored) {
				// Try again if future policy changes make this composition invalid.
			}
		}
	}

	private char randomChar(char[] chars) {
		return chars[secureRandom.nextInt(chars.length)];
	}

	private void shuffle(char[] chars) {
		for (int i = chars.length - 1; i > 0; i--) {
			int j = secureRandom.nextInt(i + 1);
			char temp = chars[i];
			chars[i] = chars[j];
			chars[j] = temp;
		}
	}
}
