package br.com.brumi.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.brumi.ComoAdminConfig;
import br.com.brumi.TestcontainersConfiguration;
import br.com.brumi.domain.Categoria;
import br.com.brumi.domain.Produto;
import br.com.brumi.repository.CategoriaRepository;
import br.com.brumi.repository.ProdutoRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
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
 * Integracao completa (HTTP -> service -> Postgres real) dos filtros, paginacao e CRUD de produtos.
 * Os dados sao criados no @BeforeEach e desfeitos ao fim de cada teste (@Transactional).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, ComoAdminConfig.class})
@ActiveProfiles("test")
@Transactional
class ProdutoApiIntegracaoTest {

	@Autowired
	MockMvc mvc;
	@Autowired
	CategoriaRepository categorias;
	@Autowired
	ProdutoRepository produtos;

	Long cestas, kits, presentes;

	@BeforeEach
	void criarDados() {
		Categoria c1 = categorias.save(new Categoria("Cestas"));
		Categoria c2 = categorias.save(new Categoria("Kits"));
		Categoria c3 = categorias.save(new Categoria("Presentes"));
		cestas = c1.getId();
		kits = c2.getId();
		presentes = c3.getId();

		criar("Cesta Especial", "199.90", true, c1, c3);
		criar("Cesta Premium", "249.90", true, c1);
		criar("Kit Perfumado", "159.90", true, c2);
		criar("Caneca Personalizada", "39.90", true, c3);
		criar("Kit Antigo", "89.90", false, c2);
		criar("Brinde 100% algodão", "10.00", true);
		criar("Caixa_Surpresa", "20.00", true);
		criar("CaixaXSurpresa", "21.00", true);
		criar("Sem Preço", null, true);
	}

	private void criar(String nome, String preco, boolean ativo, Categoria... cats) {
		Produto p = new Produto(nome, null, preco == null ? null : new BigDecimal(preco));
		p.setAtivo(ativo);
		for (Categoria c : cats) {
			p.getCategorias().add(c);
		}
		produtos.save(p);
	}

	// ---------- filtros ----------

	@Test
	void semFiltrosListaSoOsAtivos() throws Exception {
		mvc.perform(get("/api/produtos"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElementos").value(8))
			.andExpect(jsonPath("$.conteudo[*].nome").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("Kit Antigo"))));
	}

	@Test
	void ativoFalseListaSoOsInativos() throws Exception {
		mvc.perform(get("/api/produtos?ativo=false"))
			.andExpect(jsonPath("$.conteudo[*].nome", contains("Kit Antigo")));
	}

	@Test
	void nomeIgnoraMaiusculasEBuscaPorTrecho() throws Exception {
		mvc.perform(get("/api/produtos?nome=CESTA"))
			.andExpect(jsonPath("$.conteudo[*].nome", containsInAnyOrder("Cesta Especial", "Cesta Premium")));
		mvc.perform(get("/api/produtos?nome=sonal"))
			.andExpect(jsonPath("$.conteudo[*].nome", contains("Caneca Personalizada")));
	}

	@Test
	void curingasDoLikeSaoTratadosComoTextoLiteral() throws Exception {
		// "%" sozinho nao pode casar com tudo: so o produto que tem o caractere "%" no nome.
		// .param() em vez de colocar "?nome=%" na URL: o MockMvc codificaria o "%" uma segunda vez.
		mvc.perform(get("/api/produtos").param("nome", "%"))
			.andExpect(jsonPath("$.conteudo[*].nome", contains("Brinde 100% algodão")));
		// "_" nao pode casar com qualquer caractere: "caixa_s" nao encontra "CaixaXSurpresa".
		mvc.perform(get("/api/produtos?nome=caixa_s"))
			.andExpect(jsonPath("$.conteudo[*].nome", contains("Caixa_Surpresa")));
	}

	@Test
	void categoriaUnicaEMultiplasSemDuplicarProdutos() throws Exception {
		mvc.perform(get("/api/produtos?categorias=" + cestas))
			.andExpect(jsonPath("$.conteudo[*].nome", containsInAnyOrder("Cesta Especial", "Cesta Premium")));

		// "Cesta Especial" esta em Cestas E em Presentes: precisa aparecer UMA vez, e o total precisa bater.
		mvc.perform(get("/api/produtos?categorias=" + cestas + "," + presentes))
			.andExpect(jsonPath("$.totalElementos").value(3))
			.andExpect(jsonPath("$.conteudo[*].nome", containsInAnyOrder("Cesta Especial", "Cesta Premium", "Caneca Personalizada")));
		mvc.perform(get("/api/produtos?categorias=" + cestas + "&categorias=" + presentes))
			.andExpect(jsonPath("$.totalElementos").value(3));
	}

