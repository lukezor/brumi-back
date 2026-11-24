package br.com.brumi.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Envelope de uma pagina de resultados. Em vez de devolver o Page do Spring Data direto (o formato
 * do JSON dele nao e garantido entre versoes), definimos o NOSSO contrato estavel.
 *
 * Exemplo: { "conteudo": [...], "pagina": 0, "tamanho": 20, "totalElementos": 53, "totalPaginas": 3 }
 * "pagina" comeca em 0.
 */
public record PaginaResponse<T>(List<T> conteudo, int pagina, int tamanho, long totalElementos, int totalPaginas) {

	/** Converte um Page<E> (entidades) em PaginaResponse<T> (DTOs), aplicando "conversor" a cada item. */
	public static <E, T> PaginaResponse<T> de(Page<E> page, Function<E, T> conversor) {
		return new PaginaResponse<>(
			page.getContent().stream().map(conversor).toList(),
			page.getNumber(),
			page.getSize(),
			page.getTotalElements(),
			page.getTotalPages());
	}
}
