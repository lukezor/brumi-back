package br.com.brumi.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Entidade Produto. Tem duas relacoes:
 *  - N:N com Categoria (tabela de juncao produto_categoria);
 *  - 1:N com ProdutoImagem (as imagens pertencem ao produto).
 */
@Entity
@Table(name = "produto")
public class Produto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 200)
	private String nome;

	@Column(length = 1000)
	private String descricao;

	/** BigDecimal: decimal exato. O Float do Python acumulava erros de arredondamento. */
	@Column(precision = 10, scale = 2)
	private BigDecimal preco;

	@Column(nullable = false)
	private boolean ativo = true;

	/**
	 * Relacao muitos-para-muitos. O Produto e o "dono" da relacao e descreve a tabela de juncao.
	 * A Categoria nao conhece seus produtos (relacao unidirecional): ainda nao precisamos disso.
	 * LAZY e o padrao em colecoes: as categorias so sao buscadas no banco quando acessadas.
	 */
	@ManyToMany
	@JoinTable(
		name = "produto_categoria",
		joinColumns = @JoinColumn(name = "produto_id"),
		inverseJoinColumns = @JoinColumn(name = "categoria_id"))
	private Set<Categoria> categorias = new LinkedHashSet<>();

	/**
	 * mappedBy = "produto": o dono desta relacao e o campo "produto" de ProdutoImagem.
	 * cascade ALL: salvar/remover o produto salva/remove suas imagens junto.
	 * orphanRemoval: tirar uma imagem da lista apaga a linha dela no banco.
	 * @OrderBy: ao carregar, as imagens vem ordenadas pela posicao.
	 */
	@OneToMany(mappedBy = "produto", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("posicao ASC")
	private List<ProdutoImagem> imagens = new ArrayList<>();

	protected Produto() {
	}

	public Produto(String nome, String descricao, BigDecimal preco) {
		this.nome = nome;
		this.descricao = descricao;
		this.preco = preco;
	}

	/** Adiciona uma imagem no fim da lista. A regra de maximo de 5 fica no service (Task 8). */
	public void adicionarImagem(String caminho) {
		imagens.add(new ProdutoImagem(this, imagens.size(), caminho));
	}

	public Long getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public BigDecimal getPreco() {
		return preco;
	}

	public void setPreco(BigDecimal preco) {
		this.preco = preco;
	}

	public boolean isAtivo() {
		return ativo;
	}

	public void setAtivo(boolean ativo) {
		this.ativo = ativo;
	}

	public Set<Categoria> getCategorias() {
		return categorias;
	}

	public List<ProdutoImagem> getImagens() {
		return imagens;
	}
}
