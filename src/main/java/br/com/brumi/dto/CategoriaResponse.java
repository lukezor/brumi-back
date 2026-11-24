package br.com.brumi.dto;

import br.com.brumi.domain.Categoria;

/**
 * Dados que a API DEVOLVE sobre uma categoria.
 *
 * Por que nao devolver a entidade Categoria direto? A entidade e um detalhe interno, ligado ao
 * banco (colecoes lazy, campos que nao queremos expor). O DTO e o "contrato" publico da API:
 * podemos mudar uma sem quebrar a outra.
 */
public record CategoriaResponse(Long id, String nome) {

	/** Converte entidade em DTO (mapeamento manual, sem biblioteca, por decisao do projeto). */
	public static CategoriaResponse de(Categoria categoria) {
		return new CategoriaResponse(categoria.getId(), categoria.getNome());
	}
}
