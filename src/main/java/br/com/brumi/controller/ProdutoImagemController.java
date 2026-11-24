package br.com.brumi.controller;

import br.com.brumi.config.OpenApiConfig;
import br.com.brumi.dto.ErroResponse;
import br.com.brumi.dto.ProdutoResponse;
import br.com.brumi.service.ProdutoImagemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Imagens de produto", description = "Envio e remoção das fotos de um produto (até 5)")
public class ProdutoImagemController {

	private final ProdutoImagemService service;

	public ProdutoImagemController(ProdutoImagemService service) {
		this.service = service;
	}

	/**
	 * multipart/form-data e o formato que o navegador usa para enviar arquivos (o FormData do JavaScript).
	 * MultipartFile representa cada arquivo recebido. (≈ imagens: List[UploadFile] = File(...) do FastAPI.)
	 */
	@Operation(summary = "Envia uma ou mais imagens",
		description = "PNG, JPEG ou WEBP (conferido pelo conteúdo), até 5MB cada, no máximo 5 por produto. "
			+ "Se um arquivo for inválido, nenhum é salvo.",
		security = @SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT))
	@ApiResponse(responseCode = "200", description = "Produto atualizado, com as URLs das imagens")
	@ApiResponse(responseCode = "400", description = "Tipo não aceito, arquivo vazio ou grande, ou mais de 5 imagens",
		content = @Content(schema = @Schema(implementation = ErroResponse.class)))
	@ApiResponse(responseCode = "401", description = "Sem token ou token inválido", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
	@ApiResponse(responseCode = "404", description = "Produto não existe", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
	@ApiResponse(responseCode = "413", description = "Envio maior que o limite do servidor", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ProdutoResponse adicionar(@PathVariable Long id, @RequestPart("imagens") List<MultipartFile> imagens) {
		return service.adicionar(id, imagens);
	}

	@Operation(summary = "Remove uma imagem pela posição",
		description = "As imagens seguintes sobem uma posição. O arquivo é apagado do disco.",
		security = @SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT))
	@ApiResponse(responseCode = "200", description = "Produto atualizado")
	@ApiResponse(responseCode = "400", description = "Posição fora da lista", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
	@ApiResponse(responseCode = "401", description = "Sem token ou token inválido", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
	@ApiResponse(responseCode = "404", description = "Produto não existe ou não tem imagens", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
	@DeleteMapping("/{indice}")
	public ProdutoResponse remover(@PathVariable Long id,
			@Parameter(description = "Posição da imagem, começando em 0") @PathVariable int indice) {
		return service.remover(id, indice);
	}
}
