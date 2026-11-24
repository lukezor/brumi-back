from pydantic import BaseModel
from typing import Optional, List

# ----------- Categoria -----------

class CategoriaBase(BaseModel):
    nome: str

class CategoriaCreate(CategoriaBase):
    pass

class CategoriaRead(CategoriaBase):
    id: int

    class Config:
        from_attributes = True


# ----------- Produto -----------

class ProdutoBase(BaseModel):
    nome: str
    descricao: Optional[str] = None
    preco: Optional[float] = None

class ProdutoCreate(ProdutoBase):
    categorias_ids: List[int]  # IDs das categorias associadas

class ProdutoUpdate(BaseModel):
    nome: Optional[str] = None
    descricao: Optional[str] = None
    preco: Optional[float] = None
    ativo: Optional[bool] = None
    categorias_ids: Optional[List[int]] = None

class ProdutoRead(ProdutoBase):
    id: int
    categorias: List[CategoriaRead] = []  # Para saída com join
    ativo: bool
    imagens: List[str] = []

    class Config:
        from_attributes = True

# ----------- Autenticação ----------

class LoginData(BaseModel):
    username: str
    password: str

class Token(BaseModel):
    access_token: str
    token_type: str
