"""Trava de custos da Brumi: desliga o faturamento do projeto no primeiro custo detectado.

Fluxo: Orcamento do Google Cloud -> mensagem no topico Pub/Sub -> esta funcao.
A mensagem traz costAmount (quanto o projeto ja gastou no mes). Se for maior que zero, a funcao
desvincula a conta de faturamento do projeto. Tudo que e pago para; o site sai do ar ate alguem
religar o faturamento no console (ver docs/deploy/task-05).

Variaveis de ambiente:
  PROJETO_ID  - id do projeto a proteger (obrigatoria).
  MODO_TESTE  - "true" (padrao): so registra no log o que faria. "false": desliga de verdade.
"""
import base64
import json
import os

import functions_framework
from google.cloud import billing_v1


@functions_framework.cloud_event
def desligar_faturamento(evento):
    dados = json.loads(base64.b64decode(evento.data["message"]["data"]).decode("utf-8"))
    custo = float(dados.get("costAmount", 0))
    projeto = os.environ["PROJETO_ID"]
    modo_teste = os.getenv("MODO_TESTE", "true").lower() != "false"

    print(f"Orcamento '{dados.get('budgetDisplayName')}': custo {custo} {dados.get('currencyCode')}")
    if custo <= 0:
        print("Sem custo. Nada a fazer.")
        return

    if modo_teste:
        print(f"MODO_TESTE: desligaria o faturamento do projeto {projeto}")
        return

    cliente = billing_v1.CloudBillingClient()
    nome = f"projects/{projeto}"
    if not cliente.get_project_billing_info(name=nome).billing_enabled:
        print("Faturamento ja estava desligado.")
        return
    # billing_account_name vazio = desvincular a conta de faturamento (metodo da documentacao do Google).
    cliente.update_project_billing_info(
        name=nome, project_billing_info=billing_v1.ProjectBillingInfo(billing_account_name="")
    )
    print(f"Faturamento DESLIGADO no projeto {projeto}.")
