package br.com.brumi.repository;

import br.com.brumi.domain.Produto;
import br.com.brumi.dto.FiltroProduto;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/**
 * Monta a clausula WHERE da listagem a partir dos filtros, peca por peca.
 *
 * Uma Specification e um "pedaco de WHERE" reutilizavel e combinavel com AND. So entra no WHERE
 * o filtro que foi informado. (≈ o query = query.filter(...) condicional do crud.py,
 * so que sem concatenar texto SQL: tudo e parametrizado, sem risco de SQL injection.)
 */
public final class ProdutoSpecs {

	private ProdutoSpecs() {
	}

	public static Specification<Produto> comFiltro(FiltroProduto filtro) {
		List<Specification<Produto>> partes = new ArrayList<>();
		partes.add(ativoIgual(filtro.ativo()));
		if (filtro.nome() != null && !filtro.nome().isBlank()) {
			partes.add(nomeContem(filtro.nome()));
		}
		if (filtro.categorias() != null && !filtro.categorias().isEmpty()) {
			partes.add(emAlgumaCategoria(filtro.categorias()));
		}
		if (filtro.precoMin() != null) {
			partes.add((produto, query, cb) -> cb.greaterThanOrEqualTo(produto.get("preco"), filtro.precoMin()));
		}
		if (filtro.precoMax() != null) {
			partes.add((produto, query, cb) -> cb.lessThanOrEqualTo(produto.get("preco"), filtro.precoMax()));
		}
		return Specification.allOf(partes);
	}

	private static Specification<Produto> ativoIgual(boolean ativo) {
		return (produto, query, cb) -> cb.equal(produto.get("ativo"), ativo);
	}

	/** Contem o texto, ignorando maiusculas/minusculas (≈ ilike). Os curingas do usuario sao neutralizados. */
	private static Specification<Produto> nomeContem(String nome) {
		String padrao = "%" + escaparLike(nome.trim().toLowerCase(Locale.ROOT)) + "%";
		return (produto, query, cb) -> cb.like(cb.lower(produto.get("nome")), padrao, '\\');
	}

	/**
	 * Produto que esteja em PELO MENOS UMA das categorias (OR), como no sistema original.
	 *
	 * Usa EXISTS com subconsulta em vez de JOIN. Com JOIN, um produto presente em duas das categorias
	 * pedidas apareceria DUAS vezes na lista (e a paginacao contaria errado). O EXISTS nao duplica.
	 */
	private static Specification<Produto> emAlgumaCategoria(List<Long> ids) {
		return (produto, query, cb) -> {
			Subquery<Long> sub = query.subquery(Long.class);
			Root<Produto> p = sub.from(Produto.class);
			Join<Object, Object> categoria = p.join("categorias");
			sub.select(p.get("id")).where(
				cb.equal(p.get("id"), produto.get("id")),
				categoria.get("id").in(ids));
			return cb.exists(sub);
		};
	}

	/** No LIKE, % e _ sao curingas. Se o usuario digitar "100%", queremos o texto literal "100%". */
	private static String escaparLike(String texto) {
		return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}
}
