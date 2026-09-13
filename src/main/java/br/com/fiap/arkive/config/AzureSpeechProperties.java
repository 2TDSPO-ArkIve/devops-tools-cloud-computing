package br.com.fiap.arkive.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

@ConfigurationProperties(prefix = "arkive.azure.speech")
public class AzureSpeechProperties {

	private String endpoint = "";
	private String apiKey = "";
	private DataSize maxUploadSize = DataSize.ofMegabytes(10);
	private Duration recognitionTimeout = Duration.ofSeconds(120);
	private Duration conversionTimeout = Duration.ofSeconds(30);
	private Duration sdkOperationTimeout = Duration.ofSeconds(10);
	private int maxConcurrentRequests = 1;
	private double phraseListBiasingWeight = 1.3;
	private String phraseListResource = "azure-speech-veterinary-phrases.txt";

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public DataSize getMaxUploadSize() {
		return maxUploadSize;
	}

	public void setMaxUploadSize(DataSize maxUploadSize) {
		this.maxUploadSize = maxUploadSize;
	}

	public Duration getRecognitionTimeout() {
		return recognitionTimeout;
	}

	public void setRecognitionTimeout(Duration recognitionTimeout) {
		this.recognitionTimeout = recognitionTimeout;
	}

	public Duration getConversionTimeout() {
		return conversionTimeout;
	}

	public void setConversionTimeout(Duration conversionTimeout) {
		this.conversionTimeout = conversionTimeout;
	}

	public Duration getSdkOperationTimeout() {
		return sdkOperationTimeout;
	}

	public void setSdkOperationTimeout(Duration sdkOperationTimeout) {
		this.sdkOperationTimeout = sdkOperationTimeout;
	}

	public int getMaxConcurrentRequests() {
		return maxConcurrentRequests;
	}

	public void setMaxConcurrentRequests(int maxConcurrentRequests) {
		this.maxConcurrentRequests = maxConcurrentRequests;
	}

	public double getPhraseListBiasingWeight() {
		return phraseListBiasingWeight;
	}

	public void setPhraseListBiasingWeight(double phraseListBiasingWeight) {
		this.phraseListBiasingWeight = phraseListBiasingWeight;
	}

	public String getPhraseListResource() {
		return phraseListResource;
	}

	public void setPhraseListResource(String phraseListResource) {
		this.phraseListResource = phraseListResource;
	}

	public boolean isConfigured() {
		return hasText(endpoint) && hasText(apiKey);
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
