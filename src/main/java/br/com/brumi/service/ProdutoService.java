package br.com.brumi.service;

import br.com.brumi.domain.Categoria;
import br.com.brumi.domain.Produto;
import br.com.brumi.dto.FiltroProduto;
import br.com.brumi.dto.PaginaResponse;
import br.com.brumi.dto.ProdutoRequest;
import br.com.brumi.dto.ProdutoResponse;
import br.com.brumi.dto.ProdutoUpdateRequest;
import br.com.brumi.exception.RecursoNaoEncontradoException;
import br.com.brumi.exception.RequisicaoInvalidaException;
import br.com.brumi.repository.CategoriaRepository;
import br.com.brumi.repository.ProdutoRepository;
import br.com.brumi.repository.ProdutoSpecs;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Regras de negocio de produtos (≈ a parte de produto do crud.py). */
@Service
public class ProdutoService {

	/** Campos pelos quais o cliente pode ordenar. Qualquer outro e recusado (evita erro 500 e vazamento). */
	private static final Set<String> CAMPOS_ORDENAVEIS = Set.of("id", "nome", "preco");

	private final ProdutoRepository produtoRepository;
	private final CategoriaRepository categoriaRepository;

	public ProdutoService(ProdutoRepository produtoRepository, CategoriaRepository categoriaRepository) {
		this.produtoRepository = produtoRepository;
		this.categoriaRepository = categoriaRepository;
	}

	@Transactional(readOnly = true)
	public PaginaResponse<ProdutoResponse> listar(FiltroProduto filtro, Pageable pageable) {
		Pageable seguro = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
			ordenacaoSegura(pageable.getSort()));
		Page<Produto> pagina = produtoRepository.findAll(ProdutoSpecs.comFiltro(filtro), seguro);
		return PaginaResponse.de(pagina, ProdutoResponse::de);
	}

	@Transactional(readOnly = true)
	public ProdutoResponse obter(Long id) {
		return ProdutoResponse.de(buscar(id));
	}

	@Transactional
	public ProdutoResponse criar(ProdutoRequest request) {
		Produto produto = new Produto(request.nome().trim(), request.descricao(), request.preco());
		produto.getCategorias().addAll(buscarCategorias(request.categoriasIds()));
		return ProdutoResponse.de(produtoRepository.save(produto));
	}

	/**
	 * Atualizacao parcial. Repare que NAO chamamos save(): o produto foi carregado dentro da transacao,
	 * entao o Hibernate o "vigia" (dirty checking). Ao fim do metodo ele percebe o que mudou e emite
	 * o UPDATE sozinho.
	 */
	@Transactional
	public ProdutoResponse atualizar(Long id, ProdutoUpdateRequest request) {
		Produto produto = buscar(id);
		if (request.nome() != null) {
			produto.setNome(request.nome().trim());
		}
		if (request.descricao() != null) {
			produto.setDescricao(request.descricao());
		}
		if (request.preco() != null) {
			produto.setPreco(request.preco());
		}
		if (request.ativo() != null) {
			produto.setAtivo(request.ativo());
		}
		if (request.categoriasIds() != null) {
			List<Categoria> novas = buscarCategorias(request.categoriasIds());
			produto.getCategorias().clear();
			produto.getCategorias().addAll(novas);
		}
		return ProdutoResponse.de(produto);
	}

	@Transactional
	public void deletar(Long id) {
		produtoRepository.delete(buscar(id));
	}

	// ---------- auxiliares ----------

	private Produto buscar(Long id) {
		return produtoRepository.findById(id)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado"));
	}

	/** Busca as categorias pedidas. Se alguma nao existir, recusa a operacao inteira (400). */
	private List<Categoria> buscarCategorias(List<Long> ids) {
		Set<Long> distintos = new LinkedHashSet<>(ids);
		List<Categoria> encontradas = categoriaRepository.findAllById(distintos);
		if (encontradas.size() != distintos.size()) {
			Set<Long> faltando = new LinkedHashSet<>(distintos);
			encontradas.forEach(c -> faltando.remove(c.getId()));
			throw new RequisicaoInvalidaException("Categorias não encontradas: " + faltando);
		}
		return encontradas;
	}

	/**
	 * Valida os campos de ordenacao e garante uma ordem ESTAVEL. Sem criterio de desempate, itens com
	 * o mesmo valor (ex.: mesmo preco) podem trocar de lugar entre paginas. Acrescentamos "id" no fim.
	 */
	private Sort ordenacaoSegura(Sort sort) {
		if (sort.isUnsorted()) {
			return Sort.by("id");
		}
		List<Sort.Order> ordens = new ArrayList<>();
		for (Sort.Order ordem : sort) {
			if (!CAMPOS_ORDENAVEIS.contains(ordem.getProperty())) {
				throw new RequisicaoInvalidaException("Ordenação inválida: '" + ordem.getProperty()
					+ "'. Campos permitidos: " + String.join(", ", new java.util.TreeSet<>(CAMPOS_ORDENAVEIS)));
			}
			ordens.add(ordem);
		}
		if (ordens.stream().noneMatch(o -> o.getProperty().equals("id"))) {
			ordens.add(Sort.Order.asc("id"));
		}
		return Sort.by(ordens);
	}
}
