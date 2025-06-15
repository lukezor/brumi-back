from sqlalchemy import Column, Integer, String, Float, Table, ForeignKey, Boolean, ARRAY
from sqlalchemy.orm import relationship
from .database import Base

# Tabela associativa entre produtos e categorias
produto_categoria = Table(
    "produto_categoria",
    Base.metadata,
    Column("produto_id", Integer, ForeignKey("produtos.id"), primary_key=True),
    Column("categoria_id", Integer, ForeignKey("categorias.id"), primary_key=True)
)

class Categoria(Base):
    __tablename__ = "categorias"

    id = Column(Integer, primary_key=True, index=True)
    nome = Column(String, unique=True, nullable=False)

    produtos = relationship(
        "Produto",
        secondary=produto_categoria,
        back_populates="categorias"
    )

class Produto(Base):
    __tablename__ = "produtos"

    id = Column(Integer, primary_key=True, index=True)
    nome = Column(String, nullable=False)
    descricao = Column(String, nullable=True)
    preco = Column(Float, nullable=True)
    ativo = Column(Boolean, default=True)
    
    imagens = Column(ARRAY(String), default=[])

    categorias = relationship(
        "Categoria",
        secondary=produto_categoria,
        back_populates="produtos"
    )
