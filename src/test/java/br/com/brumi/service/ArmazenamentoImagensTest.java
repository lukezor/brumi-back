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
 * Teste unitario da validacao (ValidadorImagens) e do armazenamento em disco (ArmazenamentoLocal).
 * @TempDir: o JUnit cria uma pasta temporaria nova para cada teste e a apaga no fim.
 */
class ArmazenamentoImagensTest {

	@TempDir
	Path temp;

	Path pastaUploads;
	ArmazenamentoLocal armazenamento;
	ValidadorImagens validador;

	@BeforeEach
	void criar() throws Exception {
		pastaUploads = temp.resolve("uploads");
		var props = new UploadProperties(pastaUploads.toString(), DataSize.ofKilobytes(1));
		armazenamento = new ArmazenamentoLocal(props);
		validador = new ValidadorImagens(props);
	}

	@Test
	void criaAPastaSeNaoExistir() {
		assertThat(pastaUploads).isDirectory();
	}

	@Test
	void reconheceOsTresFormatosPeloConteudo() {
		assertThat(validador.validar(Imagens.png("a.png")).tipo()).isEqualTo(TipoImagem.PNG);
		assertThat(validador.validar(Imagens.jpeg("a.jpg")).tipo()).isEqualTo(TipoImagem.JPEG);
		assertThat(validador.validar(Imagens.webp("a.webp")).tipo()).isEqualTo(TipoImagem.WEBP);
	}

	@Test
	void ignoraContentTypeEExtensaoDeclarados() {
		// Diz ser PNG, mas o conteudo e texto: recusado.
		var falso = Imagens.arquivo("foto.png", "image/png", "<script>alert(1)</script>".getBytes());
		assertThatThrownBy(() -> validador.validar(falso))
			.isInstanceOf(RequisicaoInvalidaException.class)
			.hasMessage("A imagem 'foto.png' não é PNG, JPEG nem WEBP");

		// Diz ser texto e se chama .txt, mas o conteudo e PNG: aceito como PNG.
		var disfarcado = Imagens.arquivo("nota.txt", "text/plain", Imagens.bytes(Imagens.ASSINATURA_PNG, 50));
		assertThat(validador.validar(disfarcado).tipo()).isEqualTo(TipoImagem.PNG);
	}

	@Test
	void recusaGifVazioEGrandeDemais() {
		assertThatThrownBy(() -> validador.validar(Imagens.gif("a.gif"))).isInstanceOf(RequisicaoInvalidaException.class);
		assertThatThrownBy(() -> validador.validar(Imagens.arquivo("vazio.png", "image/png", new byte[0])))
			.hasMessage("A imagem 'vazio.png' está vazia");
		var grande = Imagens.arquivo("g.png", "image/png", Imagens.bytes(Imagens.ASSINATURA_PNG, 1025)); // limite: 1 KB
		assertThatThrownBy(() -> validador.validar(grande)).hasMessageContaining("excede o tamanho máximo");
	}

	@Test
	void salvaComNomeAleatorioEExtensaoDoTipoRealELeDeVolta() throws Exception {
		var disfarcado = Imagens.arquivo("../../../malicioso.txt", "text/plain", Imagens.bytes(Imagens.ASSINATURA_JPEG, 50));

		String nome = armazenamento.salvar(validador.validar(disfarcado));

		assertThat(nome).matches("[0-9a-f\\-]{36}\\.jpg");               // UUID + extensao do tipo detectado
		assertThat(pastaUploads.resolve(nome)).exists();                   // gravado DENTRO da pasta
		assertThat(Files.readAllBytes(pastaUploads.resolve(nome))).isEqualTo(disfarcado.getBytes());
		assertThat(temp.resolve("malicioso.txt")).doesNotExist();

		var lida = armazenamento.ler(nome).orElseThrow();
		assertThat(lida.tipo()).isEqualTo(TipoImagem.JPEG);
		assertThat(lida.conteudo()).isEqualTo(disfarcado.getBytes());
	}

	@Test
	void removeApagaOArquivoESemErroSeJaNaoExistir() {
		String nome = armazenamento.salvar(validador.validar(Imagens.png("a.png")));

		armazenamento.remover(nome);
		assertThat(pastaUploads.resolve(nome)).doesNotExist();
		assertThat(armazenamento.ler(nome)).isEmpty();
		armazenamento.remover(nome); // de novo: nao lanca
	}

	@Test
	void nuncaLeNemApagaForaDaPasta() throws Exception {
		Path vitima = Files.writeString(temp.resolve("importante.txt"), "nao me apague");

		armazenamento.remover("../importante.txt");
		armazenamento.remover(vitima.toString()); // caminho absoluto

		assertThat(vitima).exists();
		assertThat(armazenamento.ler("../importante.txt")).isEmpty();
		assertThatThrownBy(() -> armazenamento.resolver("../importante.txt")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> armazenamento.resolver("sub/pasta.png")).isInstanceOf(IllegalArgumentException.class);
	}
}
