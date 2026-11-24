package br.com.brumi.dto;

/**
 * Resposta do login. Os nomes seguem a convencao do OAuth2 (padrao de mercado para tokens):
 * accessToken = o JWT; tokenType = "Bearer" (como enviar); expiresIn = validade em segundos.
 */
public record LoginResponse(String accessToken, String tokenType, long expiresIn) {
}
