package com.ragdoc.platform.rag.section;

import static org.assertj.core.api.Assertions.assertThat;

import com.ragdoc.platform.config.RagProperties;
import com.ragdoc.platform.rag.ingestion.ParentSection;
import com.ragdoc.platform.rag.ingestion.SemanticChunkingService;
import com.ragdoc.platform.rag.pdf.PdfLine;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SectionChunkingServiceTest {

    private SectionChunkingService sectionChunkingService;

    @BeforeEach
    void setUp() {
        SemanticChunkingService semanticChunkingService = new SemanticChunkingService(
                new RagProperties(
                        new RagProperties.Storage("./storage"),
                        new RagProperties.Retrieval(5, 10, 0.8, 40, 0.3, 60, 20, 5, "passthrough"),
                        new RagProperties.Chunking(512),
                        new RagProperties.Embedding("mock", 768, null, null),
                        new RagProperties.Llm("mock", null, null)
                )
        );
        sectionChunkingService = new SectionChunkingService(
                new HeadingDetector(),
                semanticChunkingService
        );
    }

    @Test
    void buildsParentSectionsFromDetectedHeadings() {
        List<PdfLine> lines = List.of(
                PdfLine.emptyLine(),
                PdfLine.of("1. Introduction", 16f, true),
                PdfLine.emptyLine(),
                PdfLine.of("This is the introduction body text with enough content.", 12f, false),
                PdfLine.emptyLine(),
                PdfLine.of("2. Experience", 16f, true),
                PdfLine.emptyLine(),
                PdfLine.of("Worked on backend systems and RAG pipelines.", 12f, false)
        );

        List<ParentSection> sections = sectionChunkingService.buildParentSections(lines);

        assertThat(sections).hasSize(2);
        assertThat(sections.get(0).sectionTitle()).isEqualTo("1. Introduction");
        assertThat(sections.get(0).fullContent()).contains("introduction body text");
        assertThat(sections.get(0).children()).isNotEmpty();
        assertThat(sections.get(0).isOrphan()).isFalse();
        assertThat(sections.get(1).sectionTitle()).isEqualTo("2. Experience");
        assertThat(sections.get(1).sectionIndex()).isEqualTo(1);
    }

    @Test
    void returnsFallbackSectionWhenNoHeadingDetected() {
        List<PdfLine> lines = List.of(
                PdfLine.of("Only body text without any heading markers here.", 12f, false)
        );

        List<ParentSection> sections = sectionChunkingService.buildParentSections(lines);

        assertThat(sections).hasSize(1);
        assertThat(sections.getFirst().sectionTitle()).isEqualTo("Document");
        assertThat(sections.getFirst().children()).hasSize(1);
        assertThat(sections.getFirst().isOrphan()).isFalse();
    }

    @Test
    void mergesOrphanSectionIntoAdjacentSection() {
        List<PdfLine> lines = List.of(
                PdfLine.of("1. Introduction", 16f, true),
                PdfLine.emptyLine(),
                PdfLine.of("Intro body text for retrieval.", 12f, false),
                PdfLine.emptyLine(),
                PdfLine.of("2. Orphan Heading", 16f, true),
                PdfLine.emptyLine(),
                PdfLine.of("3. Next Section", 16f, true),
                PdfLine.emptyLine(),
                PdfLine.of("Next section body for retrieval too.", 12f, false)
        );

        List<ParentSection> sections = sectionChunkingService.buildParentSections(lines);

        assertThat(sections).hasSize(2);
        assertThat(sections.get(1).fullContent()).contains("2. Orphan Heading");
        assertThat(sections.get(1).children()).isNotEmpty();
        assertThat(sections.get(1).isOrphan()).isFalse();
    }
}
