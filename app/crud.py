from sqlalchemy.orm import Session
from . import models, schemas
from typing import Optional, List
from fastapi import UploadFile, HTTPException
import os
from uuid import uuid4

# --- UPLOAD ---
async def salvar_imagem(imagem: UploadFile):
    if not os.path.exists("uploads"):
        os.makedirs("uploads")
    if imagem.content_type not in ["image/webp", "image/png", "image/jpeg"]:
        raise HTTPException(status_code=400, detail="Formato de imagem inválido")
    if imagem.filename is not None:
        nome_arquivo = f"{uuid4()}.{imagem.filename.split('.')[-1]}"
    else:
        nome_arquivo = f"{uuid4()}.{imagem.content_type.split('/')[-1]}"
    caminho_arquivo = os.path.join("uploads", nome_arquivo)
    with open(caminho_arquivo, "wb") as f:
        content = await imagem.read()
        f.write(content)
    return caminho_arquivo

def deletar_imagem(caminho: str):
    if os.path.exists(caminho):
        os.remove(caminho)

# --- CATEGORIA ---

def criar_categoria(db: Session, categoria: schemas.CategoriaCreate):
    db_categoria = models.Categoria(nome=categoria.nome)
    db.add(db_categoria)
    db.commit()
    db.refresh(db_categoria)
    return db_categoria

def listar_categorias(db: Session, skip: int = 0, limit: int = 100):
    return db.query(models.Categoria).offset(skip).limit(limit).all()

def obter_categoria(db: Session, categoria_id: int):
    return db.query(models.Categoria).filter(models.Categoria.id == categoria_id).first()

# --- PRODUTO ---

def criar_produto(db: Session, produto: schemas.ProdutoCreate):
    db_produto = models.Produto(
        nome=produto.nome,
        descricao=produto.descricao,
        preco=produto.preco,
        ativo = True
    )
    # Associar categorias
    if produto.categorias_ids:
        categorias = db.query(models.Categoria).filter(models.Categoria.id.in_(produto.categorias_ids)).all()
        db_produto.categorias = categorias

    db.add(db_produto)
    db.commit()
    db.refresh(db_produto)
    return db_produto

def listar_produtos(
    db: Session,
    skip: int = 0,
    limit: int = 100,
    nome: Optional[str] = None,
    categorias: Optional[List[int]] = None,
    preco_min: Optional[float] = None,
    preco_max: Optional[float] = None,
    ativo: Optional[bool] = True
):
    query = db.query(models.Produto)

    if ativo is not None:
        query = query.filter(models.Produto.ativo == ativo)

    if nome:
        query = query.filter(models.Produto.nome.ilike(f"%{nome}%"))

    if categorias:
        query = query.join(models.Produto.categorias).filter(models.Categoria.id.in_(categorias))

    if preco_min is not None:
        query = query.filter(models.Produto.preco >= preco_min)

    if preco_max is not None:
        query = query.filter(models.Produto.preco <= preco_max)

    return query.offset(skip).limit(limit).all()


def obter_produto(db: Session, produto_id: int):
    return db.query(models.Produto).filter(models.Produto.id == produto_id).first()

def atualizar_produto(db: Session, produto_id: int, produto: schemas.ProdutoUpdate):
    db_produto = obter_produto(db, produto_id)
    if not db_produto:
        return None
    if produto.nome is not None:
        db_produto.nome = produto.nome
    if produto.descricao is not None:
        db_produto.descricao = produto.descricao
    if produto.preco is not None:
        db_produto.preco = produto.preco
    if produto.ativo is not None:
        db_produto.ativo = produto.ativo

    if produto.categorias_ids is not None:
        categorias = db.query(models.Categoria).filter(models.Categoria.id.in_(produto.categorias_ids)).all()
        db_produto.categorias = categorias
    else:
        db_produto.categorias = []

    db.commit()
    db.refresh(db_produto)
    return db_produto

def deletar_produto(db: Session, produto_id: int):
    db_produto = obter_produto(db, produto_id)
    if not db_produto:
        return None
    db.delete(db_produto)
    db.commit()
    return db_produto
