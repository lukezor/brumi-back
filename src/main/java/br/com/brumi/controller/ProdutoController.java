package br.com.brumi.controller;

import br.com.brumi.dto.FiltroProduto;
import br.com.brumi.dto.PaginaResponse;
import br.com.brumi.dto.ProdutoRequest;
import br.com.brumi.dto.ProdutoResponse;
import br.com.brumi.dto.ProdutoUpdateRequest;
import br.com.brumi.service.ProdutoService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
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
public class ProdutoController {

	private final ProdutoService service;

	public ProdutoController(ProdutoService service) {
		this.service = service;
	}

	/**
	 * GET /api/produtos?nome=caneca&categorias=1,2&precoMin=10&precoMax=50&ativo=true&page=0&size=20&sort=preco,desc
	 *
	 * @RequestParam: le um parametro da query string. required=false = opcional.
	 * Pageable: o Spring monta sozinho a partir de ?page=&size=&sort= (limites em application.yaml).
	 * Lista de ids aceita "categorias=1&categorias=2" e tambem "categorias=1,2".
	 */
	@GetMapping
	public PaginaResponse<ProdutoResponse> listar(
			@RequestParam(required = false) String nome,
			@RequestParam(required = false) List<Long> categorias,
			@RequestParam(required = false) BigDecimal precoMin,
			@RequestParam(required = false) BigDecimal precoMax,
			@RequestParam(defaultValue = "true") boolean ativo,
			Pageable pageable) {
		return service.listar(new FiltroProduto(nome, categorias, precoMin, precoMax, ativo), pageable);
	}

	@GetMapping("/{id}")
	public ProdutoResponse obter(@PathVariable Long id) {
		return service.obter(id);
	}

	@PostMapping
	public ResponseEntity<ProdutoResponse> criar(@Valid @RequestBody ProdutoRequest request) {
		ProdutoResponse criado = service.criar(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(criado.id())
			.toUri();
		return ResponseEntity.created(location).body(criado);
	}

	/** Atualizacao parcial: so os campos enviados mudam. */
	@PutMapping("/{id}")
	public ProdutoResponse atualizar(@PathVariable Long id, @Valid @RequestBody ProdutoUpdateRequest request) {
		return service.atualizar(id, request);
	}

	/** 204 No Content: deu certo e nao ha corpo a devolver. */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deletar(@PathVariable Long id) {
		service.deletar(id);
		return ResponseEntity.noContent().build();
	}
}
