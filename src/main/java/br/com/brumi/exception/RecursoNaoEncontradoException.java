package br.com.brumi.exception;

/**
 * Lancada pelo service quando algo pedido nao existe. O GlobalExceptionHandler a transforma em HTTP 404.
 *
 * Estende RuntimeException ("unchecked"): nao precisa ser declarada com "throws" em cada metodo
 * do caminho. (≈ HTTPException(404) do FastAPI, mas sem conhecer HTTP: o service nao sabe de web.)
 */
public class RecursoNaoEncontradoException extends RuntimeException {

	public RecursoNaoEncontradoException(String mensagem) {
		super(mensagem);
	}
}
