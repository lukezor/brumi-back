from fastapi import FastAPI, Depends, HTTPException, Query, UploadFile, Request
from fastapi.params import File
from fastapi.staticfiles import StaticFiles
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
from typing import List, Optional
import logging
from datetime import timedelta
from app import models, schemas, crud, auth
from app.database import engine, SessionLocal, Base

# Cria as tabelas no banco, se ainda não existirem
Base.metadata.create_all(bind=engine)

app = FastAPI(title="Brumi Presentes API")
app.mount("/uploads", StaticFiles(directory="uploads"), name="uploads")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Permite todas as origens
    allow_credentials=True,
    allow_methods=["*"],  # Permite todos os métodos HTTP
    allow_headers=["*"],  # Permite todos os cabeçalhos
)

# Configuração de logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Tratamento global de erros
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    logger.error(f"Erro não tratado: {str(exc)}", exc_info=True)
    return JSONResponse(
        status_code=500,
        content={
            "error": "Erro interno do servidor",
            "message": "Ocorreu um erro inesperado. Tente novamente mais tarde."
        }
    )

# Handler específico para HTTPException
@app.exception_handler(HTTPException)
async def http_exception_handler(request: Request, exc: HTTPException):
    return JSONResponse(
        status_code=exc.status_code,
        content={
            "error": exc.detail,
            "status_code": exc.status_code
        }
    )

# Handler específico para erro de token de acesso obrigatório
@app.exception_handler(403)
async def forbidden_exception_handler(request: Request, exc: HTTPException):
    return JSONResponse(
        status_code=401,
        content={
            "error": "Token de acesso obrigatório",
            "message": "É necessário fazer login para acessar este recurso.",
            "status_code": 401
        }
    )

# Dependency para pegar a sessão do banco
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# --- ROTAS DE AUTENTICAÇÃO ---

@app.post("/auth/login", response_model=schemas.Token)
def login(login_data: schemas.LoginData):
    user = auth.authenticate_user(login_data.username, login_data.password)
    if not user:
        raise HTTPException(
            status_code=401,
            detail="Credenciais inválidas",
            headers={"WWW-Authenticate": "Bearer"},
        )
    access_token_expires = timedelta(minutes=int(auth.ACCESS_TOKEN_EXPIRE_MINUTES))
    access_token = auth.create_access_token(
        data={"sub": user["username"]}, expires_delta=access_token_expires
    )
    return {"access_token": access_token, "token_type": "bearer"}

# --- ROTAS DE CATEGORIAS ---

@app.post("/categorias/", response_model=schemas.CategoriaRead)
def criar_categoria(categoria: schemas.CategoriaCreate, db: Session = Depends(get_db), current_user: dict = Depends(auth.get_current_user)):
    db_categoria = crud.criar_categoria(db, categoria)
    return db_categoria

@app.get("/categorias/", response_model=List[schemas.CategoriaRead])
def listar_categorias(skip: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    return crud.listar_categorias(db, skip=skip, limit=limit)

@app.get("/categorias/{categoria_id}", response_model=schemas.CategoriaRead)
def obter_categoria(categoria_id: int, db: Session = Depends(get_db)):
    db_categoria = crud.obter_categoria(db, categoria_id)
    if not db_categoria:
        raise HTTPException(status_code=404, detail="Categoria não encontrada")
    return db_categoria

# --- ROTAS DE PRODUTOS ---

@app.post("/produtos/", response_model=schemas.ProdutoRead)
def criar_produto(produto: schemas.ProdutoCreate, db: Session = Depends(get_db), current_user: dict = Depends(auth.get_current_user)):
    return crud.criar_produto(db, produto)

@app.get("/produtos/", response_model=List[schemas.ProdutoRead])
def listar_produtos(
    skip: int = 0,
    limit: int = 100,
    nome: Optional[str] = None,
    categorias: Optional[List[int]] = Query(None),
    preco_min: Optional[float] = None,
    preco_max: Optional[float] = None,
    ativo: Optional[bool] = True,
    db: Session = Depends(get_db)
):
    return crud.listar_produtos(
        db=db,
        skip=skip,
        limit=limit,
        nome=nome,
        categorias=categorias,
        preco_min=preco_min,
        preco_max=preco_max,
        ativo=ativo
    )

@app.get("/produtos/{produto_id}", response_model=schemas.ProdutoRead)
def obter_produto(produto_id: int, db: Session = Depends(get_db)):
    db_produto = crud.obter_produto(db, produto_id)
    if not db_produto:
        raise HTTPException(status_code=404, detail="Produto não encontrado")
    return db_produto

@app.put("/produtos/{produto_id}", response_model=schemas.ProdutoRead)
def atualizar_produto(produto_id: int, produto: schemas.ProdutoUpdate, db: Session = Depends(get_db), current_user: dict = Depends(auth.get_current_user)):
    db_produto = crud.atualizar_produto(db, produto_id, produto)
    if not db_produto:
        raise HTTPException(status_code=404, detail="Produto não encontrado para atualizar")
    return db_produto

@app.delete("/produtos/{produto_id}", response_model=schemas.ProdutoRead)
def deletar_produto(produto_id: int, db: Session = Depends(get_db), current_user: dict = Depends(auth.get_current_user)):
    db_produto = crud.deletar_produto(db, produto_id)
    if not db_produto:
        raise HTTPException(status_code=404, detail="Produto não encontrado para deletar")
    return db_produto

#--- ROTAS DE UPLOAD ---
@app.post("/produtos/{produto_id}/imagens/", response_model=schemas.ProdutoRead)
async def adicionar_imagens_produto(produto_id: int, imagens: List[UploadFile] = File(...), db: Session = Depends(get_db), current_user: dict = Depends(auth.get_current_user)):
    return await crud.adicionar_imagens_produto(db, produto_id, imagens)

@app.delete("/produtos/{produto_id}/imagens/{index}", response_model=schemas.ProdutoRead)
def remover_imagem_produto(produto_id: int, index: int, db: Session = Depends(get_db), current_user: dict = Depends(auth.get_current_user)):
    return crud.remover_imagem_produto(db, produto_id, index)