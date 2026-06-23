package com.ragdoc.platform.rag.parentchild;

/**
 * Parent-Child chunking 파이프라인용 Child 모델.
 * Child가 실제 embedding 및 벡터 검색 대상이다.
 */
public record ChildChunk(
        Long childId,
        Long parentId,
        Long documentId,
        String content,
        int chunkIndex
) {

    public static ChildChunk of(String content, int chunkIndex) {
        return new ChildChunk(null, null, null, content, chunkIndex);
    }

    public ChildChunk withParentId(Long parentId) {
        return new ChildChunk(childId, parentId, documentId, content, chunkIndex);
    }

    public ChildChunk withDocumentId(Long documentId) {
        return new ChildChunk(childId, parentId, documentId, content, chunkIndex);
    }

    public ChildChunk withChildId(Long childId) {
        return new ChildChunk(childId, parentId, documentId, content, chunkIndex);
    }

    /** 임베딩에 사용할 텍스트. 섹션 제목을 앞에 붙여 검색 품질을 높인다. */
    public String toEmbeddingText(String sectionTitle) {
        if (sectionTitle == null || sectionTitle.isBlank()) {
            return content;
        }
        return sectionTitle + "\n\n" + content;
    }
}
