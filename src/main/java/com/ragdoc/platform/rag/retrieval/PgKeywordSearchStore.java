package com.ragdoc.platform.rag.retrieval;

import com.ragdoc.platform.rag.vector.ChunkSearchResult;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class PgKeywordSearchStore implements KeywordSearchStore {

    private static final Logger log = LoggerFactory.getLogger(PgKeywordSearchStore.class);

    private final JdbcTemplate jdbcTemplate;

    public PgKeywordSearchStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ChunkSearchResult> search(Long userId, String query, int limit) {
        List<ChunkSearchResult> ftsResults = searchFullText(userId, query, limit);
        if (!ftsResults.isEmpty()) {
            log.info(
                    "Keyword FTS search completed userId={} limit={} resultCount={} scores={}",
                    userId,
                    limit,
                    ftsResults.size(),
                    formatScores(ftsResults)
            );
            return ftsResults;
        }

        List<ChunkSearchResult> likeResults = searchLikeFallback(userId, query, limit);
        log.info(
                "Keyword LIKE fallback completed userId={} limit={} resultCount={}",
                userId,
                limit,
                likeResults.size()
        );
        return likeResults;
    }

    private List<ChunkSearchResult> searchFullText(Long userId, String query, int limit) {
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
                       ts_rank(
                           to_tsvector('simple', coalesce(dc.content, '') || ' ' || coalesce(dc.section_title, '')),
                           plainto_tsquery('simple', ?)
                       ) AS score
                FROM document_chunks dc
                JOIN parent_sections ps ON ps.id = dc.parent_section_id
                JOIN documents d ON d.id = dc.document_id
                WHERE d.user_id = ?
                  AND d.status = 'READY'
                  AND dc.parent_section_id IS NOT NULL
                  AND to_tsvector('simple', coalesce(dc.content, '') || ' ' || coalesce(dc.section_title, ''))
                      @@ plainto_tsquery('simple', ?)
                ORDER BY score DESC
                LIMIT ?
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> mapRow(rs),
                sanitizeForFullText(query),
                userId,
                sanitizeForFullText(query),
                limit
        );
    }

    private List<ChunkSearchResult> searchLikeFallback(Long userId, String query, int limit) {
        String pattern = "%" + query.replace("%", "").replace("_", "") + "%";
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
                       1.0 AS score
                FROM document_chunks dc
                JOIN parent_sections ps ON ps.id = dc.parent_section_id
                JOIN documents d ON d.id = dc.document_id
                WHERE d.user_id = ?
                  AND d.status = 'READY'
                  AND dc.parent_section_id IS NOT NULL
                  AND (
                      dc.content ILIKE ?
                      OR dc.section_title ILIKE ?
                      OR ps.title ILIKE ?
                      OR ps.full_content ILIKE ?
                  )
                ORDER BY dc.id
                LIMIT ?
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> mapRow(rs),
                userId,
                pattern,
                pattern,
                pattern,
                pattern,
                limit
        );
    }

    private ChunkSearchResult mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new ChunkSearchResult(
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
        );
    }

    private String sanitizeForFullText(String query) {
        return query.replace("'", " ").trim();
    }

    private String formatScores(List<ChunkSearchResult> results) {
        return results.stream()
                .map(result -> String.format("%.4f", result.score()))
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
