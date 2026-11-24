# Brumi Presentes - Backend API

API REST em FastAPI para catálogo de produtos com autenticação JWT.

## Como rodar

### 1. Instalar dependências
```bash
# Criar ambiente virtual
python -m venv venv

# Ativar ambiente virtual
# Windows:
venv\Scripts\activate
# Linux/Mac:
source venv/bin/activate

# Instalar dependências
pip install -r requirements.txt
```

### 2. Configurar banco de dados
Crie um banco PostgreSQL e configure o arquivo `.env`:

```env
DATABASE_URL=postgresql://usuario:senha@localhost:5432/nome_do_banco
SECRET_KEY=sua-chave-secreta
ADMIN_USERNAME=admin
ADMIN_PASSWORD=sua_senha
ACCESS_TOKEN_EXPIRE_MINUTES=1440
```

### 3. Executar
```bash
uvicorn app.main:app --reload
```

### 4. Acessar
- API: `http://localhost:8000`
- Documentação: `http://localhost:8000/docs`

## Endpoints principais

- `GET /produtos/` - Listar produtos (público)
- `POST /auth/login` - Login admin
- `POST /produtos/` - Criar produto (admin)
- `POST /produtos/{id}/imagens/` - Upload imagens (admin)