package br.com.brumi.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.brumi.ComoAdminConfig;
import br.com.brumi.Imagens;
import br.com.brumi.TestcontainersConfiguration;
import br.com.brumi.domain.Produto;
import br.com.brumi.repository.ProdutoRepository;
import br.com.brumi.service.ArmazenamentoLocal;
import br.com.brumi.service.ProdutoImagemService;
import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Upload e remocao de imagens de ponta a ponta: HTTP, banco e DISCO.
 *
 * SEM @Transactional na classe, de proposito: precisamos que o commit aconteca de verdade, porque e depois
 * dele que os arquivos sao apagados. Por isso a limpeza e manual, no @AfterEach.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, ComoAdminConfig.class})
@ActiveProfiles("test")
class ProdutoImagemApiTest {

	@Autowired
	MockMvc mvc;
	@Autowired
	ProdutoRepository produtos;
	@Autowired
	ArmazenamentoLocal armazenamento;
	@Autowired
	ProdutoImagemService imagemService;
	@Autowired
	TransactionTemplate transacao;

	Path pasta;
	List<Long> criados = new ArrayList<>();
	Long produtoId;

	@BeforeEach
	void preparar() throws IOException {
		pasta = armazenamento.raiz();
		limparPasta();
		produtoId = novoProduto();
	}

	@AfterEach
	void limpar() throws IOException {
		criados.forEach(id -> produtos.findById(id).ifPresent(produtos::delete));
		limparPasta();
	}

	private Long novoProduto() {
		Long id = produtos.save(new Produto("Produto com fotos", null, new BigDecimal("10.00"))).getId();
		criados.add(id);
		return id;
	}

	private void limparPasta() throws IOException {
		try (Stream<Path> arquivos = Files.list(pasta)) {
			for (Path p : arquivos.toList()) {
				Files.deleteIfExists(p);
			}
		}
	}

	private long arquivosNaPasta() throws IOException {
		try (Stream<Path> arquivos = Files.list(pasta)) {
			return arquivos.count();
		}
	}

	private ResultActions enviar(Long id, MockMultipartFile... arquivos) throws Exception {
		var req = multipart("/api/produtos/" + id + "/imagens");
		for (MockMultipartFile a : arquivos) {
			req.file(a);
		}
		return mvc.perform(req);
	}

	private static String nomeDoArquivo(String url) {
		return url.substring("/uploads/".length());
	}

	// ---------- upload ----------

