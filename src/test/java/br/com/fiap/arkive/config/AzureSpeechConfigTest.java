package br.com.fiap.arkive.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AzureSpeechConfigTest {

	@Test
	void carregaListaDeFrasesVeterinarias() {
		AzureSpeechConfig config = new AzureSpeechConfig();

		var phrases = config.azureSpeechVeterinaryPhrases(new AzureSpeechProperties());

		assertTrue(phrases.contains("displasia coxofemoral"));
		assertTrue(phrases.contains("prednisolona"));
		assertTrue(phrases.contains("Labrador Retriever"));
	}
}
