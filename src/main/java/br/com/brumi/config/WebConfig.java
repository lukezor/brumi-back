package br.com.brumi.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registra as propriedades de upload (brumi.uploads.*) e de armazenamento (brumi.armazenamento.*).
 *
 * As fotos eram servidas aqui por um resource handler ligado a pasta do disco. Agora quem serve e o
 * ImagemController, que le pela interface ArmazenamentoImagens (disco em dev, Cloud Storage em producao).
 */
@Configuration
@EnableConfigurationProperties({UploadProperties.class, ArmazenamentoProperties.class})
public class WebConfig {
}
