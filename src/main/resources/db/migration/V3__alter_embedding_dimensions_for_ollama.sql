-- Ollama nomic-embed-text uses 768 dimensions (default embedding model).
UPDATE document_chunks SET embedding = NULL WHERE embedding IS NOT NULL;

DROP INDEX IF EXISTS idx_document_chunks_embedding;

ALTER TABLE document_chunks
    ALTER COLUMN embedding TYPE vector(768);

CREATE INDEX idx_document_chunks_embedding ON document_chunks USING hnsw (embedding vector_cosine_ops);
