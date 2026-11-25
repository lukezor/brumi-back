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

	/** Maximo de imagens por produto. O banco garante o mesmo limite (CHECK posicao entre 0 e 4). */
	public static final int MAXIMO_IMAGENS = 5;

	/** Adiciona uma imagem no fim da lista. A regra do maximo fica no service, que da uma mensagem amigavel. */
	public void adicionarImagem(String caminho) {
		imagens.add(new ProdutoImagem(this, imagens.size(), caminho));
	}

	/**
	 * Remove a imagem da posicao "indice" e renumera as seguintes, para nao ficar buraco:
	 * [0 capa][1 detalhe][2 embalagem] -> remover(1) -> [0 capa][1 embalagem].
	 * Devolve o caminho do arquivo removido, para o service apaga-lo do disco.
	 *
	 * Por um instante, duas imagens tem a mesma posicao. A constraint UNIQUE DEFERRABLE do V1 so confere
	 * no fim da transacao, entao isso nao da erro.
	 */
	public String removerImagem(int indice) {
		String caminho = imagens.remove(indice).getCaminho();
		for (int i = 0; i < imagens.size(); i++) {
			imagens.get(i).setPosicao(i);
		}
		return caminho;
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
