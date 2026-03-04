package com.evalx;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import java.io.File;

public class PdfTextExtractorTest {
    @Test
    public void readQuestionPaper() throws Exception {
        try (PDDocument document = Loader.loadPDF(new File("/Users/sahilkhundiya/Desktop/EvalX/EvalX/question_paper.pdf"))) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(2);
            String text = stripper.getText(document);
            System.out.println("--- Question Paper ---");
            System.out.println(text.substring(0, Math.min(text.length(), 2000)));
        }
    }

    @Test
    public void readAnswerKey() throws Exception {
        try (PDDocument document = Loader.loadPDF(new File("/Users/sahilkhundiya/Desktop/EvalX/EvalX/anwwer_key.pdf"))) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(2);
            String text = stripper.getText(document);
            System.out.println("--- Answer Key ---");
            System.out.println(text.substring(0, Math.min(text.length(), 2000)));
        }
    }
}
