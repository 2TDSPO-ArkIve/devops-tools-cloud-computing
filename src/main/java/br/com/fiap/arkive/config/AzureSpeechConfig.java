package br.com.fiap.arkive.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableConfigurationProperties(AzureSpeechProperties.class)
public class AzureSpeechConfig {

	@Bean
	@Qualifier("azureSpeechVeterinaryPhrases")
	List<String> azureSpeechVeterinaryPhrases(AzureSpeechProperties properties) {
		ClassPathResource resource = new ClassPathResource(properties.getPhraseListResource());
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
			return reader.lines()
					.map(String::trim)
					.filter(line -> !line.isBlank())
					.filter(line -> !line.startsWith("#"))
					.distinct()
					.toList();
		} catch (IOException ex) {
			throw new IllegalStateException("Lista de frases veterinarias para transcricao indisponivel.", ex);
		}
	}
}
