package br.com.brumi.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * DEMONSTRACAO do problema que a configuracao resolve. Com default_batch_fetch_size=1 (= sem lote),
 * o Hibernate busca as categorias e imagens de UM produto por vez: o clasico N+1.
 * Este teste existe para documentar o "antes" e provar que o "depois" (ProdutoSemNMaisUmTest) melhorou.
 */
@SpringBootTest(properties = {
	"spring.jpa.properties.hibernate.generate_statistics=true",
	"spring.jpa.properties.hibernate.default_batch_fetch_size=1"
})
class ProdutoNMaisUmDemonstracaoTest extends ContagemDeConsultasBase {

	@Test
	void semLoteAListagemDisparaUmaConsultaPorProdutoParaCadaColecao() {
		long consultas = contarConsultasDaListagem();

		// 1 (contagem) + 1 (pagina) + 10 (categorias, uma por produto) + 10 (imagens, uma por produto) = 22
		assertThat(consultas).as("consultas SQL").isEqualTo(22);
	}
}
