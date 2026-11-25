package br.com.brumi.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.brumi.Imagens;
import com.google.cloud.NoCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * ArmazenamentoGcs contra o fake-gcs-server, um emulador do Cloud Storage num container. Testa a nossa
 * classe sem conta no Google e sem custo. Nao sobe o Spring: so a classe e o cliente do Storage.
 */
@Testcontainers
class ArmazenamentoGcsTest {

	private static final String BUCKET = "brumi-teste";

	@Container
	static final GenericContainer<?> GCS = new GenericContainer<>("fsouza/fake-gcs-server:1.56")
		.withCommand("-scheme", "http", "-port", "4443")
		.withExposedPorts(4443);

	static Storage storage;

	@BeforeAll
	static void criarBucket() {
		storage = StorageOptions.newBuilder()
			.setHost("http://" + GCS.getHost() + ":" + GCS.getMappedPort(4443))
			.setProjectId("brumi-local")
			.setCredentials(NoCredentials.getInstance())
			.build()
			.getService();
		storage.create(BucketInfo.of(BUCKET));
	}

	@Test
	void salvaLeERemoveNoBucket() throws Exception {
		var armazenamento = new ArmazenamentoGcs(storage, BUCKET);
		var png = Imagens.png("capa.png");

		String nome = armazenamento.salvar(new ImagemValidada(png, TipoImagem.PNG));

		assertThat(nome).matches("[0-9a-f\\-]{36}\\.png");
		var blob = storage.get(BlobId.of(BUCKET, nome));
		assertThat(blob.getContentType()).isEqualTo("image/png");

		var lida = armazenamento.ler(nome).orElseThrow();
		assertThat(lida.conteudo()).isEqualTo(png.getBytes());
		assertThat(lida.tipo()).isEqualTo(TipoImagem.PNG);

		armazenamento.remover(nome);
		assertThat(armazenamento.ler(nome)).isEmpty();
		armazenamento.remover(nome); // de novo: nao lanca
	}

	@Test
	void nomeForaDoPadraoNemChegaAoBucket() {
		var armazenamento = new ArmazenamentoGcs(storage, BUCKET);
		assertThat(armazenamento.ler("../outro-bucket/segredo.txt")).isEmpty();
	}
}
