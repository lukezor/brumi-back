package br.com.brumi.controller;

import br.com.brumi.dto.LoginRequest;
import br.com.brumi.dto.LoginResponse;
import br.com.brumi.service.AutenticacaoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AutenticacaoService service;

	public AuthController(AutenticacaoService service) {
		this.service = service;
	}

	/** POST /api/auth/login -> 200 com o token, ou 401 se usuario/senha estiverem errados. */
	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		return service.login(request);
	}
}
