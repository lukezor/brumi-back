package br.com.brumi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.brumi.Imagens;
import br.com.brumi.config.UploadProperties;
import br.com.brumi.exception.RequisicaoInvalidaException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.unit.DataSize;

/**
 * Teste unitario do armazenamento em disco. @TempDir: o JUnit cria uma pasta temporaria nova para cada teste
 * e a apaga no fim. Nada toca a pasta uploads/ de verdade.
 */
class ArmazenamentoImagensTest {

	@TempDir
	Path temp;

	Path pastaUploads;
	ArmazenamentoImagens armazenamento;

	@BeforeEach
	void criar() throws Exception {
		pastaUploads = temp.resolve("uploads");
		armazenamento = new ArmazenamentoImagens(new UploadProperties(pastaUploads.toString(), DataSize.ofKilobytes(1)));
	}

	@Test
	void criaAPastaSeNaoExistir() {
		assertThat(pastaUploads).isDirectory();
	}

	@Test
	void reconheceOsTresFormatosPeloConteudo() {
		assertThat(armazenamento.validar(Imagens.png("a.png")).tipo()).isEqualTo(TipoImagem.PNG);
		assertThat(armazenamento.validar(Imagens.jpeg("a.jpg")).tipo()).isEqualTo(TipoImagem.JPEG);
		assertThat(armazenamento.validar(Imagens.webp("a.webp")).tipo()).isEqualTo(TipoImagem.WEBP);
	}

	@Test
	void ignoraContentTypeEExtensaoDeclarados() {
		// Diz ser PNG, mas o conteudo e texto: recusado.
		var falso = Imagens.arquivo("foto.png", "image/png", "<script>alert(1)</script>".getBytes());
		assertThatThrownBy(() -> armazenamento.validar(falso))
			.isInstanceOf(RequisicaoInvalidaException.class)
			.hasMessage("A imagem 'foto.png' não é PNG, JPEG nem WEBP");

		// Diz ser texto e se chama .txt, mas o conteudo e PNG: aceito como PNG.
		var disfarcado = Imagens.arquivo("nota.txt", "text/plain", Imagens.bytes(Imagens.ASSINATURA_PNG, 50));
		assertThat(armazenamento.validar(disfarcado).tipo()).isEqualTo(TipoImagem.PNG);
	}

	@Test
	void recusaGifVazioEGrandeDemais() {
		assertThatThrownBy(() -> armazenamento.validar(Imagens.gif("a.gif"))).isInstanceOf(RequisicaoInvalidaException.class);
		assertThatThrownBy(() -> armazenamento.validar(Imagens.arquivo("vazio.png", "image/png", new byte[0])))
			.hasMessage("A imagem 'vazio.png' está vazia");
		var grande = Imagens.arquivo("g.png", "image/png", Imagens.bytes(Imagens.ASSINATURA_PNG, 1025)); // limite: 1 KB
		assertThatThrownBy(() -> armazenamento.validar(grande)).hasMessageContaining("excede o tamanho máximo");
	}

	@Test
	void salvaComNomeAleatorioEExtensaoDoTipoReal() throws Exception {
		var disfarcado = Imagens.arquivo("../../../malicioso.txt", "text/plain", Imagens.bytes(Imagens.ASSINATURA_JPEG, 50));

		String nome = armazenamento.salvar(armazenamento.validar(disfarcado));

		assertThat(nome).matches("[0-9a-f\\-]{36}\\.jpg");               // UUID + extensao do tipo detectado
		assertThat(pastaUploads.resolve(nome)).exists();                   // gravado DENTRO da pasta
		assertThat(Files.readAllBytes(pastaUploads.resolve(nome))).isEqualTo(disfarcado.getBytes());
		assertThat(temp.resolve("malicioso.txt")).doesNotExist();
	}

	@Test
	void removeApagaOArquivoESemErroSeJaNaoExistir() {
		String nome = armazenamento.salvar(armazenamento.validar(Imagens.png("a.png")));

		armazenamento.remover(nome);
		assertThat(pastaUploads.resolve(nome)).doesNotExist();
		armazenamento.remover(nome); // de novo: nao lanca
	}

	@Test
	void removerNuncaApagaForaDaPasta() throws Exception {
		Path vitima = Files.writeString(temp.resolve("importante.txt"), "nao me apague");

		armazenamento.remover("../importante.txt");
		armazenamento.remover(vitima.toString()); // caminho absoluto

		assertThat(vitima).exists();
		assertThatThrownBy(() -> armazenamento.resolver("../importante.txt")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> armazenamento.resolver("sub/pasta.png")).isInstanceOf(IllegalArgumentException.class);
	}
}
