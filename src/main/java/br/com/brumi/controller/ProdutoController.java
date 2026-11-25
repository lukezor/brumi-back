package br.com.brumi.controller;

import br.com.brumi.config.OpenApiConfig;
import br.com.brumi.dto.ErroResponse;
import br.com.brumi.dto.FiltroProduto;
import br.com.brumi.dto.PaginaResponse;
import br.com.brumi.dto.ProdutoRequest;
import br.com.brumi.dto.ProdutoResponse;
import br.com.brumi.dto.ProdutoUpdateRequest;
import br.com.brumi.service.ProdutoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Camada HTTP de produtos. Sem regra de negocio: delega ao service. */
@RestController
@RequestMapping("/api/produtos")
@Tag(name = "Produtos", description = "Catálogo de produtos: busca, cadastro, alteração e remoção")
public class ProdutoController {

	private final ProdutoService service;

	public ProdutoController(ProdutoService service) {
		this.service = service;
	}

	/**
	 * @ParameterObject: faz o Swagger mostrar page, size e sort como campos separados da query string
	 * (sem isso, o Pageable apareceria como um objeto JSON, que nao e como ele e enviado).
	 */
	@Operation(summary = "Lista produtos com filtros e paginação",
		description = "Todos os filtros são opcionais e se combinam com E. Sem `ativo`, mostra só os ativos. "
			+ "`sort` aceita id, nome ou preco (ex.: `preco,desc`). Máximo de 100 itens por página.")
	@ApiResponse(responseCode = "200", description = "Uma página de produtos")
	@ApiResponse(responseCode = "400", description = "Parâmetro com formato errado ou ordenação não permitida",
		content = @Content(schema = @Schema(implementation = ErroResponse.class)))
	@GetMapping
	public PaginaResponse<ProdutoResponse> listar(
			@Parameter(description = "Trecho do nome, sem diferenciar maiúsculas", example = "cesta")
			@RequestParam(required = false) String nome,
			@Parameter(description = "Ids de categorias. O produto precisa estar em pelo menos uma", example = "1,3")
			@RequestParam(required = false) List<Long> categorias,
			@Parameter(description = "Preço mínimo (inclusivo)", example = "50")
			@RequestParam(required = false) BigDecimal precoMin,
			@Parameter(description = "Preço máximo (inclusivo)", example = "200")
			@RequestParam(required = false) BigDecimal precoMax,
			@Parameter(description = "true = só ativos, false = só inativos")
			@RequestParam(defaultValue = "true") boolean ativo,
			@ParameterObject Pageable pageable) {
		return service.listar(new FiltroProduto(nome, categorias, precoMin, precoMax, ativo), pageable);
	}

	@Operation(summary = "Busca um produto pelo id", description = "Encontra também produtos inativos.")
	@ApiResponse(responseCode = "200", description = "Encontrado")
	@ApiResponse(responseCode = "404", description = "Não existe", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
	@GetMapping("/{id}")
	public ProdutoResponse obter(@PathVariable Long id) {
		return service.obter(id);
	}

	@Operation(summary = "Cria um produto", description = "O produto nasce ativo e sem imagens.",
		security = @SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT))
	@ApiResponse(responseCode = "201", description = "Criado. O cabeçalho Location traz a URL do novo produto")
	@ApiResponse(responseCode = "400", description = "Dados inválidos ou categoria inexistente",
		content = @Content(schema = @Schema(implementation = ErroResponse.class)))
	@ApiResponse(responseCode = "401", description = "Sem token ou token inválido", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
	@PostMapping
	public ResponseEntity<ProdutoResponse> criar(@Valid @RequestBody ProdutoRequest request) {
		ProdutoResponse criado = service.criar(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(criado.id())
			.toUri();
		return ResponseEntity.created(location).body(criado);
	}

	@Operation(summary = "Altera um produto (parcial)",
		description = "Só os campos enviados mudam. `categoriasIds` substitui todas as categorias (lista vazia remove todas).",
		security = @SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT))
	@ApiResponse(responseCode = "200", description = "Alterado")
	@ApiResponse(responseCode = "400", description = "Dados inválidos ou categoria inexistente",
		content = @Content(schema = @Schema(implementation = ErroResponse.class)))
	@ApiResponse(responseCode = "401", description = "Sem token ou token inválido", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
	@ApiResponse(responseCode = "404", description = "Produto não existe", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
	@PutMapping("/{id}")
	public ProdutoResponse atualizar(@PathVariable Long id, @Valid @RequestBody ProdutoUpdateRequest request) {
		return service.atualizar(id, request);
	}

	@Operation(summary = "Apaga um produto", description = "Apaga também as imagens dele.",
		security = @SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT))
	@ApiResponse(responseCode = "204", description = "Apagado")
	@ApiResponse(responseCode = "401", description = "Sem token ou token inválido", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
	@ApiResponse(responseCode = "404", description = "Produto não existe", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deletar(@PathVariable Long id) {
		service.deletar(id);
		return ResponseEntity.noContent().build();
	}
}
