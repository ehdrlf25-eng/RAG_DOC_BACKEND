package com.ragdoc.platform.rag.vector;

import com.pgvector.PGvector;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * PostgreSQL pgvector 기반 벡터 저장소.
 * <p>
 * Child chunk 임베딩을 저장하고 HNSW 인덱스를 활용한 코사인 거리 검색을 수행한다.
 * 운영 환경({@code !test} 프로필)에서 사용된다.
 */
@Component
@Profile("!test")
public class PgVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(PgVectorStore.class);

    private final JdbcTemplate jdbcTemplate;

    public PgVectorStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void saveEmbedding(Long chunkId, float[] embedding) {
        jdbcTemplate.update(
                "UPDATE document_chunks SET embedding = ? WHERE id = ?",
                new PGvector(embedding),
                chunkId
        );
    }

    /**
     * Child chunk만 검색하고 Parent section 메타데이터를 함께 반환한다.
     * <p>
     * pgvector {@code <=>} 연산자(코사인 거리)를 사용하며, score = 1 - distance로 변환한다.
     */
    @Override
    public List<ChunkSearchResult> searchSimilar(
            Long userId,
            float[] queryEmbedding,
            int topK,
            double minSimilarity
    ) {
        String sql = """
                SELECT dc.id,
                       dc.parent_section_id,
                       dc.document_id,
                       d.original_filename,
                       ps.section_index,
                       dc.chunk_index,
                       ps.title AS section_title,
                       dc.content AS child_content,
                       ps.full_content AS parent_content,
                       1 - (dc.embedding <=> ?) AS score
                FROM document_chunks dc
                JOIN parent_sections ps ON ps.id = dc.parent_section_id
                JOIN documents d ON d.id = dc.document_id
                WHERE d.user_id = ?
                  AND d.status = 'READY'
                  AND dc.parent_section_id IS NOT NULL
                  AND dc.embedding IS NOT NULL
                  AND (1 - (dc.embedding <=> ?)) >= ?
                ORDER BY dc.embedding <=> ?
                LIMIT ?
                """;

        PGvector queryVector = new PGvector(queryEmbedding);

        List<ChunkSearchResult> results = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new ChunkSearchResult(
                        rs.getLong("id"),
                        rs.getLong("parent_section_id"),
                        rs.getLong("document_id"),
                        rs.getString("original_filename"),
                        rs.getInt("section_index"),
                        rs.getInt("chunk_index"),
                        rs.getString("section_title"),
                        rs.getString("child_content"),
                        rs.getString("parent_content"),
                        rs.getDouble("score")
                ),
                queryVector,
                userId,
                queryVector,
                minSimilarity,
                queryVector,
                topK
        );

        log.info(
                "Vector search completed userId={} topK={} minSimilarity={} queryDimensions={} childResultCount={} scores={}",
                userId,
                topK,
                minSimilarity,
                queryEmbedding.length,
                results.size(),
                results.stream()
                        .map(result -> String.format("%.4f", result.score()))
                        .collect(Collectors.joining(", ", "[", "]"))
        );
        return results;
    }
}
