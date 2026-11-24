package br.com.brumi.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.brumi.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Teste de INTEGRACAO: aplicacao inteira (controller -> service -> repository -> Postgres real).
 * E o teste mais proximo do uso real. @Transactional desfaz o que cada teste gravou.
 * Perfil "test": banco vazio (sem o seed de dev).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
class CategoriaApiIntegracaoTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void criaELista() throws Exception {
		mockMvc.perform(post("/api/categorias").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"  Cestas \"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.nome").value("Cestas"));

		mockMvc.perform(post("/api/categorias").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Artesanato\"}"))
			.andExpect(status().isCreated());

		mockMvc.perform(get("/api/categorias"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].nome").value("Artesanato"))
			.andExpect(jsonPath("$[1].nome").value("Cestas"));
	}

	@Test
	void naoPermiteNomeDuplicado() throws Exception {
		mockMvc.perform(post("/api/categorias").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Kits\"}"))
			.andExpect(status().isCreated());

		mockMvc.perform(post("/api/categorias").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Kits\"}"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.mensagem").value("Já existe uma categoria com o nome 'Kits'"));
	}

	@Test
	void buscaPorIdExistenteEInexistente() throws Exception {
		String corpo = mockMvc.perform(post("/api/categorias").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Kits\"}"))
			.andReturn().getResponse().getContentAsString();
		String id = corpo.replaceAll(".*\"id\":(\\d+).*", "$1");

		mockMvc.perform(get("/api/categorias/" + id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nome").value("Kits"));

		mockMvc.perform(get("/api/categorias/999999"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.mensagem").value("Categoria não encontrada"));
	}
}
