package br.com.brumi.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Corpo do POST /api/produtos (criacao). O produto nasce sempre ATIVO.
 * (≈ ProdutoCreate do schemas.py.)
 */
public record ProdutoRequest(
	@NotBlank(message = "O nome do produto é obrigatório")
	@Size(max = 200, message = "O nome do produto deve ter no máximo 200 caracteres")
	String nome,

	@Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres")
	String descricao,

	// Opcional (como no sistema original). Se informado: nao negativo, ate 8 digitos inteiros e 2 decimais,
	// o mesmo limite da coluna NUMERIC(10,2).
	@PositiveOrZero(message = "O preço não pode ser negativo")
	@Digits(integer = 8, fraction = 2, message = "O preço deve ter no máximo 8 dígitos inteiros e 2 casas decimais")
	BigDecimal preco,

	// Obrigatorio, mas pode ser lista vazia (produto sem categoria), como no sistema original.
	@NotNull(message = "Informe a lista de categorias (pode ser vazia)")
	List<@NotNull(message = "Id de categoria inválido") Long> categoriasIds) {
}
