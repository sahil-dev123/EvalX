package com.evalx;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import java.io.File;

public class MasterSearchTest {
    @Test
    public void search() throws Exception {
        String target = "2284829154";
        File qp = new File("/Users/sahilkhundiya/Desktop/EvalX/EvalX/question_paper.pdf");
        File ak = new File("/Users/sahilkhundiya/Desktop/EvalX/EvalX/anwwer_key.pdf");

        try (PDDocument doc = Loader.loadPDF(qp)) {
            String text = new PDFTextStripper().getText(doc);
            System.out.println("In QP: " + text.contains(target));
        }
        try (PDDocument doc = Loader.loadPDF(ak)) {
            String text = new PDFTextStripper().getText(doc);
            System.out.println("In AK: " + text.contains(target));
        }
    }
}
