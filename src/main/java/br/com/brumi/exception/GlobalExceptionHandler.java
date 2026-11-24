package br.com.brumi.exception;

import br.com.brumi.dto.ErroResponse;
import br.com.brumi.dto.ErroResponse.ErroCampo;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Ponto UNICO onde excecoes viram respostas HTTP. (≈ @app.exception_handler do FastAPI.)
 *
 * @RestControllerAdvice: "conselheiro" global de todos os controllers.
 * Cada @ExceptionHandler diz: "quando esta excecao escapar de um controller, responda assim".
 *
 * Estendemos ResponseEntityExceptionHandler, que ja trata os erros padrao do Spring MVC
 * (JSON malformado, tipo errado na URL, metodo nao permitido, etc.). Aqui so os reescrevemos
 * para sairem no nosso formato (ErroResponse).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	// ---------- Nossas excecoes de negocio ----------

	@ExceptionHandler(RecursoNaoEncontradoException.class)
	ResponseEntity<Object> naoEncontrado(RecursoNaoEncontradoException ex) {
		return resposta(HttpStatus.NOT_FOUND, ex.getMessage(), null);
	}

	@ExceptionHandler(ConflitoException.class)
	ResponseEntity<Object> conflito(ConflitoException ex) {
		return resposta(HttpStatus.CONFLICT, ex.getMessage(), null);
	}

	/**
	 * Rede de seguranca: se duas requisicoes criarem o mesmo nome AO MESMO TEMPO, a checagem do service
	 * passa nas duas e o banco (UNIQUE) barra a segunda. Tratamos como conflito, nao como erro 500.
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<Object> integridade(DataIntegrityViolationException ex) {
		log.warn("Violação de integridade de dados: {}", ex.getMostSpecificCause().getMessage());
		return resposta(HttpStatus.CONFLICT, "A operação viola uma regra de integridade dos dados", null);
	}

	// ---------- Erro inesperado (bug nosso) ----------

	/**
	 * Ultima linha de defesa. O detalhe completo vai para o LOG (para voce investigar), mas o cliente
	 * recebe uma mensagem generica: nunca devolva stack trace ou mensagem interna (vaza informacao).
	 */
	@ExceptionHandler(Exception.class)
	ResponseEntity<Object> inesperado(Exception ex) {
		log.error("Erro não tratado", ex);
		return resposta(HttpStatus.INTERNAL_SERVER_ERROR,
			"Ocorreu um erro inesperado. Tente novamente mais tarde.", null);
	}

	// ---------- Erros padrao do Spring MVC, reescritos no nosso formato ----------

	/** Falha do @Valid: devolve a lista de campos invalidos. */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<ErroCampo> campos = ex.getBindingResult().getFieldErrors().stream()
			.map(e -> new ErroCampo(e.getField(), e.getDefaultMessage()))
			.toList();
		return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos", campos);
	}

	/** Todos os demais erros padrao do Spring passam por aqui. Trocamos o corpo pelo nosso formato. */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
			HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
		HttpStatus status = HttpStatus.valueOf(statusCode.value());
		return ResponseEntity.status(status).headers(headers)
			.body(ErroResponse.de(status, mensagemPadrao(status)));
	}

	// ---------- Auxiliares ----------

	private ResponseEntity<Object> resposta(HttpStatus status, String mensagem, List<ErroCampo> campos) {
		return ResponseEntity.status(status).body(ErroResponse.de(status, mensagem, campos));
	}

	/** Mensagens em portugues para os erros padrao (as originais do Spring estao em ingles e tecnicas). */
	private String mensagemPadrao(HttpStatus status) {
		return switch (status) {
			case BAD_REQUEST -> "Requisição inválida";
			case NOT_FOUND -> "Recurso não encontrado";
			case METHOD_NOT_ALLOWED -> "Método HTTP não permitido para este recurso";
			case NOT_ACCEPTABLE -> "Formato de resposta não suportado";
			case UNSUPPORTED_MEDIA_TYPE -> "Tipo de conteúdo não suportado";
			default -> status.is5xxServerError()
				? "Ocorreu um erro inesperado. Tente novamente mais tarde."
				: "Não foi possível processar a requisição";
		};
	}
}
