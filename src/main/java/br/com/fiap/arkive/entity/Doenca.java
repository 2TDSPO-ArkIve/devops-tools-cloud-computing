package br.com.fiap.arkive.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "TB_ARKIVE_DOENCA")
public class Doenca {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_DOENCA")
	private Long id;

	@Column(name = "NM_DOENCA", nullable = false, length = 100)
	private String nome;

	@Column(name = "ID_CATEGORIA")
	private Long categoriaId;

	@Lob
	@Column(name = "DS_DOENCA")
	private String descricao;

	@Column(name = "CD_CID_VET", length = 20)
	private String cidVet;

	@Lob
	@Column(name = "DS_SINTOMAS")
	private String sintomas;

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

	public Long getCategoriaId() {
		return categoriaId;
	}

	public void setCategoriaId(Long categoriaId) {
		this.categoriaId = categoriaId;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public String getCidVet() {
		return cidVet;
	}

	public void setCidVet(String cidVet) {
		this.cidVet = cidVet;
	}

	public String getSintomas() {
		return sintomas;
	}

	public void setSintomas(String sintomas) {
		this.sintomas = sintomas;
	}

	public String getAtivo() {
		return ativo;
	}

	public void setAtivo(String ativo) {
		this.ativo = ativo;
	}

}
