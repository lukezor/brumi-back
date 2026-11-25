package br.com.brumi.config;

import br.com.brumi.service.ArmazenamentoGcs;
import com.google.cloud.NoCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Liga o Cloud Storage quando brumi.armazenamento.tipo=gcs. Com tipo=local, nada daqui e criado.
 *
 * gcs.host vazio: Google de verdade, com a credencial do ambiente (no Cloud Run, a conta de servico; na sua
 * maquina, "gcloud auth application-default login"). gcs.host preenchido: emulador local, sem credencial.
 */
@Configuration
@ConditionalOnProperty(name = "brumi.armazenamento.tipo", havingValue = "gcs")
public class GcsConfig {

	@Bean
	Storage storage(ArmazenamentoProperties props) {
		String host = props.gcs().host();
		if (host == null || host.isBlank()) {
			return StorageOptions.getDefaultInstance().getService();
		}
		return StorageOptions.newBuilder()
			.setHost(host)
			.setProjectId("brumi-local")
			.setCredentials(NoCredentials.getInstance())
			.build()
			.getService();
	}

	@Bean
	ArmazenamentoGcs armazenamentoGcs(Storage storage, ArmazenamentoProperties props) {
		return new ArmazenamentoGcs(storage, props.gcs().bucket());
	}
}
