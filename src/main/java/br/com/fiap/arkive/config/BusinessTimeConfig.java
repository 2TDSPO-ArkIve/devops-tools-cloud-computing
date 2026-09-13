package br.com.fiap.arkive.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class BusinessTimeConfig {
	@Bean
	public Clock businessClock() {
		return Clock.system(ZoneId.of("America/Sao_Paulo"));
	}
}
