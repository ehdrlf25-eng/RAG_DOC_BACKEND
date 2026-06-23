CREATE TABLE parent_sections (
    id              BIGSERIAL PRIMARY KEY,
    document_id     BIGINT NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    section_index   INT NOT NULL,
    title           VARCHAR(500) NOT NULL,
    full_content    TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_parent_sections_document_section_index UNIQUE (document_id, section_index)
);

CREATE INDEX idx_parent_sections_document_id ON parent_sections (document_id);

ALTER TABLE document_chunks
    ADD COLUMN parent_section_id BIGINT REFERENCES parent_sections (id) ON DELETE CASCADE;

ALTER TABLE document_chunks
    DROP CONSTRAINT uq_document_chunks_document_chunk_index;

ALTER TABLE document_chunks
    ADD CONSTRAINT uq_document_chunks_parent_chunk_index UNIQUE (parent_section_id, chunk_index);

CREATE INDEX idx_document_chunks_parent_section_id ON document_chunks (parent_section_id);
