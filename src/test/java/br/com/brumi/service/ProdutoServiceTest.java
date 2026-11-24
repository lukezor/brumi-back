package br.com.brumi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.brumi.domain.Categoria;
import br.com.brumi.domain.Produto;
import br.com.brumi.dto.FiltroProduto;
import br.com.brumi.dto.ProdutoRequest;
import br.com.brumi.dto.ProdutoResponse;
import br.com.brumi.dto.ProdutoUpdateRequest;
import br.com.brumi.exception.RecursoNaoEncontradoException;
import br.com.brumi.exception.RequisicaoInvalidaException;
import br.com.brumi.repository.CategoriaRepository;
import br.com.brumi.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

/** Teste unitario das regras do ProdutoService. Os filtros SQL ficam no teste de integracao. */
@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

	@Mock
	ProdutoRepository produtoRepository;

	@Mock
	CategoriaRepository categoriaRepository;

	@InjectMocks
	ProdutoService service;

	private static Categoria categoria(long id, String nome) {
		Categoria c = new Categoria(nome);
		ReflectionTestUtils.setField(c, "id", id);
		return c;
	}

	private static Produto produto() {
		Produto p = new Produto("Caneca", "Branca", new BigDecimal("39.90"));
		ReflectionTestUtils.setField(p, "id", 1L);
		return p;
	}

	@Test
	void criarRecusaCategoriasInexistentesListandoQuais() {
		when(categoriaRepository.findAllById(any())).thenReturn(List.of(categoria(1, "Cestas")));

		assertThatThrownBy(() -> service.criar(new ProdutoRequest("Caneca", null, null, List.of(1L, 7L, 9L))))
			.isInstanceOf(RequisicaoInvalidaException.class)
			.hasMessage("Categorias não encontradas: [7, 9]");
		verify(produtoRepository, never()).save(any());
	}

	@Test
	void criarAceitaIdsRepetidosEListaVazia() {
		when(categoriaRepository.findAllById(any())).thenReturn(List.of(categoria(1, "Cestas")));
		when(produtoRepository.save(any(Produto.class))).thenAnswer(inv -> inv.getArgument(0));

		ProdutoResponse comRepetido = service.criar(new ProdutoRequest("  Cesta  ", null, null, List.of(1L, 1L)));
		assertThat(comRepetido.nome()).isEqualTo("Cesta");
		assertThat(comRepetido.categorias()).hasSize(1);
		assertThat(comRepetido.ativo()).isTrue();

		when(categoriaRepository.findAllById(any())).thenReturn(List.of());
		assertThat(service.criar(new ProdutoRequest("Avulso", null, null, List.of())).categorias()).isEmpty();
	}

	@Test
	void atualizarMudaSoOsCamposEnviados() {
		Produto p = produto();
		when(produtoRepository.findById(1L)).thenReturn(Optional.of(p));

		ProdutoResponse r = service.atualizar(1L, new ProdutoUpdateRequest(null, null, new BigDecimal("45.00"), null, null));

		assertThat(r.preco()).isEqualByComparingTo("45.00");
		assertThat(r.nome()).isEqualTo("Caneca");
		assertThat(r.descricao()).isEqualTo("Branca");
		assertThat(r.ativo()).isTrue();
		verify(categoriaRepository, never()).findAllById(any());
	}

	@Test
	void atualizarComListaDeCategoriasSubstituiTodas() {
		Produto p = produto();
		p.getCategorias().add(categoria(1, "Cestas"));
		when(produtoRepository.findById(1L)).thenReturn(Optional.of(p));
		when(categoriaRepository.findAllById(any())).thenReturn(List.of(categoria(2, "Kits")));

		ProdutoResponse r = service.atualizar(1L, new ProdutoUpdateRequest(null, null, null, false, List.of(2L)));

		assertThat(r.categorias()).extracting("nome").containsExactly("Kits");
		assertThat(r.ativo()).isFalse();
	}

	@Test
	void atualizarComListaVaziaRemoveTodasAsCategorias() {
		Produto p = produto();
		p.getCategorias().add(categoria(1, "Cestas"));
		when(produtoRepository.findById(1L)).thenReturn(Optional.of(p));
		when(categoriaRepository.findAllById(any())).thenReturn(List.of());

		assertThat(service.atualizar(1L, new ProdutoUpdateRequest(null, null, null, null, List.of())).categorias()).isEmpty();
	}

	@Test
	void atualizarEDeletarInexistenteLancamNaoEncontrado() {
		when(produtoRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.atualizar(99L, new ProdutoUpdateRequest(null, null, null, null, null)))
			.isInstanceOf(RecursoNaoEncontradoException.class).hasMessage("Produto não encontrado");
		assertThatThrownBy(() -> service.deletar(99L)).isInstanceOf(RecursoNaoEncontradoException.class);
		verify(produtoRepository, never()).delete(any(Produto.class));
	}

	@Test
	void listarRecusaOrdenacaoPorCampoNaoPermitido() {
		FiltroProduto semFiltros = new FiltroProduto(null, null, null, null, true);

		assertThatThrownBy(() -> service.listar(semFiltros, PageRequest.of(0, 10, Sort.by("descricao"))))
			.isInstanceOf(RequisicaoInvalidaException.class)
			.hasMessageContaining("'descricao'")
			.hasMessageContaining("id, nome, preco");
	}
}
