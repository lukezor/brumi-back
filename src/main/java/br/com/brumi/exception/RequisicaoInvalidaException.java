package br.com.brumi.exception;

/**
 * Lancada quando o pedido e valido no formato mas viola uma regra (ex.: categoria que nao existe,
 * campo de ordenacao nao permitido). Vira HTTP 400.
 */
public class RequisicaoInvalidaException extends RuntimeException {

	public RequisicaoInvalidaException(String mensagem) {
		super(mensagem);
	}
}
