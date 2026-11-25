package br.com.brumi.controller;

import br.com.brumi.dto.PingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @RestController: esta classe recebe requisições HTTP e devolve JSON.
 * @RequestMapping("/api"): prefixo de todas as rotas da classe (como um APIRouter(prefix="/api") no FastAPI).
 * @GetMapping("/ping"): este método atende GET /api/ping (como @app.get("/ping")).
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Diagnóstico")
public class PingController {

	@Operation(summary = "Confere se a API está no ar")
	@GetMapping("/ping")
	public PingResponse ping() {
		return new PingResponse("ok", "Brumi API no ar");
	}
}
