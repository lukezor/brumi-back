package br.com.brumi.controller;

import br.com.brumi.dto.ProdutoResponse;
import br.com.brumi.service.ProdutoImagemService;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Imagens de um produto. Escrita: so ROLE_ADMIN (regras do SecurityConfig para /api/produtos/**). */
@RestController
@RequestMapping("/api/produtos/{id}/imagens")
public class ProdutoImagemController {

	private final ProdutoImagemService service;

	public ProdutoImagemController(ProdutoImagemService service) {
		this.service = service;
	}

	/**
	 * POST /api/produtos/3/imagens  (multipart/form-data, um ou mais campos "imagens")
	 *
	 * multipart/form-data e o formato que o navegador usa para enviar arquivos (o FormData do JavaScript).
	 * MultipartFile representa cada arquivo recebido: nome original, tamanho, conteudo...
	 * (≈ imagens: List[UploadFile] = File(...) do FastAPI.)
	 *
	 * Devolve o produto atualizado (200), como no sistema original.
	 */
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ProdutoResponse adicionar(@PathVariable Long id, @RequestPart("imagens") List<MultipartFile> imagens) {
		return service.adicionar(id, imagens);
	}

	/** DELETE /api/produtos/3/imagens/0 -> remove a primeira imagem e devolve o produto atualizado. */
	@DeleteMapping("/{indice}")
	public ProdutoResponse remover(@PathVariable Long id, @PathVariable int indice) {
		return service.remover(id, indice);
	}
}
