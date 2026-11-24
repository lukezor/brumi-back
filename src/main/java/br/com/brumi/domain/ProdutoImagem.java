package br.com.brumi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Uma imagem de um produto. Substitui a antiga coluna ARRAY(String) do Python:
 * em vez de uma lista dentro da linha do produto, cada imagem e uma linha propria.
 */
@Entity
@Table(name = "produto_imagem")
public class ProdutoImagem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * Lado "muitos" da relacao: varias imagens para um produto. Na tabela, vira a coluna produto_id.
	 * LAZY: o produto so e carregado do banco se alguem realmente chamar getProduto().
	 */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "produto_id", nullable = false)
	private Produto produto;

	/** Posicao da imagem na lista do produto (0 = primeira). */
	@Column(nullable = false)
	private int posicao;

	/** Caminho/URL do arquivo, por exemplo "uploads/3f2a...png". */
	@Column(nullable = false, length = 500)
	private String caminho;

	protected ProdutoImagem() {
	}

	ProdutoImagem(Produto produto, int posicao, String caminho) {
		this.produto = produto;
		this.posicao = posicao;
		this.caminho = caminho;
	}

	public Long getId() {
		return id;
	}

	public Produto getProduto() {
		return produto;
	}

	public int getPosicao() {
		return posicao;
	}

	public void setPosicao(int posicao) {
		this.posicao = posicao;
	}

	public String getCaminho() {
		return caminho;
	}
}
