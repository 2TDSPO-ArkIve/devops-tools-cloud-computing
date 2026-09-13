package br.com.fiap.arkive.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "TB_ARKIVE_ANIMAL")
public class Animal {

	@Column(name = "DT_NASCIMENTO")
	private LocalDate dataNascimento;

	public LocalDate getDataNascimento() {
		return dataNascimento;
	}

	public void setDataNascimento(LocalDate dataNascimento) {
		this.dataNascimento = dataNascimento;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_ANIMAL")
	private Long id;

	@Column(name = "NM_ANIMAL", nullable = false, length = 50)
	private String nome;

	@Column(name = "DS_SEXO", columnDefinition = "CHAR(1)")
	private String sexo;

	@Column(name = "DS_CASTRADO", nullable = false, columnDefinition = "CHAR(1)")
	private String castrado = "N";

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ID_ESPECIE", nullable = false)
	private Especie especie;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_RACA")
	private Raca raca;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_CLINICA")
	private Clinica clinica;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_VETERINARIO_CADASTRO")
	private Veterinario veterinarioCadastro;

	@Column(name = "ST_ATIVO", nullable = false, columnDefinition = "CHAR(1)")
	private String ativo = "S";

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getSexo() {
		return sexo;
	}

	public void setSexo(String sexo) {
		this.sexo = sexo;
	}

	public String getCastrado() {
		return castrado;
	}

	public void setCastrado(String castrado) {
		this.castrado = castrado;
	}

	public Especie getEspecie() {
		return especie;
	}

	public void setEspecie(Especie especie) {
		this.especie = especie;
	}

	public Raca getRaca() {
		return raca;
	}

	public void setRaca(Raca raca) {
		this.raca = raca;
	}

	public Clinica getClinica() {
		return clinica;
	}

	public void setClinica(Clinica clinica) {
		this.clinica = clinica;
	}

	public Veterinario getVeterinarioCadastro() {
		return veterinarioCadastro;
	}

	public void setVeterinarioCadastro(Veterinario veterinarioCadastro) {
		this.veterinarioCadastro = veterinarioCadastro;
	}

	public String getAtivo() {
		return ativo;
	}

	public void setAtivo(String ativo) {
		this.ativo = ativo;
	}

}
