package br.com.brumi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dados que o cliente ENVIA para criar uma categoria (corpo do POST).
 *
 * As anotacoes de Bean Validation (@NotBlank, @Size) descrevem as regras. Elas so sao conferidas
 * quando o controller recebe este objeto com @Valid. (≈ validacoes de um modelo Pydantic.)
 */
public record CategoriaRequest(
	@NotBlank(message = "O nome da categoria é obrigatório")
	@Size(max = 100, message = "O nome da categoria deve ter no máximo 100 caracteres")
	String nome) {
}
