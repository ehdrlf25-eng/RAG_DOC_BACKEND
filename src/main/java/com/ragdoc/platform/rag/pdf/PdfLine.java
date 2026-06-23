package com.ragdoc.platform.rag.pdf;

public record PdfLine(
        String text,
        float fontSize,
        boolean bold,
        boolean isBlank
) {

    public static PdfLine emptyLine() {
        return new PdfLine("", 0f, false, true);
    }

    public static PdfLine of(String text, float fontSize, boolean bold) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.isEmpty()) {
            return emptyLine();
        }
        return new PdfLine(normalized, fontSize, bold, false);
    }
}
