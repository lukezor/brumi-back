package br.com.brumi.repository;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.brumi.TestcontainersConfiguration;
import br.com.brumi.domain.Categoria;
import br.com.brumi.domain.Produto;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** Confere que o perfil "dev" carrega os dados de exemplo (db/seed/R__seed_dev.sql). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("dev")
class SeedDevTest {

	@Autowired
	CategoriaRepository categorias;

	@Autowired
	ProdutoRepository produtos;

	@Test
	void seedCriaCategoriasEProdutosDeExemplo() {
		assertThat(categorias.findAll()).extracting(Categoria::getNome)
			.containsExactlyInAnyOrder("Cestas", "Kits", "Presentes Especiais");

		List<Produto> todos = produtos.findAll();
		assertThat(todos).hasSize(5);

		Produto cesta = todos.stream().filter(p -> p.getNome().equals("Cesta Especial")).findFirst().orElseThrow();
		assertThat(cesta.getCategorias()).extracting(Categoria::getNome)
			.containsExactlyInAnyOrder("Cestas", "Presentes Especiais");
		assertThat(cesta.getPreco()).isEqualByComparingTo("199.90");

		assertThat(todos.stream().filter(p -> !p.isAtivo())).extracting(Produto::getNome)
			.containsExactly("Kit Descontinuado");
	}
}
