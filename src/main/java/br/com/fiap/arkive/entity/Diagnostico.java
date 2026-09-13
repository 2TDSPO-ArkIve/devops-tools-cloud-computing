package br.com.fiap.arkive.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "TB_ARKIVE_DIAGNOSTICO")
public class Diagnostico {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_DIAGNOSTICO")
	private Long id;

	@Lob
	@Column(name = "DS_DIAGNOSTICO", nullable = false)
	private String diagnostico;

	@Column(name = "TP_SEVERIDADE", length = 20)
	private String severidade;

	@Column(name = "ST_CONFIRMADO", nullable = false, columnDefinition = "CHAR(1)")
	private String confirmado = "S";

	@Lob
	@Column(name = "DS_INSIGHT_IA")
	private String insightIa;

	@Lob
	@Column(name = "DS_FONTES_IA")
	private String fontesIaJson;

	@Column(name = "PC_CONFIANCA", precision = 5, scale = 2)
	private BigDecimal confianca;

	@Column(name = "ST_VALIDACAO_VET", columnDefinition = "CHAR(1)")
	private String validacaoVet;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ID_CONSULTA", nullable = false)
	private Consulta consulta;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_DOENCA")
	private Doenca doenca;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getDiagnostico() {
		return diagnostico;
	}

	public void setDiagnostico(String diagnostico) {
		this.diagnostico = diagnostico;
	}

	public String getSeveridade() {
		return severidade;
	}

	public void setSeveridade(String severidade) {
		this.severidade = severidade;
	}

	public String getConfirmado() {
		return confirmado;
	}

	public void setConfirmado(String confirmado) {
		this.confirmado = confirmado;
	}

	public String getInsightIa() {
		return insightIa;
	}

	public void setInsightIa(String insightIa) {
		this.insightIa = insightIa;
	}

	public String getFontesIaJson() {
		return fontesIaJson;
	}

	public void setFontesIaJson(String fontesIaJson) {
		this.fontesIaJson = fontesIaJson;
	}

	public BigDecimal getConfianca() {
		return confianca;
	}

	public void setConfianca(BigDecimal confianca) {
		this.confianca = confianca;
	}

	public String getValidacaoVet() {
		return validacaoVet;
	}

	public void setValidacaoVet(String validacaoVet) {
		this.validacaoVet = validacaoVet;
	}

	public Consulta getConsulta() {
		return consulta;
	}

	public void setConsulta(Consulta consulta) {
		this.consulta = consulta;
	}

	public Doenca getDoenca() {
		return doenca;
	}

	public void setDoenca(Doenca doenca) {
		this.doenca = doenca;
	}

}
