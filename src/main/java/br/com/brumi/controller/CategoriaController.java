package br.com.brumi.controller;

import br.com.brumi.dto.CategoriaRequest;
import br.com.brumi.dto.CategoriaResponse;
import br.com.brumi.service.CategoriaService;
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
 */
@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

	private final CategoriaService service;

	public CategoriaController(CategoriaService service) {
		this.service = service;
	}

	/** GET /api/categorias -> 200 com a lista (o Spring converte List<record> em JSON). */
	@GetMapping
	public List<CategoriaResponse> listar() {
		return service.listar();
	}

	/**
	 * GET /api/categorias/5 -> 200, ou 404 se nao existir (a excecao do service e tratada no handler).
	 * @PathVariable: pega o "5" da URL e converte para Long. (≈ categoria_id: int no FastAPI.)
	 */
	@GetMapping("/{id}")
	public CategoriaResponse obter(@PathVariable Long id) {
		return service.obter(id);
	}

	/**
	 * POST /api/categorias -> 201 Created.
	 * @RequestBody: converte o JSON do corpo em CategoriaRequest.
	 * @Valid: executa as validacoes do DTO ANTES de entrar no metodo. Se falhar, o metodo nem roda
	 * e o handler devolve 400.
	 * 201 + cabecalho Location (URL do recurso criado) e a convencao REST para criacao.
	 */
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
