package com.evalx;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MasterDumpTest {
    @Test
    public void dump() throws Exception {
        try (PDDocument document = Loader.loadPDF(new File("/Users/sahilkhundiya/Desktop/EvalX/EvalX/question_paper.pdf"))) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setStartPage(1);
            stripper.setEndPage(42); // All pages
            String text = stripper.getText(document);
            Files.write(Paths.get("/Users/sahilkhundiya/Desktop/EvalX/EvalX/question_paper_dump.txt"), text.getBytes());
        }
    }
}
