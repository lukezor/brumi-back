package br.com.brumi.dto;

/**
 * DTO (Data Transfer Object) de resposta do /api/ping.
 * Um "record" é uma classe imutável com construtor, getters, equals e toString gerados pelo Java.
 * O Spring converte este objeto em JSON automaticamente: {"status":"ok","mensagem":"..."}.
 * É o papel que um modelo Pydantic tinha no FastAPI.
 */
public record PingResponse(String status, String mensagem) {
}
