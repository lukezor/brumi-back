package br.com.brumi.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.brumi.TestcontainersConfiguration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Confere a especificacao OpenAPI gerada (o JSON por tras do Swagger UI). Nao testa a tela em si, mas
 * garante que a documentacao esta completa e coerente com as regras de seguranca.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class DocumentacaoApiTest {

	@Autowired
	MockMvc mvc;

	DocumentContext spec;

	@BeforeEach
	void lerEspecificacao() throws Exception {
		String json = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		spec = JsonPath.parse(json);
	}

	@Test
	void swaggerUiEEspecificacaoSaoPublicos() throws Exception {
		mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
		mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
	}

	@Test
	void documentaTodasAsRotasDaApi() {
		Map<String, Object> rotas = spec.read("$.paths");
		assertThat(rotas).containsKeys(
			"/api/ping", "/api/auth/login",
			"/api/categorias", "/api/categorias/{id}",
			"/api/produtos", "/api/produtos/{id}",
			"/api/produtos/{id}/imagens", "/api/produtos/{id}/imagens/{indice}");
		assertThat(spec.read("$.info.title", String.class)).isEqualTo("Brumi Presentes API");
	}

	@Test
	void declaraOEsquemaBearerJwt() {
		assertThat(spec.read("$.components.securitySchemes.bearer-jwt.scheme", String.class)).isEqualTo("bearer");
		assertThat(spec.read("$.components.securitySchemes.bearer-jwt.bearerFormat", String.class)).isEqualTo("JWT");
	}

	@Test
	void cadeadoSoNasOperacoesDeEscrita() {
		// Protegidas: precisam ter "security".
		for (String operacao : List.of(
				"$.paths['/api/categorias'].post", "$.paths['/api/produtos'].post",
				"$.paths['/api/produtos/{id}'].put", "$.paths['/api/produtos/{id}'].delete",
				"$.paths['/api/produtos/{id}/imagens'].post", "$.paths['/api/produtos/{id}/imagens/{indice}'].delete")) {
			assertThat(spec.read(operacao + ".security", List.class)).as(operacao).isNotEmpty();
		}
		// Publicas: nao podem ter.
		for (String operacao : List.of(
				"$.paths['/api/categorias'].get", "$.paths['/api/produtos'].get", "$.paths['/api/produtos/{id}'].get",
				"$.paths['/api/auth/login'].post", "$.paths['/api/ping'].get")) {
			Map<String, Object> op = spec.read(operacao);
			assertThat(op).as(operacao).doesNotContainKey("security");
		}
	}

	@Test
	void paginacaoApareceComoParametrosDaUrl() {
		List<String> parametros = spec.read("$.paths['/api/produtos'].get.parameters[*].name");
		assertThat(parametros).contains("nome", "categorias", "precoMin", "precoMax", "ativo", "page", "size", "sort");
	}

	@Test
	void uploadEDocumentadoComoMultipart() {
		Map<String, Object> corpo = spec.read("$.paths['/api/produtos/{id}/imagens'].post.requestBody.content");
		assertThat(corpo).containsKey("multipart/form-data");
	}
}
