package br.com.brumi.config;

import java.lang.management.ManagementFactory;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Trava do Java4Shell (CVE-2026-114938): com brumi.seguranca.exigir-flag-jvm=true (perfil prod), a aplicacao
 * NAO SOBE se a JVM nao tiver sido iniciada com -XX:-UseBASE64Intrinsics. Assim um deploy feito por engano
 * sem a flag (outro Dockerfile, outro comando) falha na hora, em vez de ficar vulneravel em silencio.
 *
 * Como funciona: getInputArguments() devolve as opcoes passadas a JVM (as "-X..." do comando java).
 * Como a verificacao lanca excecao, o Spring aborta a inicializacao e o Cloud Run nao troca para a versao nova.
 */
@Component
@ConditionalOnProperty(name = "brumi.seguranca.exigir-flag-jvm", havingValue = "true")
public class VerificacaoFlagJvm implements BeanFactoryPostProcessor {

	public static final String FLAG = "-XX:-UseBASE64Intrinsics";

	private static final Logger log = LoggerFactory.getLogger(VerificacaoFlagJvm.class);

	/**
	 * BeanFactoryPostProcessor roda ANTES de o Spring criar os beans comuns (banco, Flyway, web). Assim a
	 * verificacao acontece primeiro, e o erro mostrado e o da flag, nao um erro de conexao qualquer.
	 */
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		verificar(ManagementFactory.getRuntimeMXBean().getInputArguments());
		log.info("Java4Shell: flag {} ativa", FLAG);
	}

	/** Separado do construtor para o teste passar a lista que quiser. */
	static void verificar(List<String> argumentosJvm) {
		// A ultima ocorrencia vence: "-XX:-Use... -XX:+Use..." deixa o recurso LIGADO.
		int desligada = argumentosJvm.lastIndexOf(FLAG);
		int ligada = argumentosJvm.lastIndexOf("-XX:+UseBASE64Intrinsics");
		if (desligada < 0 || ligada > desligada) {
			throw new IllegalStateException("A JVM precisa ser iniciada com " + FLAG
				+ " (contorno da CVE-2026-114938). Ex.: java " + FLAG + " -jar brumi-api.jar");
		}
	}
}
