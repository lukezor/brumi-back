package br.com.brumi.config;

import br.com.brumi.dto.ProdutoResponse;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serve os arquivos da pasta de uploads em /uploads/**. (≈ app.mount("/uploads", StaticFiles(...)) do main.py.)
 *
 * GET /uploads/3f2a....png -> le o arquivo da pasta e devolve com o Content-Type certo (image/png).
 * O proprio Spring bloqueia tentativas de sair da pasta (ex.: /uploads/../application.yaml).
 */
@Configuration
@EnableConfigurationProperties(UploadProperties.class)
public class WebConfig implements WebMvcConfigurer {

	private final UploadProperties uploads;

	public WebConfig(UploadProperties uploads) {
		this.uploads = uploads;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		// toUri() gera "file:///C:/.../uploads/", o formato que o Spring espera para pastas do disco.
		String pasta = Path.of(uploads.diretorio()).toAbsolutePath().normalize().toUri().toString();
		registry.addResourceHandler(ProdutoResponse.PREFIXO_URL_IMAGEM + "**")
			.addResourceLocations(pasta)
			// Cada arquivo tem nome unico (UUID) e nunca muda: o navegador pode guardar em cache por 1 ano.
			.setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic());
	}
}
