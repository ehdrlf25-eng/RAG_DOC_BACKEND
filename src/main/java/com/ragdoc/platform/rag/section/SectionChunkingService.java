package com.ragdoc.platform.rag.section;

import com.ragdoc.platform.rag.ingestion.ParentSection;
import com.ragdoc.platform.rag.ingestion.SemanticChunkingService;
import com.ragdoc.platform.rag.parentchild.ChildChunk;
import com.ragdoc.platform.rag.pdf.PdfLine;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * PDF 라인 → Section(Parent) + ChildChunk ingestion 오케스트레이션.
 * 헤딩 감지는 {@link HeadingDetector}에 위임하고, semantic child 분할은
 * {@link SemanticChunkingService}에 위임한다.
 */
@Service
public class SectionChunkingService {

    private static final Logger log = LoggerFactory.getLogger(SectionChunkingService.class);
    private static final String DEFAULT_SECTION_TITLE = "Document";

    private final HeadingDetector headingDetector;
    private final SemanticChunkingService semanticChunkingService;

    public SectionChunkingService(
            HeadingDetector headingDetector,
            SemanticChunkingService semanticChunkingService
    ) {
        this.headingDetector = headingDetector;
        this.semanticChunkingService = semanticChunkingService;
    }

    /**
     * 추출된 PDF 라인을 ParentSection + ChildChunk 구조로 변환한다.
     */
    public List<ParentSection> buildParentSections(List<PdfLine> lines) {
        if (lines == null || lines.isEmpty()) {
            log.info("Section ingestion skipped: no lines");
            return List.of();
        }

        float bodyFontSize = estimateBodyFontSize(lines);
        List<SectionDraft> drafts = extractSectionDrafts(lines, bodyFontSize);
        if (drafts.isEmpty()) {
            drafts = List.of(buildFallbackDraft(lines));
        }
        List<SectionDraft> consistentDrafts = enforceSectionChunkConsistency(drafts);

        List<ParentSection> parentSections = new ArrayList<>(consistentDrafts.size());
        for (SectionDraft draft : consistentDrafts) {
            List<ChildChunk> children = semanticChunkingService.chunk(draft.body());
            boolean orphanFromChunking = children.isEmpty();
            if (children.isEmpty()) {
                children = List.of(ChildChunk.of(buildFallbackContent(draft), 0));
            }
            parentSections.add(ParentSection.of(
                    draft.title(),
                    draft.body(),
                    draft.sectionIndex(),
                    children,
                    draft.isOrphan() || orphanFromChunking
            ));
        }

        logIngestionStats(lines.size(), bodyFontSize, parentSections, drafts.size() - consistentDrafts.size());
        return parentSections;
    }

    private List<SectionDraft> extractSectionDrafts(List<PdfLine> lines, float bodyFontSize) {
        List<SectionDraft> drafts = new ArrayList<>();
        String currentTitle = null;
        StringBuilder currentBody = new StringBuilder();

        for (int i = 0; i < lines.size(); i++) {
            PdfLine line = lines.get(i);
            PdfLine previous = i > 0 ? lines.get(i - 1) : null;
            PdfLine next = i < lines.size() - 1 ? lines.get(i + 1) : null;

            if (headingDetector.isHeading(line, previous, next, bodyFontSize)) {
                flushDraft(drafts, currentTitle, currentBody);
                currentTitle = line.text();
                currentBody = new StringBuilder();
                continue;
            }

            if (line.isBlank()) {
                appendParagraphBreak(currentBody);
                continue;
            }

            appendLine(currentBody, line.text());
        }

        flushDraft(drafts, currentTitle, currentBody);
        return drafts;
    }

    private void flushDraft(List<SectionDraft> drafts, String title, StringBuilder body) {
        String normalizedBody = body.toString().trim();
        if ((title == null || title.isBlank()) && normalizedBody.isEmpty()) {
            return;
        }

        String sectionTitle = (title == null || title.isBlank()) ? DEFAULT_SECTION_TITLE : title.trim();
        drafts.add(new SectionDraft(sectionTitle, normalizedBody, drafts.size(), normalizedBody.isBlank()));
    }

    private SectionDraft buildFallbackDraft(List<PdfLine> lines) {
        StringBuilder body = new StringBuilder();
        for (PdfLine line : lines) {
            if (!line.isBlank()) {
                appendLine(body, line.text());
            }
        }
        String normalized = body.toString().trim();
        if (normalized.isBlank()) {
            normalized = DEFAULT_SECTION_TITLE;
        }
        return new SectionDraft(DEFAULT_SECTION_TITLE, normalized, 0, false);
    }

    private void appendLine(StringBuilder content, String lineText) {
        if (!content.isEmpty() && !content.toString().endsWith("\n\n")) {
            content.append(' ');
        }
        content.append(lineText);
    }

