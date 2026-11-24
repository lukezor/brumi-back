package br.com.brumi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/**
 * Configuracao do armazenamento de imagens (brumi.uploads.*).
 *
 * diretorio: pasta onde os arquivos sao gravados (relativa a pasta de onde a aplicacao roda).
 * tamanhoMaximoPorArquivo: DataSize entende "5MB", "500KB" etc.
 */
@ConfigurationProperties(prefix = "brumi.uploads")
public record UploadProperties(
	@DefaultValue("uploads") String diretorio,
	@DefaultValue("5MB") DataSize tamanhoMaximoPorArquivo) {
}
