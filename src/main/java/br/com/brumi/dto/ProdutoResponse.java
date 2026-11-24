package br.com.brumi.dto;

import br.com.brumi.domain.Categoria;
import br.com.brumi.domain.Produto;
import br.com.brumi.domain.ProdutoImagem;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/** Produto como a API o devolve. (≈ ProdutoRead do schemas.py.) */
public record ProdutoResponse(
	Long id,
	String nome,
	String descricao,
	BigDecimal preco,
	boolean ativo,
	List<CategoriaResponse> categorias,
	List<String> imagens) {

	/**
	 * Converte a entidade em DTO. ATENCAO: acessa colecoes lazy (categorias e imagens), entao precisa
	 * ser chamado DENTRO de uma transacao (o service faz isso).
	 */
	public static ProdutoResponse de(Produto produto) {
		return new ProdutoResponse(
			produto.getId(),
			produto.getNome(),
			produto.getDescricao(),
			produto.getPreco(),
			produto.isAtivo(),
			produto.getCategorias().stream()
				.sorted(Comparator.comparing(Categoria::getNome))
				.map(CategoriaResponse::de)
				.toList(),
			produto.getImagens().stream().map(ProdutoImagem::getCaminho).toList());
	}
}
