package br.com.fiap.arkive.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "arkive.clinical-engine")
public class ClinicalEngineProperties {

	private String baseUrl = "https://arkive-clinical-engine.onrender.com";
	private Duration connectTimeout = Duration.ofSeconds(10);
	private Duration readTimeout = Duration.ofSeconds(120);

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public Duration getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(Duration connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public Duration getReadTimeout() {
		return readTimeout;
	}

	public void setReadTimeout(Duration readTimeout) {
		this.readTimeout = readTimeout;
	}
}
