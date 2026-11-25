package br.com.brumi.service;

import br.com.brumi.config.UploadProperties;
import br.com.brumi.exception.RequisicaoInvalidaException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Grava e apaga os arquivos de imagem no disco. Nao sabe nada de produto nem de banco.
 * (≈ salvar_imagem e deletar_imagem do crud.py.)
 */
@Service
public class ArmazenamentoImagens {

	private static final Logger log = LoggerFactory.getLogger(ArmazenamentoImagens.class);

	/** Um arquivo que ja passou por todas as validacoes e esta pronto para ser gravado. */
	public record ImagemValidada(MultipartFile arquivo, TipoImagem tipo) {
	}

	private final Path raiz;
	private final long tamanhoMaximo;

	public ArmazenamentoImagens(UploadProperties props) throws IOException {
		// toAbsolutePath + normalize: transforma "uploads" em "C:/.../brumi-back/uploads", sem "." nem "..".
		this.raiz = Path.of(props.diretorio()).toAbsolutePath().normalize();
		this.tamanhoMaximo = props.tamanhoMaximoPorArquivo().toBytes();
		Files.createDirectories(raiz);
	}

	public Path raiz() {
		return raiz;
	}

	/**
	 * Confere o arquivo SEM gravar nada: nao vazio, dentro do tamanho e com assinatura de PNG, JPEG ou WEBP.
	 * O Content-Type e o nome enviados pelo cliente sao ignorados: so o conteudo decide.
	 */
	public ImagemValidada validar(MultipartFile arquivo) {
		String nome = nomeParaMensagem(arquivo);
		if (arquivo.isEmpty()) {
			throw new RequisicaoInvalidaException("A imagem '" + nome + "' está vazia");
		}
		if (arquivo.getSize() > tamanhoMaximo) {
			throw new RequisicaoInvalidaException("A imagem '" + nome + "' excede o tamanho máximo de "
				+ (tamanhoMaximo / (1024 * 1024)) + "MB");
		}
		byte[] inicio;
		try (InputStream in = arquivo.getInputStream()) {
			inicio = in.readNBytes(TipoImagem.BYTES_DA_ASSINATURA);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Falha ao ler o arquivo enviado", ex);
		}
		TipoImagem tipo = TipoImagem.detectar(inicio).orElseThrow(() ->
			new RequisicaoInvalidaException("A imagem '" + nome + "' não é PNG, JPEG nem WEBP"));
		return new ImagemValidada(arquivo, tipo);
	}

	/**
	 * Grava o arquivo com um nome NOVO e aleatorio (UUID) e devolve esse nome. O nome original e descartado:
	 * ele poderia conter "../../" (path traversal), caracteres invalidos ou coincidir com outro arquivo.
	 */
	public String salvar(ImagemValidada imagem) {
		String nome = UUID.randomUUID() + "." + imagem.tipo().extensao();
		Path destino = resolver(nome);
		try (InputStream in = imagem.arquivo().getInputStream()) {
			Files.copy(in, destino);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Falha ao gravar a imagem", ex);
		}
		log.debug("Imagem gravada: {}", destino);
		return nome;
	}

	/**
	 * Apaga o arquivo, se existir. Nunca lanca excecao: e chamado DEPOIS de o banco confirmar a operacao, e
	 * uma falha aqui (arquivo travado, por exemplo) nao pode desfazer o que ja foi confirmado. So registra no log.
	 */
	public void remover(String nome) {
		try {
			Files.deleteIfExists(resolver(nome));
		}
		catch (IOException | IllegalArgumentException ex) {
			log.warn("Não foi possível apagar a imagem '{}': {}", nome, ex.getMessage());
		}
	}

	/**
	 * Monta o caminho completo DENTRO da pasta de uploads. Defesa contra path traversal: se o nome tentar sair
	 * da pasta (ex.: "../../application.yaml"), o caminho normalizado nao comeca com a raiz e e recusado.
	 */
	Path resolver(String nome) {
		Path caminho = raiz.resolve(nome).normalize();
		if (!caminho.getParent().equals(raiz)) {
			throw new IllegalArgumentException("Nome de arquivo fora da pasta de uploads: " + nome);
		}
		return caminho;
	}

	/** Nome original so para mensagens de erro, limitado para nao devolver textos enormes. */
	private static String nomeParaMensagem(MultipartFile arquivo) {
		String nome = arquivo.getOriginalFilename() == null ? "sem nome" : arquivo.getOriginalFilename();
		return nome.length() > 80 ? nome.substring(0, 80) + "..." : nome;
	}
}
