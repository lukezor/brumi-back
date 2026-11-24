package br.com.brumi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo do POST /api/auth/login. (≈ LoginData do schemas.py.) */
public record LoginRequest(
	@NotBlank(message = "Informe o usuário") @Size(max = 50, message = "Usuário muito longo") String username,
	@NotBlank(message = "Informe a senha") @Size(max = 72, message = "Senha muito longa") String password) {

	/**
	 * Nunca mostrar a senha em logs ou mensagens de erro. O toString() padrao de um record imprime todos os
	 * campos, entao o sobrescrevemos.
	 */
	@Override
	public String toString() {
		return "LoginRequest[username=" + username + ", password=***]";
	}
}
