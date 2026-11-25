package br.com.brumi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Usuario que pode entrar no painel administrativo. */
@Entity
@Table(name = "usuario")
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50)
	private String username;

	/** Hash BCrypt da senha. A senha em texto nunca e armazenada. */
	@Column(name = "senha_hash", nullable = false, length = 100)
	private String senhaHash;

	/** EnumType.STRING grava "ADMIN" no banco. (O padrao, ORDINAL, gravaria 0, que quebra se a ordem mudar.) */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Papel papel;

	@Column(nullable = false)
	private boolean ativo = true;

	protected Usuario() {
	}

	public Usuario(String username, String senhaHash, Papel papel) {
		this.username = username;
		this.senhaHash = senhaHash;
		this.papel = papel;
	}

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getSenhaHash() {
		return senhaHash;
	}

	public Papel getPapel() {
		return papel;
	}

	public boolean isAtivo() {
		return ativo;
	}

	public void setAtivo(boolean ativo) {
		this.ativo = ativo;
	}
}
