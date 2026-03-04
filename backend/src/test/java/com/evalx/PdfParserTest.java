package com.evalx;

import com.evalx.service.ResponseParserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PdfParserTest {

    @Autowired
    private ResponseParserService responseParserService;

    @Test
    public void testParseGatePdf() throws Exception {
        File file = new File("/Users/sahilkhundiya/Desktop/EvalX/EvalX/response_sheet.pdf");
        if (!file.exists()) {
            System.out.println("Skipping test, PDF not found");
            return;
        }

        byte[] content = Files.readAllBytes(file.toPath());
        MockMultipartFile mockFile = new MockMultipartFile("file", "response.pdf", "application/pdf", content);

        Map<String, String> answers = responseParserService.parseResponseFile(mockFile);

        assertNotNull(answers);
        assertFalse(answers.isEmpty(), "Answers should not be empty");
        
        System.out.println("✅ Extracted " + answers.size() + " answers from PDF.");
        answers.forEach((q, a) -> System.out.println("Q" + q + " -> " + a));
        
        // Let's check a few known ones based on the earlier text scrape
        // Q.1 -> A
        // Q.2 -> B
        String q1Hash = com.evalx.util.HashUtil.generateHash("1");
        if (answers.containsKey(q1Hash)) {
           assertEquals("A", answers.get(q1Hash));
        }
    }
}
