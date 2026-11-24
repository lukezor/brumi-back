package br.com.brumi.controller;

import br.com.brumi.dto.PingResponse;
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
public class PingController {

	@GetMapping("/ping")
	public PingResponse ping() {
		return new PingResponse("ok", "Brumi API no ar");
	}
}
