package br.com.brumi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Onde as fotos ficam (brumi.armazenamento.*).
 *
 * tipo: "local" (pasta no disco, padrao) ou "gcs" (Google Cloud Storage).
 * urlPublica: vazio = as fotos saem pela propria API em /uploads/<nome>. Preenchido (ex.:
 *   "https://fotos.brumi.com.br/") = a API devolve esse endereco + nome, e o navegador busca direto la.
 *   Fica pronto para quando houver um dominio proprio.
 * gcs.bucket: nome do bucket. gcs.host: so para o emulador local (ex.: http://localhost:4443); vazio = Google.
 */
@ConfigurationProperties(prefix = "brumi.armazenamento")
public record ArmazenamentoProperties(
	@DefaultValue("local") String tipo,
	@DefaultValue("") String urlPublica,
	@DefaultValue Gcs gcs) {

	public record Gcs(@DefaultValue("") String bucket, @DefaultValue("") String host) {
	}
}
