package br.com.brumi.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Configuracao REAL do projeto (batch fetch = 50): a listagem de 10 produtos usa poucas consultas. */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class ProdutoSemNMaisUmTest extends ContagemDeConsultasBase {

	@Test
	void listagemDeDezProdutosUsaQuatroConsultas() {
		long consultas = contarConsultasDaListagem();

		// 1 (contagem total) + 1 (a pagina de produtos) + 1 (categorias dos 10 de uma vez) + 1 (imagens dos 10 de uma vez)
		assertThat(consultas).as("consultas SQL").isEqualTo(4);
	}
}