    private void appendParagraphBreak(StringBuilder content) {
        if (!content.isEmpty() && !content.toString().endsWith("\n\n")) {
            content.append("\n\n");
        }
    }

    private List<SectionDraft> enforceSectionChunkConsistency(List<SectionDraft> drafts) {
        List<SectionDraft> resolved = new ArrayList<>();

        for (int i = 0; i < drafts.size(); i++) {
            SectionDraft current = drafts.get(i);
            if (!current.isOrphan()) {
                resolved.add(new SectionDraft(current.title(), current.body(), resolved.size(), false));
                continue;
            }

            int nextIndex = findNextNonOrphanIndex(drafts, i + 1);
            if (nextIndex >= 0) {
                SectionDraft next = drafts.get(nextIndex);
                String mergedBody = mergeOrphanIntoBody(current, next.body(), true);
                drafts.set(nextIndex, new SectionDraft(next.title(), mergedBody, next.sectionIndex(), false));
                continue;
            }

            if (!resolved.isEmpty()) {
                SectionDraft previous = resolved.getLast();
                String mergedBody = mergeOrphanIntoBody(current, previous.body(), false);
                resolved.set(
                        resolved.size() - 1,
                        new SectionDraft(previous.title(), mergedBody, previous.sectionIndex(), false)
                );
                continue;
            }

            // 문서 전체가 orphan heading만 남은 경우에도 retrieval 가능하도록 rescue한다.
            resolved.add(new SectionDraft(current.title(), current.title(), resolved.size(), true));
        }

        return resolved;
    }

    private int findNextNonOrphanIndex(List<SectionDraft> drafts, int startIndex) {
        for (int i = startIndex; i < drafts.size(); i++) {
            if (!drafts.get(i).isOrphan()) {
                return i;
            }
        }
        return -1;
    }

    private String mergeOrphanIntoBody(SectionDraft orphan, String targetBody, boolean prepend) {
        String orphanMarker = orphan.title();
        if (orphanMarker == null || orphanMarker.isBlank()) {
            return targetBody;
        }
        if (targetBody == null || targetBody.isBlank()) {
            return orphanMarker;
        }
        return prepend
                ? orphanMarker + "\n\n" + targetBody
                : targetBody + "\n\n" + orphanMarker;
    }

    private String buildFallbackContent(SectionDraft draft) {
        if (draft.body() != null && !draft.body().isBlank()) {
            return draft.body();
        }
        return (draft.title() == null || draft.title().isBlank()) ? DEFAULT_SECTION_TITLE : draft.title();
    }

    private void logIngestionStats(
            int lineCount,
            float bodyFontSize,
            List<ParentSection> parentSections,
            int mergedOrphanSectionCount
    ) {
        int totalChildCount = parentSections.stream().mapToInt(section -> section.children().size()).sum();
        int averageChildChars = totalChildCount == 0
                ? 0
                : parentSections.stream()
                        .flatMap(section -> section.children().stream())
                        .mapToInt(child -> child.content().length())
                        .sum() / totalChildCount;

        log.info(
                "Section ingestion completed lineCount={} bodyFontSize={} sectionCount={} totalChildCount={} averageChildChars={} mergedOrphanSectionCount={}",
                lineCount,
                bodyFontSize,
                parentSections.size(),
                totalChildCount,
                averageChildChars,
                mergedOrphanSectionCount
        );

        for (ParentSection section : parentSections) {
            int sectionAvgChars = section.children().isEmpty()
                    ? 0
                    : section.children().stream().mapToInt(child -> child.content().length()).sum()
                            / section.children().size();
            log.debug(
                    "Section ingestion detail sectionIndex={} sectionTitle={} childCount={} averageChunkChars={} isOrphan={}",
                    section.sectionIndex(),
                    section.sectionTitle(),
                    section.children().size(),
                    sectionAvgChars,
                    section.isOrphan()
            );
        }
    }

    private float estimateBodyFontSize(List<PdfLine> lines) {
        List<Float> candidates = lines.stream()
                .filter(line -> !line.isBlank())
                .filter(line -> line.text().length() > 20)
                .map(PdfLine::fontSize)
                .filter(size -> size > 0)
                .sorted()
                .toList();

        if (candidates.isEmpty()) {
            return lines.stream()
                    .filter(line -> !line.isBlank())
                    .map(PdfLine::fontSize)
                    .filter(size -> size > 0)
                    .findFirst()
                    .orElse(12f);
        }

        int middle = candidates.size() / 2;
        return candidates.get(middle);
    }

    private record SectionDraft(String title, String body, int sectionIndex, boolean isOrphan) {
    }
}
