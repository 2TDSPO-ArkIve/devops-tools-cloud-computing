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
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_ARKIVE_CONSULTA")
public class Consulta {

	@Column(name = "DS_ENDERECO", length = 255)
	private String endereco;

	public String getEndereco() {
		return endereco;
	}

	public void setEndereco(String endereco) {
		this.endereco = endereco;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_CONSULTA")
	private Long id;

	@Column(name = "DT_HORA", nullable = false)
	private LocalDateTime dataHora;

	@Column(name = "TP_MODALIDADE", nullable = false, length = 20)
	private String modalidade;

	@Lob
	@Column(name = "DS_MOTIVO", nullable = false)
	private String motivo;

	@Lob
	@Column(name = "DS_SINTOMAS")
	private String sintomas;

	@Lob
	@Column(name = "DS_OBSERVACAO")
	private String observacao;

	@Column(name = "KG_PESO", precision = 6, scale = 2)
	private BigDecimal peso;

	@Lob
	@Column(name = "DS_TRANSCRICAO")
	private String transcricao;

	@Column(name = "ST_STATUS", nullable = false, columnDefinition = "CHAR(2)")
	private String status = "AG";

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ID_ANIMAL", nullable = false)
	private Animal animal;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ID_VETERINARIO", nullable = false)
	private Veterinario veterinario;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_CLINICA")
	private Clinica clinica;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public LocalDateTime getDataHora() {
		return dataHora;
	}

	public void setDataHora(LocalDateTime dataHora) {
		this.dataHora = dataHora;
	}

	public String getModalidade() {
		return modalidade;
	}

	public void setModalidade(String modalidade) {
		this.modalidade = modalidade;
	}

	public String getMotivo() {
		return motivo;
	}

	public void setMotivo(String motivo) {
		this.motivo = motivo;
	}

	public String getSintomas() {
		return sintomas;
	}

	public void setSintomas(String sintomas) {
		this.sintomas = sintomas;
	}

	public String getObservacao() {
		return observacao;
	}

	public void setObservacao(String observacao) {
		this.observacao = observacao;
	}

	public BigDecimal getPeso() {
		return peso;
	}

	public void setPeso(BigDecimal peso) {
		this.peso = peso;
	}

	public String getTranscricao() {
		return transcricao;
	}

	public void setTranscricao(String transcricao) {
		this.transcricao = transcricao;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Animal getAnimal() {
		return animal;
	}

	public void setAnimal(Animal animal) {
		this.animal = animal;
	}

	public Veterinario getVeterinario() {
		return veterinario;
	}

	public void setVeterinario(Veterinario veterinario) {
		this.veterinario = veterinario;
	}

	public Clinica getClinica() {
		return clinica;
	}

	public void setClinica(Clinica clinica) {
		this.clinica = clinica;
	}

}
