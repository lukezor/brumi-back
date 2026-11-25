package br.com.brumi.exception;

/** Lancada quando a operacao conflita com o estado atual (ex.: nome duplicado). Vira HTTP 409. */
public class ConflitoException extends RuntimeException {

	public ConflitoException(String mensagem) {
		super(mensagem);
	}
}
