package br.com.brumi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * Formato UNICO de erro de toda a API. O Angular so precisa saber ler este formato.
 *
 * Exemplo:
 * {
 *   "status": 400,
 *   "erro": "Bad Request",
 *   "mensagem": "Dados inválidos",
 *   "campos": [ { "campo": "nome", "mensagem": "O nome da categoria é obrigatório" } ]
 * }
 *
 * "campos" so aparece em erros de validacao (NON_NULL omite o que for null).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponse(int status, String erro, String mensagem, List<ErroCampo> campos) {

	public record ErroCampo(String campo, String mensagem) {
	}

	public static ErroResponse de(HttpStatus status, String mensagem) {
		return new ErroResponse(status.value(), status.getReasonPhrase(), mensagem, null);
	}

	public static ErroResponse de(HttpStatus status, String mensagem, List<ErroCampo> campos) {
		return new ErroResponse(status.value(), status.getReasonPhrase(), mensagem, campos);
	}
}
