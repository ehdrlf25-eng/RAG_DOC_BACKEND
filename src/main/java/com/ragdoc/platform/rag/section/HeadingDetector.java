package com.ragdoc.platform.rag.section;

import com.ragdoc.platform.rag.pdf.PdfLine;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * PDF 섹션 헤딩 감지 컴포넌트.
 * <p>
 * 폰트 크기, 볼드, 번호 패턴, 텍스트 길이, 고립 여부를 점수화하여
 * 본문과 섹션 제목을 구분한다.
 */
@Component
public class HeadingDetector {

    static final int HEADING_SCORE_THRESHOLD = 4;
    static final int SHORT_TEXT_MAX_LENGTH = 80;
    static final float FONT_SIZE_TOLERANCE = 0.5f;

    private static final Pattern NUMBERING_PATTERN = Pattern.compile(
            "^(\\d+\\.\\s*|\\d+-\\d+\\s*|Chapter\\s+\\d+)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 헤딩 여부를 판별한다. 누적 점수가 {@link #HEADING_SCORE_THRESHOLD} 이상이면 헤딩으로 간주.
     */
    public boolean isHeading(PdfLine line, PdfLine previous, PdfLine next, float bodyFontSize) {
        if (line.isBlank()) {
            return false;
        }
        return score(line, previous, next, bodyFontSize) >= HEADING_SCORE_THRESHOLD;
    }

    /**
     * 헤딩 점수를 계산한다. 폰트 크기·볼드·번호 패턴·짧은 텍스트·고립 여부를 가중 합산.
     */
    int score(PdfLine line, PdfLine previous, PdfLine next, float bodyFontSize) {
        int score = 0;
        String text = line.text();

        if (line.fontSize() > bodyFontSize + FONT_SIZE_TOLERANCE) {
            score += 2; // 본문 대비 큰 폰트
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
            score += 1; // 앞뒤 빈 줄로 둘러싸인 단독 라인
        }
        return score;
    }

    private boolean isIsolated(PdfLine previous, PdfLine next) {
        boolean previousBlank = previous == null || previous.isBlank();
        boolean nextBlank = next == null || next.isBlank();
        return previousBlank && nextBlank;
    }
}
