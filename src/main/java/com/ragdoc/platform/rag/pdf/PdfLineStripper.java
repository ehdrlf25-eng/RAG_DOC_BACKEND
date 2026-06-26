package com.ragdoc.platform.rag.pdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

/**
 * PDFBox 커스텀 stripper.
 * <p>
 * 읽기 순서를 보존하면서 라인 단위로 텍스트와 폰트 메타데이터(크기, 볼드)를 추출한다.
 * {@link com.ragdoc.platform.rag.section.HeadingDetector}의 헤딩 감지 입력으로 사용된다.
 */
public class PdfLineStripper extends PDFTextStripper {

    private final List<PdfLine> lines = new ArrayList<>();
    private final StringBuilder lineBuffer = new StringBuilder();
    private final List<TextPosition> linePositions = new ArrayList<>();

    public PdfLineStripper() throws IOException {
        super();
        setSortByPosition(true);
    }

    public List<PdfLine> extractLines() {
        return List.copyOf(lines);
    }

    @Override
    protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
        lineBuffer.append(text);
        linePositions.addAll(textPositions);
    }

    @Override
    protected void writeLineSeparator() throws IOException {
        flushCurrentLine();
        super.writeLineSeparator();
    }

    @Override
    protected void writeParagraphEnd() throws IOException {
        flushCurrentLine();
        lines.add(PdfLine.emptyLine()); // 문단 경계를 빈 라인으로 표시
        super.writeParagraphEnd();
    }

    @Override
    protected void endPage(org.apache.pdfbox.pdmodel.PDPage page) throws IOException {
        flushCurrentLine();
        super.endPage(page);
    }

    public void finish() {
        flushCurrentLine();
    }

    private void flushCurrentLine() {
        if (lineBuffer.isEmpty()) {
            return;
        }

        String text = lineBuffer.toString().trim();
        if (text.isEmpty()) {
            lines.add(PdfLine.emptyLine());
        } else {
            lines.add(buildLine(text, linePositions));
        }

        lineBuffer.setLength(0);
        linePositions.clear();
    }

    private PdfLine buildLine(String text, List<TextPosition> positions) {
        float fontSize = (float) positions.stream()
                .mapToDouble(TextPosition::getFontSizeInPt)
                .average()
                .orElse(12d);
        boolean bold = positions.stream().anyMatch(this::isBoldFont);
        return PdfLine.of(text, fontSize, bold);
    }

    private boolean isBoldFont(TextPosition position) {
        String fontName = position.getFont().getName().toLowerCase();
        return fontName.contains("bold")
                || fontName.contains("black")
                || fontName.contains("heavy")
                || fontName.contains("semibold");
    }
}
