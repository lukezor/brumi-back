package br.com.brumi.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Corpo do PUT /api/produtos/{id}: ATUALIZACAO PARCIAL.
 * Campo ausente (null) = "nao mexer". So o que for enviado e alterado. (≈ ProdutoUpdate do schemas.py.)
 *
 * Consequencia (igual ao sistema original): por aqui nao da para "apagar" descricao ou preco
 * enviando null, pois null significa "nao alterar".
 * categoriasIds: ausente = manter; lista (mesmo vazia) = SUBSTITUI todas as categorias.
 */
public record ProdutoUpdateRequest(
	// A regex rejeita nome so com espacos. Valores null passam (campo opcional).
	@Pattern(regexp = ".*\\S.*", message = "O nome do produto não pode ser vazio")
	@Size(max = 200, message = "O nome do produto deve ter no máximo 200 caracteres")
	String nome,

	@Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres")
	String descricao,

	@PositiveOrZero(message = "O preço não pode ser negativo")
	@Digits(integer = 8, fraction = 2, message = "O preço deve ter no máximo 8 dígitos inteiros e 2 casas decimais")
	BigDecimal preco,

	Boolean ativo,

	List<@jakarta.validation.constraints.NotNull(message = "Id de categoria inválido") Long> categoriasIds) {
}
