# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Architecture

This is a FastAPI-based REST API for "Brumi Presentes" (gift store), built with SQLAlchemy ORM and following a layered architecture:

- **app/main.py**: FastAPI application with route definitions for categories and products
- **app/models.py**: SQLAlchemy ORM models (Categoria, Produto) with many-to-many relationship
- **app/schemas.py**: Pydantic schemas for request/response validation
- **app/crud.py**: Database operations layer with CRUD functions
- **app/database.py**: Database configuration using environment variables

### Key Architecture Points

- Uses dependency injection pattern with `get_db()` for database sessions
- Many-to-many relationship between Products and Categories via `produto_categoria` association table
- Products have filtering capabilities (name, categories, price range, active status)
- Database URL configured via `DATABASE_URL` environment variable with python-dotenv

## Development Commands

The project uses a Python virtual environment in `venv/` directory. To work with this codebase:

```bash
# Activate virtual environment
source venv/bin/activate  # Linux/Mac
# or
venv\Scripts\activate     # Windows

# Run the application
uvicorn app.main:app --reload

# Install dependencies (if requirements.txt exists)
pip install -r requirements.txt
```

## Database Configuration

The application uses PostgreSQL and requires a `DATABASE_URL` environment variable:
```
DATABASE_URL=postgresql://lojinha_user:senha123@localhost:5432/lojinha_db
```

Database schema is managed manually - developer drops tables in PgAdmin and recreates them via `Base.metadata.create_all(bind=engine)` in main.py.

## API Endpoints

### Categories
- POST `/categorias/` - Create category
- GET `/categorias/` - List categories
- GET `/categorias/{id}` - Get category by ID

### Products  
- POST `/produtos/` - Create product
- GET `/produtos/` - List products (with filtering)
- GET `/produtos/{id}` - Get product by ID
- PUT `/produtos/{id}` - Update product
- DELETE `/produtos/{id}` - Delete product

## Current Development Status - Image Upload System

**Goal:** Implement image upload functionality for products to enable a frontend products page.

**Progress Made:**
1. ✅ **Database Model**: Added `imagens = Column(ARRAY(String), default=[])` to Produto model (PostgreSQL array of image URLs)
2. ✅ **Schema**: Updated `ProdutoRead` to include `imagens: List[str] = []` field
3. ✅ **Upload Function**: Implemented `async def salvar_imagem(imagem: UploadFile)` in crud.py with:
   - Directory creation (`uploads/` folder)
   - File type validation (jpeg, png, webp)
   - UUID-based naming for unique filenames
   - Async file reading and saving

**Next Steps:**
1. **Multiple Images Function**: Implement `salvar_multiplas_imagens(imagens: List[UploadFile]) -> List[str]`
2. **CRUD Function**: Create `criar_produto_com_upload()` that handles both product creation and image processing
3. **API Endpoint**: Add `POST /produtos/upload` endpoint using Form() data instead of Pydantic schema
4. **Integration**: Test the complete flow with multipart/form-data

**Technical Decisions Made:**
- Using single endpoint approach for better UX (user uploads everything at once)
- Processing images in crud.py to maintain separation of concerns
- PostgreSQL ARRAY type for storing image URLs
- 2MB file size limit for images
- Bottom-up implementation approach

**Current File States:**
- models.py: Updated with imagens field
- schemas.py: Updated ProdutoRead schema  
- crud.py: Contains salvar_imagem() and deletar_imagem() functions
- main.py: Unchanged, waiting for new upload endpoint