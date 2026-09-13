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

@Entity
@Table(name = "TB_ARKIVE_VETERINARIO")
public class Veterinario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_VETERINARIO")
	private Long id;

	@Column(name = "NM_VETERINARIO", nullable = false, length = 50)
	private String nome;

	@Column(name = "DC_CRMV", nullable = false, length = 10, unique = true)
	private String crmv;

	@Column(name = "DS_ESPECIALIDADE", length = 50)
	private String especialidade;

	@Column(name = "DS_EMAIL", length = 200)
	private String email;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_CLINICA")
	private Clinica clinica;

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

	public String getCrmv() {
		return crmv;
	}

	public void setCrmv(String crmv) {
		this.crmv = crmv;
	}

	public String getEspecialidade() {
		return especialidade;
	}

	public void setEspecialidade(String especialidade) {
		this.especialidade = especialidade;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Clinica getClinica() {
		return clinica;
	}

	public void setClinica(Clinica clinica) {
		this.clinica = clinica;
	}

	public String getAtivo() {
		return ativo;
	}

	public void setAtivo(String ativo) {
		this.ativo = ativo;
	}

}
