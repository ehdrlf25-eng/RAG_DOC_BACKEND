-- OpenAI text-embedding-3-small default: 1536 dimensions.
-- Existing 768-dim vectors must be cleared before altering the column type.
UPDATE document_chunks SET embedding = NULL WHERE embedding IS NOT NULL;

DROP INDEX IF EXISTS idx_document_chunks_embedding;

ALTER TABLE document_chunks
    ALTER COLUMN embedding TYPE vector(1536);

CREATE INDEX idx_document_chunks_embedding ON document_chunks USING hnsw (embedding vector_cosine_ops);

-- Re-embedding required: exclude from search until embeddings are regenerated.
UPDATE documents
SET status = 'PROCESSING',
    updated_at = NOW()
WHERE status = 'READY';
