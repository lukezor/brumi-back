# Brumi Presentes - API

API REST do catálogo de presentes customizados da Brumi: produtos, categorias, imagens e o login do
administrador.

| Tecnologia | Versão |
|---|---|
| Java | 25 |
| Spring Boot | 4.2 (Web MVC, Data JPA, Security, OAuth2 Resource Server, Validation, Actuator) |
| PostgreSQL | 17 (Docker) |
| Flyway | migrations do banco |
| springdoc-openapi | 3.2 (Swagger UI) |
| Testes | JUnit 5, Mockito, Testcontainers |
| Build | Maven, via Maven Wrapper (`mvnw`) |

## Pré-requisitos

| Ferramenta | Como conferir |
|---|---|
| JDK 25 | `java -version` |
| Docker Desktop (no Windows, com WSL2) | `docker --version` e `docker compose version` |
| Git | `git --version` |

O Maven **não** precisa estar instalado: o `mvnw` baixa a versão certa na primeira execução.
O Docker precisa estar aberto para rodar a aplicação e os testes.

## Como rodar (desenvolvimento)

### 1. Configurar as variáveis
```powershell
copy .env.example .env
```
Edite o `.env`:

| Variável | Obrigatória | Para quê |
|---|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Sim | Banco criado pelo Docker Compose |
| `JWT_SECRET` | **Sim** | Chave que assina os tokens. Mínimo de 32 caracteres, aleatória. **Sem ela a aplicação não sobe** |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | Na primeira subida | Administrador criado se ainda não existir. Senha com 8 caracteres ou mais |
| `JWT_EXPIRACAO_MINUTOS` | Não (padrão 1440) | Validade do token |
| `CORS_ORIGENS` | Não (padrão `http://localhost:4200`) | Sites que podem chamar a API pelo navegador, separados por vírgula |
| `UPLOADS_DIR` | Não (padrão `uploads`) | Pasta das imagens |
| `SWAGGER_ENABLED` | Não (padrão `true`) | Liga ou desliga o Swagger |

Para gerar uma `JWT_SECRET` no PowerShell:
```powershell
$b = New-Object byte[] 32; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); -join ($b | ForEach-Object { $_.ToString('x2') })
```
Use só letras e números nos valores: o Docker Compose interpreta `$` de forma especial.

O `.env` **nunca** vai para o git (está no `.gitignore`).

### 2. Subir
```powershell
.\mvnw spring-boot:run
```
Na primeira vez, o Maven baixa as dependências (alguns minutos). Ao subir, a aplicação:
1. Sobe o PostgreSQL pelo `compose.yaml` (suporte a Docker Compose do Spring Boot).
2. Aplica as migrations do Flyway e, só no perfil `dev`, o seed com 3 categorias e 5 produtos de exemplo.
3. Cria o administrador do `.env`, se ainda não existir.
4. Atende em `http://localhost:8080`.

### 3. Conferir
- `http://localhost:8080/api/ping` → `{"status":"ok", ...}`
- `http://localhost:8080/actuator/health` → `{"status":"UP"}`
- `http://localhost:8080/swagger-ui.html` → documentação interativa

## Documentação interativa (Swagger)
Abra `http://localhost:8080/swagger-ui.html`.

1. Em **Autenticação → POST /api/auth/login**, clique em **Try it out**, informe usuário e senha do `.env`
   e execute.
2. Copie o valor de `accessToken` da resposta.
3. Clique em **Authorize** (cadeado no topo), cole o token e confirme.
4. As operações com cadeado (criar, alterar, apagar, enviar imagens) passam a funcionar.

A especificação OpenAPI em JSON fica em `http://localhost:8080/v3/api-docs`.

## Endpoints

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| POST | `/api/auth/login` | Público | Login. Devolve `accessToken`, `tokenType`, `expiresIn` |
| GET | `/api/categorias` | Público | Lista categorias |
| GET | `/api/categorias/{id}` | Público | Uma categoria |
| POST | `/api/categorias` | Admin | Cria categoria |
| GET | `/api/produtos` | Público | Lista paginada com filtros `nome`, `categorias`, `precoMin`, `precoMax`, `ativo`, `page`, `size`, `sort` |
| GET | `/api/produtos/{id}` | Público | Um produto |
| POST | `/api/produtos` | Admin | Cria produto |
| PUT | `/api/produtos/{id}` | Admin | Altera produto (só os campos enviados) |
| DELETE | `/api/produtos/{id}` | Admin | Apaga produto e imagens |
| POST | `/api/produtos/{id}/imagens` | Admin | Envia imagens (multipart, campo `imagens`). PNG, JPEG ou WEBP, até 5 MB, no máximo 5 |
| DELETE | `/api/produtos/{id}/imagens/{indice}` | Admin | Remove imagem pela posição (começa em 0) |
| GET | `/uploads/{arquivo}` | Público | Arquivo da imagem |
| GET | `/api/ping`, `/actuator/health` | Público | Diagnóstico |

