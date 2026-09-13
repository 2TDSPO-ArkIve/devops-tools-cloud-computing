package br.com.fiap.arkive.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "TB_ARKIVE_CLINICA")
public class Clinica {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_CLINICA")
	private Long id;

	@Column(name = "NM_CLINICA", nullable = false, length = 150)
	private String nome;

	@Column(name = "DC_CNPJ", nullable = false, length = 14, unique = true)
	private String cnpj;

	@Column(name = "DS_ENDERECO", length = 255)
	private String endereco;

	@Column(name = "NR_CONTATO", length = 20)
	private String telefone;

	@Column(name = "DS_EMAIL", length = 200)
	private String email;

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

	public String getCnpj() {
		return cnpj;
	}

	public void setCnpj(String cnpj) {
		this.cnpj = cnpj;
	}

	public String getEndereco() {
		return endereco;
	}

	public void setEndereco(String endereco) {
		this.endereco = endereco;
	}

	public String getTelefone() {
		return telefone;
	}

	public void setTelefone(String telefone) {
		this.telefone = telefone;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getAtivo() {
		return ativo;
	}

	public void setAtivo(String ativo) {
		this.ativo = ativo;
	}

}
