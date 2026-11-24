package br.com.brumi.controller;

import br.com.brumi.dto.ErroResponse;
import br.com.brumi.dto.LoginRequest;
import br.com.brumi.dto.LoginResponse;
import br.com.brumi.service.AutenticacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Login do administrador")
public class AuthController {

	private final AutenticacaoService service;

	public AuthController(AutenticacaoService service) {
		this.service = service;
	}

	@Operation(summary = "Faz login e devolve um token JWT",
		description = "Use o `accessToken` no botão **Authorize** do Swagger, ou no cabeçalho "
			+ "`Authorization: Bearer <token>`. Validade em segundos no campo `expiresIn`.")
	@ApiResponse(responseCode = "200", description = "Login aceito")
	@ApiResponse(responseCode = "400", description = "Usuário ou senha não informados", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
	@ApiResponse(responseCode = "401", description = "Usuário ou senha inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		return service.login(request);
	}
}
