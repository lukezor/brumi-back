package br.com.brumi.controller;

import br.com.brumi.config.OpenApiConfig;
import br.com.brumi.dto.CategoriaRequest;
import br.com.brumi.dto.CategoriaResponse;
import br.com.brumi.dto.ErroResponse;
import br.com.brumi.service.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Camada HTTP: traduz requisicoes em chamadas ao service e devolve as respostas.
 * Nao tem regra de negocio. (≈ as rotas @app.get / @app.post do main.py.)
 *
 * As anotacoes io.swagger.* so alimentam a documentacao (Swagger). Nao mudam o comportamento.
 */
@RestController
@RequestMapping("/api/categorias")
@Tag(name = "Categorias", description = "Categorias do catálogo")
public class CategoriaController {

	private final CategoriaService service;

	public CategoriaController(CategoriaService service) {
		this.service = service;
	}

	@Operation(summary = "Lista todas as categorias", description = "Ordenadas por nome. Pública.")
	@GetMapping
	public List<CategoriaResponse> listar() {
		return service.listar();
	}

	@Operation(summary = "Busca uma categoria pelo id")
	@ApiResponse(responseCode = "200", description = "Encontrada")
	@ApiResponse(responseCode = "404", description = "Não existe", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
	@GetMapping("/{id}")
	public CategoriaResponse obter(@PathVariable Long id) {
		return service.obter(id);
	}

	/**
	 * 201 + cabecalho Location (URL do recurso criado) e a convencao REST para criacao.
	 * @Valid executa as validacoes do DTO ANTES de entrar no metodo.
	 */
	@Operation(summary = "Cria uma categoria", security = @SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT))
	@ApiResponse(responseCode = "201", description = "Criada. O cabeçalho Location traz a URL da nova categoria")
	@ApiResponse(responseCode = "400", description = "Nome vazio ou longo demais", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
	@ApiResponse(responseCode = "401", description = "Sem token ou token inválido", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
	@ApiResponse(responseCode = "409", description = "Já existe uma categoria com esse nome", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
	@PostMapping
	public ResponseEntity<CategoriaResponse> criar(@Valid @RequestBody CategoriaRequest request) {
		CategoriaResponse criada = service.criar(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(criada.id())
			.toUri();
		return ResponseEntity.created(location).body(criada);
	}
}
