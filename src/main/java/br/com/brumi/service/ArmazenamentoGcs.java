package br.com.brumi.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Guarda as fotos num bucket do Google Cloud Storage. Criado pelo GcsConfig quando brumi.armazenamento.tipo=gcs.
 *
 * Sem chave em arquivo: no Cloud Run, a biblioteca usa automaticamente a conta de servico do servico
 * (Application Default Credentials). O bucket continua privado; quem entrega a foto ao navegador e a API.
 */
public class ArmazenamentoGcs implements ArmazenamentoImagens {

	private static final Logger log = LoggerFactory.getLogger(ArmazenamentoGcs.class);

	/** Mesmo cache do ImagemController, gravado no proprio objeto (util se um dia o bucket for servido direto). */
	private static final String CACHE_LONGO = "public, max-age=31536000, immutable";

	private final Storage storage;
	private final String bucket;

	public ArmazenamentoGcs(Storage storage, String bucket) {
		if (bucket == null || bucket.isBlank()) {
			throw new IllegalStateException("brumi.armazenamento.gcs.bucket (GCS_BUCKET) é obrigatório com tipo=gcs");
		}
		this.storage = storage;
		this.bucket = bucket;
	}

	@Override
	public String salvar(ImagemValidada imagem) {
		String nome = ArmazenamentoImagens.novoNome(imagem.tipo());
		BlobInfo info = BlobInfo.newBuilder(BlobId.of(bucket, nome))
			.setContentType(imagem.tipo().mimeType())
			.setCacheControl(CACHE_LONGO)
			.build();
		try {
			// doesNotExist: falha em vez de sobrescrever, caso (quase impossivel) o UUID ja exista.
			storage.create(info, imagem.arquivo().getBytes(), Storage.BlobTargetOption.doesNotExist());
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Falha ao ler a imagem enviada", ex);
		}
		log.debug("Imagem gravada no bucket {}: {}", bucket, nome);
		return nome;
	}

	@Override
	public Optional<ImagemArmazenada> ler(String nome) {
		if (!ArmazenamentoImagens.nomeValido(nome)) {
			return Optional.empty();
		}
		var blob = storage.get(BlobId.of(bucket, nome));
		if (blob == null) {
			return Optional.empty();
		}
		return Optional.of(new ImagemArmazenada(blob.getContent(), ArmazenamentoImagens.tipoPeloNome(nome)));
	}

	@Override
	public void remover(String nome) {
		if (!ArmazenamentoImagens.nomeValido(nome)) {
			log.warn("Nome de imagem inválido, nada apagado: '{}'", nome);
			return;
		}
		try {
			storage.delete(BlobId.of(bucket, nome));
		}
		catch (StorageException ex) {
			log.warn("Não foi possível apagar a imagem '{}': {}", nome, ex.getMessage());
		}
	}
}
