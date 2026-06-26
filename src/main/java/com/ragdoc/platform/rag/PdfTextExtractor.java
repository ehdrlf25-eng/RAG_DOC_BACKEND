package com.ragdoc.platform.rag;

import com.ragdoc.platform.rag.pdf.PdfLine;
import com.ragdoc.platform.rag.pdf.PdfLineStripper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * PDF 문서에서 텍스트 라인과 폰트 메타데이터를 추출하는 서비스.
 * <p>
 * RAG ingestion 파이프라인의 첫 단계로, {@link com.ragdoc.platform.rag.pdf.PdfLineStripper}를 통해
 * 읽기 순서가 보존된 라인 목록을 생성한다.
 */
@Service
public class PdfTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(PdfTextExtractor.class);

    /**
     * PDF 입력 스트림에서 페이지별 텍스트 라인을 추출한다.
     *
     * @return 라인 목록과 페이지 수
     */
    public PdfExtractionResult extract(InputStream inputStream) throws IOException {
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PdfLineStripper stripper = new PdfLineStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(document.getNumberOfPages());
            stripper.getText(document);
            stripper.finish();

            List<PdfLine> lines = stripper.extractLines();
            int pageCount = document.getNumberOfPages();
            int nonBlankLineCount = (int) lines.stream().filter(line -> !line.isBlank()).count();

            log.info(
                    "PDF lines extracted pageCount={} totalLineCount={} nonBlankLineCount={}",
                    pageCount,
                    lines.size(),
                    nonBlankLineCount
            );
            return new PdfExtractionResult(lines, pageCount);
        }
    }

    public record PdfExtractionResult(List<PdfLine> lines, int pageCount) {
    }
}