	@Test
	void enviaImagensGravaNoDiscoEDevolveAsUrls() throws Exception {
		String corpo = enviar(produtoId, Imagens.png("capa.png"), Imagens.jpeg("detalhe.jpg"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(produtoId))
			.andExpect(jsonPath("$.imagens", hasSize(2)))
			.andExpect(jsonPath("$.imagens[0]").value(org.hamcrest.Matchers.matchesPattern("/uploads/[0-9a-f\\-]{36}\\.png")))
			.andExpect(jsonPath("$.imagens[1]").value(org.hamcrest.Matchers.endsWith(".jpg")))
			.andReturn().getResponse().getContentAsString();

		List<String> urls = JsonPath.read(corpo, "$.imagens");
		assertThat(pasta.resolve(nomeDoArquivo(urls.get(0)))).exists();
		assertThat(pasta.resolve(nomeDoArquivo(urls.get(1)))).exists();

		// O arquivo fica acessivel publicamente, com o tipo certo e cache longo.
		mvc.perform(get(urls.get(0)).with(anonymous()))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Type", "image/png"))
			.andExpect(header().string("Cache-Control", "max-age=31536000, public, immutable"))
			.andExpect(content().bytes(Imagens.png("x").getBytes()));

		// E o GET do produto mostra as imagens, em ordem.
		mvc.perform(get("/api/produtos/" + produtoId)).andExpect(jsonPath("$.imagens", contains(urls.toArray())));
	}

	@Test
	void imagemQueNaoExisteDevolve404() throws Exception {
		mvc.perform(get("/uploads/nao-existe.png").with(anonymous()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void tentativaDeSairDaPastaPelaUrlNaoFunciona() throws Exception {
		mvc.perform(get("/uploads/%2e%2e/%2e%2e/pom.xml").with(anonymous()))
			.andExpect(status().is4xxClientError());
		mvc.perform(get("/uploads/../application.yaml").with(anonymous()))
			.andExpect(status().is4xxClientError());
	}

	@Test
	void maisDeCincoImagensERecusadoSemGravarNada() throws Exception {
		enviar(produtoId, Imagens.png("1.png"), Imagens.png("2.png"), Imagens.png("3.png"), Imagens.png("4.png"))
			.andExpect(status().isOk());
		long antes = arquivosNaPasta();

		enviar(produtoId, Imagens.png("5.png"), Imagens.png("6.png"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.mensagem").value("O produto pode ter no máximo 5 imagens. Ele já tem 4 e foram enviadas 2."));

		assertThat(arquivosNaPasta()).isEqualTo(antes);
		enviar(produtoId, Imagens.webp("5.webp")).andExpect(status().isOk()).andExpect(jsonPath("$.imagens", hasSize(5)));
	}

	@Test
	void umArquivoInvalidoRecusaOLoteInteiro() throws Exception {
		enviar(produtoId, Imagens.png("ok.png"), Imagens.gif("animacao.gif"), Imagens.jpeg("ok.jpg"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.mensagem").value("A imagem 'animacao.gif' não é PNG, JPEG nem WEBP"));

		assertThat(arquivosNaPasta()).isZero();
		mvc.perform(get("/api/produtos/" + produtoId)).andExpect(jsonPath("$.imagens", hasSize(0)));
	}

	@Test
	void conteudoQueMenteOContentTypeERecusado() throws Exception {
		enviar(produtoId, Imagens.arquivo("foto.png", "image/png", "<html>nao sou imagem</html>".getBytes()))
			.andExpect(status().isBadRequest());
	}

	@Test
	void arquivoVazioOuGrandeDemaisERecusado() throws Exception {
		enviar(produtoId, Imagens.arquivo("vazio.png", "image/png", new byte[0]))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.mensagem").value("A imagem 'vazio.png' está vazia"));

		byte[] seisMega = Imagens.bytes(Imagens.ASSINATURA_PNG, 6 * 1024 * 1024);
		enviar(produtoId, Imagens.arquivo("grande.png", "image/png", seisMega))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.mensagem").value("A imagem 'grande.png' excede o tamanho máximo de 5MB"));
	}

	@Test
	void campoComNomeErradoDevolve400ExplicativoE415SemMultipart() throws Exception {
		mvc.perform(multipart("/api/produtos/" + produtoId + "/imagens")
				.file(new MockMultipartFile("imagem", "a.png", "image/png", Imagens.png("a").getBytes())))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.mensagem").value("Envie os arquivos no campo 'imagens' (multipart/form-data)"));

		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/produtos/" + produtoId + "/imagens")
				.contentType("application/json").content("{}"))
			.andExpect(status().isUnsupportedMediaType());
	}

	@Test
	void produtoInexistenteDevolve404() throws Exception {
		enviar(999999L, Imagens.png("a.png")).andExpect(status().isNotFound());
		assertThat(arquivosNaPasta()).isZero();
	}

	@Test
	void semLoginDevolve401() throws Exception {
		mvc.perform(multipart("/api/produtos/" + produtoId + "/imagens").file(Imagens.png("a.png")).with(anonymous()))
			.andExpect(status().isUnauthorized());
		mvc.perform(delete("/api/produtos/" + produtoId + "/imagens/0").with(anonymous()))
			.andExpect(status().isUnauthorized());
		assertThat(arquivosNaPasta()).isZero();
	}

	// ---------- consistencia entre banco e disco ----------

	@Test
	void seOBancoDesfizerAOperacaoOsArquivosGravadosSaoApagados() throws Exception {
		transacao.executeWithoutResult(status -> {
			imagemService.adicionar(produtoId, List.of(Imagens.png("a.png"), Imagens.jpeg("b.jpg")));
			try {
				assertThat(arquivosNaPasta()).isEqualTo(2); // gravados no disco...
			}
			catch (IOException e) {
				throw new IllegalStateException(e);
			}
			status.setRollbackOnly();                          // ...mas o banco desfaz
		});

		assertThat(arquivosNaPasta()).isZero();
		mvc.perform(get("/api/produtos/" + produtoId)).andExpect(jsonPath("$.imagens", hasSize(0)));
	}

	// ---------- remocao ----------

	@Test
	void removerPeloIndiceRenumeraEApagaSoAqueleArquivo() throws Exception {
		String corpo = enviar(produtoId, Imagens.png("a.png"), Imagens.png("b.png"), Imagens.png("c.png"))
			.andReturn().getResponse().getContentAsString();
		List<String> urls = JsonPath.read(corpo, "$.imagens");

		mvc.perform(delete("/api/produtos/" + produtoId + "/imagens/1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.imagens", contains(urls.get(0), urls.get(2))));

		assertThat(pasta.resolve(nomeDoArquivo(urls.get(1)))).doesNotExist();
		assertThat(pasta.resolve(nomeDoArquivo(urls.get(0)))).exists();
		assertThat(pasta.resolve(nomeDoArquivo(urls.get(2)))).exists();

		// A antiga posicao 2 agora e a 1: remover a 1 de novo apaga a "c".
		mvc.perform(delete("/api/produtos/" + produtoId + "/imagens/1"))
			.andExpect(jsonPath("$.imagens", contains(urls.get(0))));
	}

	@Test
	void indiceInvalidoOuProdutoSemImagens() throws Exception {
		mvc.perform(delete("/api/produtos/" + produtoId + "/imagens/0"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.mensagem").value("O produto não tem imagens"));

		enviar(produtoId, Imagens.png("a.png"), Imagens.png("b.png"));
		mvc.perform(delete("/api/produtos/" + produtoId + "/imagens/2"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.mensagem").value("Índice de imagem inválido: use um valor entre 0 e 1"));
		mvc.perform(delete("/api/produtos/" + produtoId + "/imagens/-1")).andExpect(status().isBadRequest());
		assertThat(arquivosNaPasta()).isEqualTo(2);
	}

	@Test
	void apagarOProdutoApagaOsArquivosDele() throws Exception {
		enviar(produtoId, Imagens.png("a.png"), Imagens.jpeg("b.jpg"));
		Long outro = novoProduto();
		enviar(outro, Imagens.png("do-outro.png"));
		assertThat(arquivosNaPasta()).isEqualTo(3);

		mvc.perform(delete("/api/produtos/" + produtoId)).andExpect(status().isNoContent());

		assertThat(arquivosNaPasta()).isEqualTo(1); // so sobrou a imagem do outro produto
	}
}
