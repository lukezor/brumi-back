package br.com.brumi.controller;

import br.com.brumi.exception.RecursoNaoEncontradoException;
import br.com.brumi.service.ArmazenamentoImagens;
import io.swagger.v3.oas.annotations.Hidden;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /uploads/{nome}: entrega a foto, venha ela do disco ou do Cloud Storage.
 *
 * Cache-Control "public, max-age=1 ano, immutable": cada arquivo tem nome unico (UUID) e nunca muda. O navegador
 * e o CDN do Firebase Hosting podem guardar a copia para sempre, e a API quase nao e chamada para fotos.
 */
@Hidden // fora do Swagger: nao e um endpoint de dados
@RestController
public class ImagemController {

	private static final CacheControl CACHE_LONGO = CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable();

	private final ArmazenamentoImagens armazenamento;

	public ImagemController(ArmazenamentoImagens armazenamento) {
		this.armazenamento = armazenamento;
	}

	@GetMapping("/uploads/{nome}")
	public ResponseEntity<byte[]> ler(@PathVariable String nome) {
		var imagem = armazenamento.ler(nome)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Imagem não encontrada"));
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(imagem.tipo().mimeType()))
			.cacheControl(CACHE_LONGO)
			.body(imagem.conteudo());
	}
}