"Admin" = cabeçalho `Authorization: Bearer <token>` com um token de administrador.

Todos os erros seguem o formato:
```json
{ "status": 400, "erro": "Bad Request", "mensagem": "Dados inválidos",
  "campos": [ { "campo": "nome", "mensagem": "O nome do produto é obrigatório" } ] }
```
`campos` só aparece em erros de validação.

## Testes
```powershell
.\mvnw test      # roda os testes
.\mvnw verify    # testes + empacota o .jar em target/
```
Os testes de integração usam o **Testcontainers**: cada execução cria um PostgreSQL descartável no Docker.
Eles não usam o `.env` nem o banco de desenvolvimento (valores falsos em `src/test/resources/application-test.yaml`).

## Segurança: flag Java4Shell (CVE-2026-114938)
Enquanto o JDK não for atualizado para uma versão corrigida (ex.: 25.0.5.1), toda JVM que roda esta
aplicação precisa da flag **`-XX:-UseBASE64Intrinsics`**.

| Como a aplicação roda | Onde a flag está |
|---|---|
| `mvnw spring-boot:run` | Já configurada no `pom.xml` (`jvm.hardening.args`) |
| `mvnw test` / `verify` | Já configurada no `pom.xml` (surefire) |
| `java -jar` (produção) | **Precisa ir no comando** (veja abaixo) |
| Docker / variável de ambiente | `JAVA_TOOL_OPTIONS=-XX:-UseBASE64Intrinsics` |

## Rodar como em produção (`java -jar`)
O jar **não** inclui o suporte a Docker Compose, nem o seed de exemplo, nem o `.env`. Tudo vem de
variáveis de ambiente:

```powershell
.\mvnw verify
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/brumi"
$env:SPRING_DATASOURCE_USERNAME = "..."
$env:SPRING_DATASOURCE_PASSWORD = "..."
$env:JWT_SECRET = "..."
$env:ADMIN_USERNAME = "admin"
$env:ADMIN_PASSWORD = "..."
$env:SWAGGER_ENABLED = "false"     # recomendado em produção
java -XX:-UseBASE64Intrinsics -jar target\brumi-api-0.0.1-SNAPSHOT.jar
```
Com um perfil diferente de `dev`, o seed de exemplo não é carregado: o banco recebe só as migrations.

## Estrutura
```
src/main/java/br/com/brumi/
├── config/       segurança, JWT, CORS, OpenAPI, upload, propriedades
├── controller/   rotas HTTP
├── service/      regras de negócio, login, armazenamento de imagens
├── repository/   acesso ao banco (Spring Data JPA)
├── domain/       entidades (tabelas)
├── dto/          dados de entrada e saída da API
└── exception/    exceções de negócio e tratamento global de erros
src/main/resources/
├── application.yaml, application-dev.yaml
├── db/migration/ migrations versionadas (V1, V2...)
└── db/seed/      dados de exemplo (só perfil dev)
src/test/          testes (unitários, de controller e de integração)
compose.yaml       PostgreSQL de desenvolvimento
_legacy/           versão antiga em FastAPI (referência durante a migração)
```

## Banco de dados
| Comando | Efeito |
|---|---|
| `docker compose up -d` | Sobe o banco (a aplicação já faz isso sozinha) |
| `docker compose down` | Desliga, **mantendo os dados** |
| `docker compose down -v` | Desliga e **apaga os dados** (o próximo `spring-boot:run` recria tudo) |

As migrations ficam em `src/main/resources/db/migration`. **Nunca edite uma migration já aplicada**: crie
uma nova (`V3__...`).

## Problemas comuns
| Sintoma | Causa provável |
|---|---|
| "Defina a variável de ambiente JWT_SECRET" | Falta `JWT_SECRET` no `.env` ou no ambiente |
| Testes ou app falham com erro de Docker | Docker Desktop fechado |
| `Port 8080 was already in use` | Outra instância rodando |
| `mvnw` não reconhecido | Rode de dentro da pasta `brumi-back`, com `.\mvnw` |
| Login sempre 401 | O admin é criado uma vez. Mudar `ADMIN_PASSWORD` depois não altera a senha. Em dev, apague a linha da tabela `usuario` e reinicie |
| `Migration checksum mismatch` | Uma migration aplicada foi editada |
| Erro de CORS no navegador | A origem do frontend não está em `CORS_ORIGENS` |
| JSON recusado (400) ao usar `curl -d` no Windows PowerShell 5.1 | O PowerShell remove aspas. Envie o corpo por arquivo (`--data-binary "@corpo.json"`) ou use o Swagger |
