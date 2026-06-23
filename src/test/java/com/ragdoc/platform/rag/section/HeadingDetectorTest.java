package com.ragdoc.platform.rag.section;

import static org.assertj.core.api.Assertions.assertThat;

import com.ragdoc.platform.rag.pdf.PdfLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HeadingDetectorTest {

    private HeadingDetector headingDetector;

    @BeforeEach
    void setUp() {
        headingDetector = new HeadingDetector();
    }

    @Test
    void detectsHeadingWhenScoreMeetsThreshold() {
        PdfLine heading = PdfLine.of("1. Summary", 16f, true);
        PdfLine previous = PdfLine.emptyLine();
        PdfLine next = PdfLine.emptyLine();

        assertThat(headingDetector.isHeading(heading, previous, next, 12f)).isTrue();
        assertThat(headingDetector.score(heading, previous, next, 12f)).isGreaterThanOrEqualTo(4);
    }

    @Test
    void rejectsLongBodyLine() {
        PdfLine body = PdfLine.of(
                "This is a long body paragraph that should not be treated as a section heading under any normal condition.",
                12f,
                false
        );

        assertThat(headingDetector.isHeading(body, PdfLine.emptyLine(), PdfLine.emptyLine(), 12f)).isFalse();
    }

    @Test
    void detectsChapterNumberingPattern() {
        PdfLine heading = PdfLine.of("Chapter 1", 14f, false);

        assertThat(headingDetector.isHeading(heading, PdfLine.emptyLine(), PdfLine.emptyLine(), 12f)).isTrue();
    }
}
