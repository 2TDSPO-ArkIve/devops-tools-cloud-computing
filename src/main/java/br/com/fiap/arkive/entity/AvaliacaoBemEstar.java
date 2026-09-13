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
@Table(name = "TB_ARKIVE_AVALIACAO_BEM_ESTAR")
public class AvaliacaoBemEstar {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_AVALIACAO_BEM_ESTAR")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ID_ANIMAL", nullable = false)
	private Animal animal;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_RESPONSAVEL")
	private Responsavel responsavel;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_VETERINARIO")
	private Veterinario veterinario;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_CONSULTA")
	private Consulta consulta;

	@Column(name = "DT_AVALIACAO", nullable = false)
	private LocalDateTime dataAvaliacao = LocalDateTime.now();

	@Column(name = "KG_PESO", precision = 6, scale = 2)
	private BigDecimal peso;

	@Column(name = "NR_IDADE", precision = 5, scale = 2)
	private BigDecimal idade;

	@Column(name = "DS_APETITE", length = 20)
	private String apetite;

	@Column(name = "DS_ATIVIDADE", length = 20)
	private String atividade;

	@Column(name = "DS_COMPORTAMENTO", length = 30)
	private String comportamento;

	@Lob
	@Column(name = "DS_OBSERVACAO")
	private String observacao;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Animal getAnimal() {
		return animal;
	}

	public void setAnimal(Animal animal) {
		this.animal = animal;
	}

	public Responsavel getResponsavel() {
		return responsavel;
	}

	public void setResponsavel(Responsavel responsavel) {
		this.responsavel = responsavel;
	}

	public Veterinario getVeterinario() {
		return veterinario;
	}

	public void setVeterinario(Veterinario veterinario) {
		this.veterinario = veterinario;
	}

	public Consulta getConsulta() {
		return consulta;
	}

	public void setConsulta(Consulta consulta) {
		this.consulta = consulta;
	}

	public LocalDateTime getDataAvaliacao() {
		return dataAvaliacao;
	}

	public void setDataAvaliacao(LocalDateTime dataAvaliacao) {
		this.dataAvaliacao = dataAvaliacao;
	}

	public BigDecimal getPeso() {
		return peso;
	}

	public void setPeso(BigDecimal peso) {
		this.peso = peso;
	}

	public BigDecimal getIdade() {
		return idade;
	}

	public void setIdade(BigDecimal idade) {
		this.idade = idade;
	}

	public String getApetite() {
		return apetite;
	}

	public void setApetite(String apetite) {
		this.apetite = apetite;
	}

	public String getAtividade() {
		return atividade;
	}

	public void setAtividade(String atividade) {
		this.atividade = atividade;
	}

	public String getComportamento() {
		return comportamento;
	}

	public void setComportamento(String comportamento) {
		this.comportamento = comportamento;
	}

	public String getObservacao() {
		return observacao;
	}

	public void setObservacao(String observacao) {
		this.observacao = observacao;
	}

}
