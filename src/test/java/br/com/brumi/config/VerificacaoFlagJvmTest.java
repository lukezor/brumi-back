package br.com.brumi.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class VerificacaoFlagJvmTest {

	@Test
	void aceitaComAFlagERecusaSemElaOuReligada() {
		assertThatCode(() -> VerificacaoFlagJvm.verificar(List.of("-Xmx256m", "-XX:-UseBASE64Intrinsics")))
			.doesNotThrowAnyException();

		assertThatThrownBy(() -> VerificacaoFlagJvm.verificar(List.of("-Xmx256m")))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("-XX:-UseBASE64Intrinsics");

		// Desligada e depois religada: a ultima vence, entao esta vulneravel.
		assertThatThrownBy(() -> VerificacaoFlagJvm.verificar(List.of("-XX:-UseBASE64Intrinsics", "-XX:+UseBASE64Intrinsics")))
			.isInstanceOf(IllegalStateException.class);
	}
}
