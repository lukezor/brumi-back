package br.com.brumi.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.brumi.TestcontainersConfiguration;
import br.com.brumi.domain.Categoria;
import br.com.brumi.domain.Produto;
import br.com.brumi.domain.ProdutoImagem;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/**
 * @DataJpaTest: sobe so a camada de persistencia (entidades, repositories, Flyway). Cada teste roda
 * dentro de uma transacao que e desfeita no fim, entao um teste nao suja o banco do outro.
 *
 * replace = NONE: usa o Postgres real do Testcontainers, em vez de trocar por um banco em memoria.
 * Perfil "test": NAO carrega o seed de dev, entao o banco comeca vazio e os testes ficam previsiveis.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class ProdutoPersistenciaTest {

	@Autowired
	CategoriaRepository categorias;

	@Autowired
	ProdutoRepository produtos;

	@Autowired
	EntityManager em;

	/** Força o envio do SQL ao banco e esvazia o cache, para a leitura seguinte ir ao banco de verdade. */
	private void sincronizarComBanco() {
		em.flush();
		em.clear();
	}

	@Test
	void salvaProdutoComCategoriasEImagensEmOrdem() {
		Categoria cestas = categorias.save(new Categoria("Cestas"));
		Produto produto = new Produto("Cesta Especial", "Uma cesta", new BigDecimal("199.90"));
		produto.getCategorias().add(cestas);
		produto.adicionarImagem("uploads/a.png");
		produto.adicionarImagem("uploads/b.png");
		Long id = produtos.save(produto).getId();
		sincronizarComBanco();

		Produto lido = produtos.findById(id).orElseThrow();

		assertThat(lido.getCategorias()).extracting(Categoria::getNome).containsExactly("Cestas");
		assertThat(lido.getImagens()).extracting(ProdutoImagem::getCaminho)
			.containsExactly("uploads/a.png", "uploads/b.png");
		assertThat(lido.getImagens()).extracting(ProdutoImagem::getPosicao).containsExactly(0, 1);
		assertThat(lido.isAtivo()).isTrue();
	}

	@Test
	void precoMantemDuasCasasDecimais() {
		Long id = produtos.save(new Produto("Caneca", null, new BigDecimal("19.90"))).getId();
		sincronizarComBanco();

		assertThat(produtos.findById(id).orElseThrow().getPreco()).isEqualByComparingTo("19.90");
	}

	@Test
	void nomeDeCategoriaDuplicadoViolaUnicidade() {
		categorias.saveAndFlush(new Categoria("Kits"));

		assertThatThrownBy(() -> categorias.saveAndFlush(new Categoria("Kits")))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void removerProdutoRemoveSuasImagens() {
		Produto produto = new Produto("Kit", null, new BigDecimal("10.00"));
		produto.adicionarImagem("uploads/x.png");
		Long id = produtos.save(produto).getId();
		sincronizarComBanco();

		produtos.deleteById(id);
		sincronizarComBanco();

		Long restantes = em.createQuery("select count(i) from ProdutoImagem i", Long.class).getSingleResult();
		assertThat(restantes).isZero();
	}

	@Test
	void naoPermiteMaisDeCincoPosicoesDeImagem() {
		Produto produto = new Produto("Cesta cheia", null, new BigDecimal("10.00"));
		for (int i = 0; i < 6; i++) {
			produto.adicionarImagem("uploads/" + i + ".png");
		}

		// A regra "maximo 5" sera do service (Task 8), mas o banco tambem nos protege (CHECK posicao 0..4).
		assertThatThrownBy(() -> produtos.saveAndFlush(produto))
			.isInstanceOf(DataIntegrityViolationException.class);
	}
}
