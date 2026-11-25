package br.com.brumi.service;

import br.com.brumi.TestcontainersConfiguration;
import br.com.brumi.domain.Categoria;
import br.com.brumi.domain.Produto;
import br.com.brumi.dto.FiltroProduto;
import br.com.brumi.repository.CategoriaRepository;
import br.com.brumi.repository.ProdutoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base para medir QUANTAS consultas SQL uma listagem de 10 produtos dispara.
 * Cada subclasse liga as estatisticas do Hibernate e escolhe o valor de default_batch_fetch_size.
 */
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
abstract class ContagemDeConsultasBase {

	@Autowired
	ProdutoService service;
	@Autowired
	ProdutoRepository produtos;
	@Autowired
	CategoriaRepository categorias;
	@Autowired
	EntityManager em;
	@Autowired
	EntityManagerFactory emf;

	/** Cria 12 produtos, cada um com 2 categorias e 2 imagens, lista a 1a pagina (10) e devolve o no de SQLs. */
	long contarConsultasDaListagem() {
		Categoria a = categorias.save(new Categoria("A"));
		Categoria b = categorias.save(new Categoria("B"));
		for (int i = 0; i < 12; i++) {
			Produto p = new Produto("Produto " + i, null, new BigDecimal("10.00"));
			p.getCategorias().add(a);
			p.getCategorias().add(b);
			p.adicionarImagem("uploads/" + i + "-0.png");
			p.adicionarImagem("uploads/" + i + "-1.png");
			produtos.save(p);
		}
		// Grava no banco e ESVAZIA o cache do Hibernate: senao ele reaproveitaria os objetos em memoria
		// e nao precisaria consultar nada, escondendo o problema.
		em.flush();
		em.clear();

		Statistics estatisticas = emf.unwrap(SessionFactory.class).getStatistics();
		estatisticas.clear();

		var pagina = service.listar(new FiltroProduto(null, null, null, null, true), PageRequest.of(0, 10));

		org.assertj.core.api.Assertions.assertThat(pagina.conteudo()).hasSize(10);
		org.assertj.core.api.Assertions.assertThat(pagina.conteudo().get(0).categorias()).hasSize(2);
		org.assertj.core.api.Assertions.assertThat(pagina.conteudo().get(0).imagens()).hasSize(2);
		return estatisticas.getPrepareStatementCount();
	}
}
