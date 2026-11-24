package br.com.brumi.dto;

import br.com.brumi.domain.Categoria;
import br.com.brumi.domain.Produto;
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
	 * O banco guarda so o NOME do arquivo ("3f2a....png"). A API devolve o caminho publico
	 * ("/uploads/3f2a....png"), que o frontend junta com o endereco da API para montar o <img src>.
	 */
	public static final String PREFIXO_URL_IMAGEM = "/uploads/";

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
			produto.getImagens().stream().map(i -> PREFIXO_URL_IMAGEM + i.getCaminho()).toList());
	}
}
