package br.com.brumi.service;

import br.com.brumi.config.UploadProperties;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Guarda as fotos numa pasta do disco (brumi.uploads.diretorio). Ativa quando brumi.armazenamento.tipo=local
 * ou quando a propriedade nao existe (matchIfMissing). Em nuvem o disco e descartavel: la usamos o GCS.
 */
@Service
@ConditionalOnProperty(name = "brumi.armazenamento.tipo", havingValue = "local", matchIfMissing = true)
public class ArmazenamentoLocal implements ArmazenamentoImagens {

	private static final Logger log = LoggerFactory.getLogger(ArmazenamentoLocal.class);

	private final Path raiz;

	public ArmazenamentoLocal(UploadProperties props) throws IOException {
		// toAbsolutePath + normalize: transforma "uploads" em "C:/.../brumi-back/uploads", sem "." nem "..".
		this.raiz = Path.of(props.diretorio()).toAbsolutePath().normalize();
		Files.createDirectories(raiz);
	}

	public Path raiz() {
		return raiz;
	}

	@Override
	public String salvar(ImagemValidada imagem) {
		String nome = ArmazenamentoImagens.novoNome(imagem.tipo());
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

	@Override
	public Optional<ImagemArmazenada> ler(String nome) {
		if (!ArmazenamentoImagens.nomeValido(nome)) {
			return Optional.empty();
		}
		Path arquivo = resolver(nome);
		if (!Files.isRegularFile(arquivo)) {
			return Optional.empty();
		}
		try {
			return Optional.of(new ImagemArmazenada(Files.readAllBytes(arquivo), ArmazenamentoImagens.tipoPeloNome(nome)));
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Falha ao ler a imagem", ex);
		}
	}

	@Override
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
		if (!raiz.equals(caminho.getParent())) {
			throw new IllegalArgumentException("Nome de arquivo fora da pasta de uploads: " + nome);
		}
		return caminho;
	}
}