	@Test
	void categoriaInexistenteNaoDaErroEDevolveVazio() throws Exception {
		mvc.perform(get("/api/produtos?categorias=999999"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.conteudo", empty()));
	}

	@Test
	void faixaDePrecoEInclusivaEIgnoraProdutosSemPreco() throws Exception {
		mvc.perform(get("/api/produtos?precoMin=159.90&precoMax=199.90"))
			.andExpect(jsonPath("$.conteudo[*].nome", containsInAnyOrder("Kit Perfumado", "Cesta Especial")));
		mvc.perform(get("/api/produtos?precoMax=21"))
			.andExpect(jsonPath("$.conteudo[*].nome", containsInAnyOrder("Brinde 100% algodão", "Caixa_Surpresa", "CaixaXSurpresa")));
		mvc.perform(get("/api/produtos?precoMin=200"))
			.andExpect(jsonPath("$.conteudo[*].nome", contains("Cesta Premium")));
	}

	@Test
	void filtrosCombinadosAplicamTodosAoMesmoTempo() throws Exception {
		mvc.perform(get("/api/produtos?nome=cesta&precoMax=200&categorias=" + presentes))
			.andExpect(jsonPath("$.conteudo[*].nome", contains("Cesta Especial")));
		mvc.perform(get("/api/produtos?nome=kit&ativo=false&precoMin=80"))
			.andExpect(jsonPath("$.conteudo[*].nome", contains("Kit Antigo")));
	}

	// ---------- paginacao e ordenacao ----------

	@Test
	void paginacaoDevolveMetadadosEPaginasSemRepeticao() throws Exception {
		mvc.perform(get("/api/produtos?size=3&page=0&sort=preco"))
			.andExpect(jsonPath("$.conteudo.length()").value(3))
			.andExpect(jsonPath("$.pagina").value(0))
			.andExpect(jsonPath("$.tamanho").value(3))
			.andExpect(jsonPath("$.totalElementos").value(8))
			.andExpect(jsonPath("$.totalPaginas").value(3))
			.andExpect(jsonPath("$.conteudo[*].nome", contains("Brinde 100% algodão", "Caixa_Surpresa", "CaixaXSurpresa")));
		mvc.perform(get("/api/produtos?size=3&page=1&sort=preco"))
			.andExpect(jsonPath("$.conteudo[*].nome", contains("Caneca Personalizada", "Kit Perfumado", "Cesta Especial")));
		mvc.perform(get("/api/produtos?size=3&page=2&sort=preco"))
			.andExpect(jsonPath("$.conteudo[*].nome", contains("Cesta Premium", "Sem Preço"))); // NULL por ultimo no ASC
		mvc.perform(get("/api/produtos?size=3&page=9&sort=preco"))
			.andExpect(jsonPath("$.conteudo", empty()));
	}

	@Test
	void ordenacaoDescendenteFunciona() throws Exception {
		mvc.perform(get("/api/produtos?sort=preco,desc&size=100&precoMin=0"))
			.andExpect(jsonPath("$.conteudo[0].nome").value("Cesta Premium"));
	}

	@Test
	void ordenacaoPorCampoNaoPermitidoDevolve400() throws Exception {
		mvc.perform(get("/api/produtos?sort=descricao"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.mensagem").value("Ordenação inválida: 'descricao'. Campos permitidos: id, nome, preco"));
		mvc.perform(get("/api/produtos?sort=naoexiste,desc"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void tamanhoDePaginaMaximoE100() throws Exception {
		mvc.perform(get("/api/produtos?size=5000"))
			.andExpect(jsonPath("$.tamanho").value(100));
	}

	// ---------- CRUD ----------

	@Test
	void fluxoCompletoCriarLerAtualizarEApagar() throws Exception {
		String criado = mvc.perform(post("/api/produtos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"  Almofada  \",\"descricao\":\"Macia\",\"preco\":59.9,\"categoriasIds\":[" + kits + "," + kits + "]}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.nome").value("Almofada"))
			.andExpect(jsonPath("$.ativo").value(true))
			.andExpect(jsonPath("$.categorias[0].nome").value("Kits"))
			.andExpect(jsonPath("$.imagens", empty()))
			.andReturn().getResponse().getContentAsString();
		// Le o "id" do produto (raiz do JSON). Uma regex pegaria o ultimo "id", que e o da categoria.
		Number idCriado = com.jayway.jsonpath.JsonPath.read(criado, "$.id");
		String id = String.valueOf(idCriado);

		mvc.perform(get("/api/produtos/" + id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.preco").value(59.9));

		// Atualiza so o preco: o resto permanece.
		mvc.perform(put("/api/produtos/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"preco\":64.5}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.preco").value(64.5))
			.andExpect(jsonPath("$.nome").value("Almofada"))
			.andExpect(jsonPath("$.descricao").value("Macia"))
			.andExpect(jsonPath("$.categorias[0].nome").value("Kits"));

		// Troca as categorias.
		mvc.perform(put("/api/produtos/" + id).contentType(MediaType.APPLICATION_JSON)
				.content("{\"categoriasIds\":[" + cestas + "," + presentes + "]}"))
			.andExpect(jsonPath("$.categorias[*].nome", contains("Cestas", "Presentes")));

		// Desativa: some da listagem padrao, mas continua acessivel por id.
		mvc.perform(put("/api/produtos/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"ativo\":false}"))
			.andExpect(jsonPath("$.ativo").value(false));
		mvc.perform(get("/api/produtos?nome=almofada")).andExpect(jsonPath("$.conteudo", empty()));
		mvc.perform(get("/api/produtos/" + id)).andExpect(status().isOk());

		mvc.perform(delete("/api/produtos/" + id)).andExpect(status().isNoContent());
		mvc.perform(get("/api/produtos/" + id))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.mensagem").value("Produto não encontrado"));
		mvc.perform(delete("/api/produtos/" + id)).andExpect(status().isNotFound());
	}

	@Test
	void categoriaInexistenteNaCriacaoOuAtualizacaoDevolve400() throws Exception {
		mvc.perform(post("/api/produtos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"X\",\"categoriasIds\":[" + kits + ",424242]}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.mensagem").value("Categorias não encontradas: [424242]"));

		Long id = produtos.findAll().get(0).getId();
		mvc.perform(put("/api/produtos/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"categoriasIds\":[777]}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.mensagem").value("Categorias não encontradas: [777]"));
	}

	@Test
	void atualizarOuApagarInexistenteDevolve404() throws Exception {
		mvc.perform(put("/api/produtos/999999").contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isNotFound());
		mvc.perform(delete("/api/produtos/999999")).andExpect(status().isNotFound());
	}
}
