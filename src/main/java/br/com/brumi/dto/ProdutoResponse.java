package br.com.brumi.dto;

import br.com.brumi.domain.Categoria;
import br.com.brumi.domain.Produto;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

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
	 *
	 * O banco guarda so o NOME do arquivo ("3f2a....png"). urlImagem transforma o nome no endereco publico
	 * (ver EnderecoImagens): "/uploads/3f2a....png" ou uma URL absoluta, se houver url-publica.
	 */
	public static ProdutoResponse de(Produto produto, Function<String, String> urlImagem) {
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
			produto.getImagens().stream().map(i -> urlImagem.apply(i.getCaminho())).toList());
	}
}
