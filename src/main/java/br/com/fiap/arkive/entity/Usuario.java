package br.com.fiap.arkive.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_ARKIVE_USUARIO")
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_USUARIO")
	private Long id;

	@Column(name = "NM_USUARIO", nullable = false, length = 150)
	private String nome;

	@Enumerated(EnumType.STRING)
	@Column(name = "TP_USUARIO", nullable = false, length = 20)
	private TipoUsuario tipo;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_RESPONSAVEL", unique = true)
	private Responsavel responsavel;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_VETERINARIO", unique = true)
	private Veterinario veterinario;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_CLINICA")
	private Clinica clinica;

	@Column(name = "DS_LOGIN", nullable = false, length = 200, unique = true)
	private String login;

	@Column(name = "DS_SENHA_HASH", nullable = false, length = 255)
	private String senhaHash;

	@Column(name = "DT_CADASTRO", nullable = false)
	private LocalDateTime dataCadastro = LocalDateTime.now();

	@Column(name = "ST_ATIVO", nullable = false, columnDefinition = "CHAR(1)")
	private String ativo = "S";

	@Column(name = "ST_TROCA_SENHA", nullable = false, columnDefinition = "CHAR(1)")
	private String trocaSenha = "N";

	@Column(name = "DT_ULTIMA_TROCA_SENHA")
	private LocalDateTime dataUltimaTrocaSenha;

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

	public TipoUsuario getTipo() {
		return tipo;
	}

	public void setTipo(TipoUsuario tipo) {
		this.tipo = tipo;
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

	public Clinica getClinica() {
		return clinica;
	}

	public void setClinica(Clinica clinica) {
		this.clinica = clinica;
	}

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public String getSenhaHash() {
		return senhaHash;
	}

	public void setSenhaHash(String senhaHash) {
		this.senhaHash = senhaHash;
	}

	public LocalDateTime getDataCadastro() {
		return dataCadastro;
	}

	public void setDataCadastro(LocalDateTime dataCadastro) {
		this.dataCadastro = dataCadastro;
	}

	public String getAtivo() {
		return ativo;
	}

	public void setAtivo(String ativo) {
		this.ativo = ativo;
	}

	public String getTrocaSenha() {
		return trocaSenha;
	}

	public void setTrocaSenha(String trocaSenha) {
		this.trocaSenha = trocaSenha;
	}

	public LocalDateTime getDataUltimaTrocaSenha() {
		return dataUltimaTrocaSenha;
	}

	public void setDataUltimaTrocaSenha(LocalDateTime dataUltimaTrocaSenha) {
		this.dataUltimaTrocaSenha = dataUltimaTrocaSenha;
	}

}
