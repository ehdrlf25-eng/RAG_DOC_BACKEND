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

@Service
public class PdfTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(PdfTextExtractor.class);

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
