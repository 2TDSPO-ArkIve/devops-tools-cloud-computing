package br.com.fiap.arkive.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "TB_ARKIVE_RESPONSAVEL")
public class Responsavel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_RESPONSAVEL")
	private Long id;

	@Column(name = "NM_RESPONSAVEL", nullable = false, length = 50)
	private String nome;

	@Column(name = "DC_CPF_RG", nullable = false, length = 20, unique = true)
	private String documento;

	@Column(name = "DS_EMAIL", length = 200)
	private String email;

	@Column(name = "NR_CONTATO", length = 20)
	private String telefone;

	@Column(name = "TP_RESPONSAVEL", nullable = false, length = 30)
	private String tipo;

	@Column(name = "DT_CADASTRO", nullable = false)
	private LocalDate dataCadastro = LocalDate.now();

	@Column(name = "ST_NOTIFICACAO", nullable = false, columnDefinition = "CHAR(1)")
	private String notificacao = "S";

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

	public String getDocumento() {
		return documento;
	}

	public void setDocumento(String documento) {
		this.documento = documento;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getTelefone() {
		return telefone;
	}

	public void setTelefone(String telefone) {
		this.telefone = telefone;
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public LocalDate getDataCadastro() {
		return dataCadastro;
	}

	public void setDataCadastro(LocalDate dataCadastro) {
		this.dataCadastro = dataCadastro;
	}

	public String getNotificacao() {
		return notificacao;
	}

	public void setNotificacao(String notificacao) {
		this.notificacao = notificacao;
	}

	public String getAtivo() {
		return ativo;
	}

	public void setAtivo(String ativo) {
		this.ativo = ativo;
	}

}
