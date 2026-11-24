package br.com.brumi.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.brumi.ComoAdminConfig;
import br.com.brumi.config.SecurityConfig;
import org.springframework.test.context.ActiveProfiles;
import br.com.brumi.dto.CategoriaResponse;
import br.com.brumi.dto.FiltroProduto;
import br.com.brumi.dto.PaginaResponse;
import br.com.brumi.dto.ProdutoRequest;
import br.com.brumi.dto.ProdutoResponse;
import br.com.brumi.dto.ProdutoUpdateRequest;
import br.com.brumi.exception.GlobalExceptionHandler;
import br.com.brumi.exception.RecursoNaoEncontradoException;
import br.com.brumi.exception.RequisicaoInvalidaException;
import br.com.brumi.service.ProdutoService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Contrato HTTP de produtos: como a URL vira parametros, formato dos JSONs, status e erros. */
@WebMvcTest(ProdutoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ComoAdminConfig.class})
@ActiveProfiles("test")
class ProdutoControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	ProdutoService service;

	private static final ProdutoResponse CANECA = new ProdutoResponse(3L, "Caneca", "Branca", new BigDecimal("39.90"), true,
		List.of(new CategoriaResponse(1L, "Cestas")), List.of("uploads/a.png"));

	@Test
	void listarSemParametrosUsaPadroes() throws Exception {
		when(service.listar(any(), any())).thenReturn(new PaginaResponse<>(List.of(CANECA), 0, 20, 1, 1));

		mockMvc.perform(get("/api/produtos"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.conteudo[0].nome").value("Caneca"))
			.andExpect(jsonPath("$.conteudo[0].preco").value(39.90))
			.andExpect(jsonPath("$.conteudo[0].categorias[0].nome").value("Cestas"))
			.andExpect(jsonPath("$.conteudo[0].imagens[0]").value("uploads/a.png"))
			.andExpect(jsonPath("$.pagina").value(0))
			.andExpect(jsonPath("$.tamanho").value(20))
			.andExpect(jsonPath("$.totalElementos").value(1))
			.andExpect(jsonPath("$.totalPaginas").value(1));

		ArgumentCaptor<FiltroProduto> filtro = ArgumentCaptor.forClass(FiltroProduto.class);
		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(service).listar(filtro.capture(), pageable.capture());
		assertThat(filtro.getValue()).isEqualTo(new FiltroProduto(null, null, null, null, true)); // ativo=true por padrao
		assertThat(pageable.getValue().getPageNumber()).isZero();
		assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
	}

	@Test
	void listarConvertesTodosOsParametrosDaUrl() throws Exception {
		when(service.listar(any(), any())).thenReturn(new PaginaResponse<>(List.of(), 1, 5, 0, 0));

		mockMvc.perform(get("/api/produtos")
				.param("nome", "caneca").param("categorias", "1,2").param("precoMin", "10").param("precoMax", "50.5")
				.param("ativo", "false").param("page", "1").param("size", "5").param("sort", "preco,desc"))
			.andExpect(status().isOk());

		ArgumentCaptor<FiltroProduto> filtro = ArgumentCaptor.forClass(FiltroProduto.class);
		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(service).listar(filtro.capture(), pageable.capture());
		assertThat(filtro.getValue()).isEqualTo(
			new FiltroProduto("caneca", List.of(1L, 2L), new BigDecimal("10"), new BigDecimal("50.5"), false));
		assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
		assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
		assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "preco"));
	}

	@Test
	void categoriasRepetidasNaUrlTambemFuncionam() throws Exception {
		when(service.listar(any(), any())).thenReturn(new PaginaResponse<>(List.of(), 0, 20, 0, 0));

		mockMvc.perform(get("/api/produtos?categorias=4&categorias=5")).andExpect(status().isOk());

		ArgumentCaptor<FiltroProduto> filtro = ArgumentCaptor.forClass(FiltroProduto.class);
		verify(service).listar(filtro.capture(), any());
		assertThat(filtro.getValue().categorias()).containsExactly(4L, 5L);
	}

	@Test
	void tamanhoDePaginaEhLimitadoA100() throws Exception {
		when(service.listar(any(), any())).thenReturn(new PaginaResponse<>(List.of(), 0, 100, 0, 0));

		mockMvc.perform(get("/api/produtos?size=100000")).andExpect(status().isOk());

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(service).listar(any(), pageable.capture());
		assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
	}

	@Test
	void parametroComTipoErradoDevolve400() throws Exception {
		mockMvc.perform(get("/api/produtos?precoMin=abc")).andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/produtos?categorias=x")).andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/produtos?ativo=talvez")).andExpect(status().isBadRequest());
	}

	@Test
	void ordenacaoInvalidaDevolve400ComMensagem() throws Exception {
		when(service.listar(any(), any())).thenThrow(new RequisicaoInvalidaException("Ordenação inválida: 'x'"));

		mockMvc.perform(get("/api/produtos?sort=x"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.mensagem").value("Ordenação inválida: 'x'"));
	}

	@Test
	void obterInexistenteDevolve404() throws Exception {
		when(service.obter(99L)).thenThrow(new RecursoNaoEncontradoException("Produto não encontrado"));

		mockMvc.perform(get("/api/produtos/99"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.mensagem").value("Produto não encontrado"));
	}

	@Test
	void criarDevolve201ComLocation() throws Exception {
		when(service.criar(any(ProdutoRequest.class))).thenReturn(CANECA);

		mockMvc.perform(post("/api/produtos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Caneca\",\"preco\":39.90,\"categoriasIds\":[1]}"))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "http://localhost/api/produtos/3"))
			.andExpect(jsonPath("$.id").value(3));
	}

	@Test
	void criarComVariosErrosListaTodosOsCampos() throws Exception {
		mockMvc.perform(post("/api/produtos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\" \",\"preco\":-5,\"descricao\":\"" + "x".repeat(1001) + "\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.mensagem").value("Dados inválidos"))
			.andExpect(jsonPath("$.campos.length()").value(4))
			.andExpect(jsonPath("$.campos[?(@.campo=='nome')]").exists())
			.andExpect(jsonPath("$.campos[?(@.campo=='preco')]").exists())
			.andExpect(jsonPath("$.campos[?(@.campo=='descricao')]").exists())
			.andExpect(jsonPath("$.campos[?(@.campo=='categoriasIds')]").exists());
	}

	@Test
	void precoComMaisDeDuasCasasDecimaisEhRecusado() throws Exception {
		mockMvc.perform(post("/api/produtos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"X\",\"preco\":10.999,\"categoriasIds\":[]}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.campos[0].campo").value("preco"));
	}

	@Test
	void atualizarParcialAceitaCorpoSoComUmCampo() throws Exception {
		when(service.atualizar(eq(3L), any(ProdutoUpdateRequest.class))).thenReturn(CANECA);

		mockMvc.perform(put("/api/produtos/3").contentType(MediaType.APPLICATION_JSON).content("{\"ativo\":false}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(3));

		ArgumentCaptor<ProdutoUpdateRequest> req = ArgumentCaptor.forClass(ProdutoUpdateRequest.class);
		verify(service).atualizar(eq(3L), req.capture());
		assertThat(req.getValue().ativo()).isFalse();
		assertThat(req.getValue().nome()).isNull();
	}

	@Test
	void atualizarComNomeSoDeEspacosDevolve400() throws Exception {
		mockMvc.perform(put("/api/produtos/3").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"   \"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.campos[0].campo").value("nome"));
	}

	@Test
	void deletarDevolve204SemCorpo() throws Exception {
		mockMvc.perform(delete("/api/produtos/3"))
			.andExpect(status().isNoContent());
		verify(service).deletar(3L);
	}
}
