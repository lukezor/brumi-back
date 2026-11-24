package br.com.brumi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Informacoes gerais da documentacao da API (OpenAPI / Swagger).
 *
 * O springdoc LE os controllers (rotas, parametros, DTOs, validacoes) e gera a especificacao sozinho.
 * Aqui so completamos o que ele nao tem como adivinhar: titulo, descricao e COMO se autenticar.
 * (≈ FastAPI(title="Brumi Presentes API") + o botao "Authorize" do /docs.)
 */
@Configuration
public class OpenApiConfig {

	/** Nome do esquema de seguranca. Os metodos protegidos o citam em @SecurityRequirement(name = ...). */
	public static final String ESQUEMA_JWT = "bearer-jwt";

	@Bean
	OpenAPI brumiOpenApi() {
		return new OpenAPI()
			.info(new Info()
				.title("Brumi Presentes API")
				.version("1.0")
				.description("""
					API do catálogo de presentes customizados.

					**Leitura** (produtos, categorias, imagens) é pública.
					**Escrita** exige um token de administrador:
					1. Chame `POST /api/auth/login` e copie o `accessToken` da resposta.
					2. Clique em **Authorize** (cadeado), cole o token e confirme.
					3. As operações com cadeado passam a enviar `Authorization: Bearer <token>`.
					"""))
			.components(new Components().addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
				.type(SecurityScheme.Type.HTTP)
				.scheme("bearer")
				.bearerFormat("JWT")
				.description("Token obtido em POST /api/auth/login")));
	}
}
