package com.ragdoc.platform.rag.section;

import com.ragdoc.platform.rag.pdf.PdfLine;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class HeadingDetector {

    static final int HEADING_SCORE_THRESHOLD = 4;
    static final int SHORT_TEXT_MAX_LENGTH = 80;
    static final float FONT_SIZE_TOLERANCE = 0.5f;

    private static final Pattern NUMBERING_PATTERN = Pattern.compile(
            "^(\\d+\\.\\s*|\\d+-\\d+\\s*|Chapter\\s+\\d+)",
            Pattern.CASE_INSENSITIVE
    );

    public boolean isHeading(PdfLine line, PdfLine previous, PdfLine next, float bodyFontSize) {
        if (line.isBlank()) {
            return false;
        }
        return score(line, previous, next, bodyFontSize) >= HEADING_SCORE_THRESHOLD;
    }

    int score(PdfLine line, PdfLine previous, PdfLine next, float bodyFontSize) {
        int score = 0;
        String text = line.text();

        if (line.fontSize() > bodyFontSize + FONT_SIZE_TOLERANCE) {
            score += 2;
        }
        if (line.bold()) {
            score += 2;
        }
        if (NUMBERING_PATTERN.matcher(text).find()) {
            score += 2;
        }
        if (text.length() < SHORT_TEXT_MAX_LENGTH) {
            score += 1;
        }
        if (isIsolated(previous, next)) {
            score += 1;
        }
        return score;
    }

    private boolean isIsolated(PdfLine previous, PdfLine next) {
        boolean previousBlank = previous == null || previous.isBlank();
        boolean nextBlank = next == null || next.isBlank();
        return previousBlank && nextBlank;
    }
}
