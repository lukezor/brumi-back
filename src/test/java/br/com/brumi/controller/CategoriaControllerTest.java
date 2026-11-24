package br.com.brumi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.brumi.config.SecurityConfig;
import br.com.brumi.dto.CategoriaRequest;
import br.com.brumi.dto.CategoriaResponse;
import br.com.brumi.exception.ConflitoException;
import br.com.brumi.exception.GlobalExceptionHandler;
import br.com.brumi.exception.RecursoNaoEncontradoException;
import br.com.brumi.service.CategoriaService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Testa a camada HTTP: rotas, JSON, status e formato de erro. O service e um mock (@MockitoBean),
 * entao nao ha banco. Aqui verificamos o "contrato" da API.
 */
@WebMvcTest(CategoriaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class CategoriaControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	CategoriaService service;

	@Test
	void listarDevolve200ComJson() throws Exception {
		when(service.listar()).thenReturn(List.of(new CategoriaResponse(1L, "Cestas"), new CategoriaResponse(2L, "Kits")));

		mockMvc.perform(get("/api/categorias"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].id").value(1))
			.andExpect(jsonPath("$[0].nome").value("Cestas"));
	}

	@Test
	void obterInexistenteDevolve404NoFormatoPadrao() throws Exception {
		when(service.obter(99L)).thenThrow(new RecursoNaoEncontradoException("Categoria não encontrada"));

		mockMvc.perform(get("/api/categorias/99"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.erro").value("Not Found"))
			.andExpect(jsonPath("$.mensagem").value("Categoria não encontrada"))
			.andExpect(jsonPath("$.campos").doesNotExist());
	}

	@Test
	void idNaoNumericoDevolve400() throws Exception {
		mockMvc.perform(get("/api/categorias/abc"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.mensagem").value("Requisição inválida"));
	}

	@Test
	void criarDevolve201ComLocation() throws Exception {
		when(service.criar(any(CategoriaRequest.class))).thenReturn(new CategoriaResponse(7L, "Kits"));

		mockMvc.perform(post("/api/categorias").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Kits\"}"))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "http://localhost/api/categorias/7"))
			.andExpect(jsonPath("$.id").value(7))
			.andExpect(jsonPath("$.nome").value("Kits"));
	}

	@Test
	void criarComNomeEmBrancoDevolve400ComListaDeCampos() throws Exception {
		mockMvc.perform(post("/api/categorias").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"   \"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.mensagem").value("Dados inválidos"))
			.andExpect(jsonPath("$.campos[0].campo").value("nome"))
			.andExpect(jsonPath("$.campos[0].mensagem").value("O nome da categoria é obrigatório"));
	}

	@Test
	void criarComJsonMalformadoDevolve400() throws Exception {
		mockMvc.perform(post("/api/categorias").contentType(MediaType.APPLICATION_JSON).content("{nome:"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.mensagem").value("Requisição inválida"));
	}

	@Test
	void criarDuplicadaDevolve409() throws Exception {
		when(service.criar(any(CategoriaRequest.class))).thenThrow(new ConflitoException("Já existe uma categoria com o nome 'Kits'"));

		mockMvc.perform(post("/api/categorias").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Kits\"}"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.mensagem").value("Já existe uma categoria com o nome 'Kits'"));
	}

	@Test
	void erroInesperadoDevolve500SemVazarDetalhes() throws Exception {
		when(service.listar()).thenThrow(new IllegalStateException("senha do banco = 123456"));

		mockMvc.perform(get("/api/categorias"))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.status").value(500))
			.andExpect(jsonPath("$.mensagem").value("Ocorreu um erro inesperado. Tente novamente mais tarde."));
	}

	@Test
	void metodoNaoPermitidoDevolve405NoFormatoPadrao() throws Exception {
		// Nao ha DELETE em /api/categorias/{id}. A rota existe, mas nao com este metodo.
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/categorias/1")
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("x")))
			.andExpect(status().isMethodNotAllowed())
			.andExpect(jsonPath("$.status").value(405));
	}
}
