CREATE INDEX IF NOT EXISTS idx_document_chunks_content_fts
    ON document_chunks
    USING GIN (to_tsvector('simple', coalesce(content, '') || ' ' || coalesce(section_title, '')));
