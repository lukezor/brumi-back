package br.com.brumi.service;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Agenda acoes para DEPOIS que a transacao do banco terminar.
 *
 * Por que isso existe: arquivo no disco e linha no banco NAO estao na mesma transacao. O banco pode desfazer
 * (rollback); o disco nao. Entao:
 *  - ao GRAVAR uma imagem: se o banco desfizer, apagamos o arquivo gravado (aoDesfazer);
 *  - ao REMOVER uma imagem: so apagamos o arquivo depois que o banco CONFIRMAR (aposCommit).
 * Assim nunca sobra no banco uma imagem cujo arquivo sumiu.
 */
final class PosTransacao {

	private PosTransacao() {
	}

	static void aposCommit(Runnable acao) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			acao.run(); // sem transacao ativa (ex.: teste unitario): executa na hora
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				acao.run();
			}
		});
	}

	static void aoDesfazer(Runnable acao) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				if (status == STATUS_ROLLED_BACK) {
					acao.run();
				}
			}
		});
	}
}
