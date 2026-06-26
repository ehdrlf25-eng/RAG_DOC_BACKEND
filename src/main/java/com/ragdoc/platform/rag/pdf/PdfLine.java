package com.ragdoc.platform.rag.pdf;

/**
 * PDF에서 추출한 한 줄 텍스트와 폰트 메타데이터.
 * {@link PdfLineStripper} 출력 단위이며 헤딩 감지 입력으로 사용된다.
 */
public record PdfLine(
        String text,
        float fontSize,
        boolean bold,
        boolean isBlank
) {

    /** 빈 줄(공백 라인) 인스턴스를 반환한다. */
    public static PdfLine emptyLine() {
        return new PdfLine("", 0f, false, true);
    }

    /** 텍스트를 trim하고 공백이면 {@link #emptyLine()}으로 변환한다. */
    public static PdfLine of(String text, float fontSize, boolean bold) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.isEmpty()) {
            return emptyLine();
        }
        return new PdfLine(normalized, fontSize, bold, false);
    }
}
