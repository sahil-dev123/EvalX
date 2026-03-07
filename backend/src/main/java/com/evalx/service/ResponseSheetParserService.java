package com.evalx.service;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResponseSheetParserService {

    @Data
    @Builder
    public static class ParsedQuestion {
        private Long id; // Question Number (1, 2...)
        private String option;
        private String type;
        private Double marks;
    }

    /**
     * Parses a candidate response sheet PDF.
     * Uses Question Number (Q.1, Q.2...) as the key.
     */
    public Map<Long, String> parseResponseSheet(MultipartFile file) throws IOException {
        log.info("GENERIC_PARSER: Parsing candidate response sheet: {}", file.getOriginalFilename());
        Map<Long, String> responses = new HashMap<>();
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            // Chunk by Q.N to handle each question block separately
            Pattern chunkPattern = Pattern.compile("(?m)^Q\\.(\\d+)(.*?)(?=(?:^Q\\.\\d+)|(?:Organizing Institute)|$)",
                    Pattern.DOTALL);
            Matcher chunkM = chunkPattern.matcher(text);

            while (chunkM.find()) {
                long qNum = Long.parseLong(chunkM.group(1));
                String block = chunkM.group(2);
                String answer = null;

                // Match MCQ/MSQ: Chosen Option : A,B
                Matcher choiceM = Pattern.compile("(?i)Chosen\\s*Option\\s*[:.-]?\\s*([A-D,]+)").matcher(block);
                if (choiceM.find()) {
                    answer = choiceM.group(1).trim();
                }

                // Match NAT: Given Answer : 48
                if (answer == null || answer.equals("--")) {
                    Matcher natM = Pattern.compile("(?i)Given\\s*Answer\\s*[:.-]?\\s*([-\\d.]+)").matcher(block);
                    if (natM.find()) {
                        answer = natM.group(1).trim();
                    }
                }

                if (answer != null && !answer.equals("--")) {
                    responses.put(qNum, answer);
                    log.debug("PARSED_CANDIDATE: Q.{} -> {}", qNum, answer);
                }
            }
        } catch (Exception e) {
            log.warn("GENERIC_PARSER_ERROR: Failed to parse candidate PDF: {}", e.getMessage());
        }
        log.info("GENERIC_PARSER_FINISH: Extracted {} answers from candidate sheet.", responses.size());
        return responses;
    }

    /**
     * Parses official answer key PDF.
     * Table Format: Q. No. | Q. Type | Section | Key/Range
     */
    public Map<Long, ParsedQuestion> parseAnswerKeyExtended(MultipartFile file) throws IOException {
        log.info("GENERIC_PARSER: Parsing official answer key: {}", file.getOriginalFilename());
        Map<Long, ParsedQuestion> questions = new HashMap<>();
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            // Extract table rows using regex
            // Format example: 34 NAT CS-2 -60.25 to -60.25
            // 25 MSQ CS-2 A;B
            Pattern rowPattern = Pattern.compile("(?m)^(\\d+)\\s+(MCQ|MSQ|NAT)\\s+\\S+\\s+(.*)$");
            Matcher m = rowPattern.matcher(text);

            while (m.find()) {
                long qNum = Long.parseLong(m.group(1));
                String type = m.group(2).toUpperCase();
                String key = m.group(3).trim();

                // Marks logic: usually GA is 1 mark (1-5) 2 marks (6-10). CS is mixed.
                // We'll use a simple heuristic based on question number if not found elsewhere.
                double marks = (qNum <= 5 || (qNum >= 11 && qNum <= 35)) ? 1.0 : 2.0;

                questions.put(qNum, ParsedQuestion.builder()
                        .id(qNum)
                        .option(key)
                        .type(type)
                        .marks(marks)
                        .build());
                log.debug("PARSED_OFFICIAL: Q.{} [{}] -> {}", qNum, type, key);
            }
        } catch (Exception e) {
            log.warn("GENERIC_PARSER_ERROR: Failed to parse official key PDF: {}", e.getMessage());
        }
        log.info("GENERIC_PARSER_FINISH: Extracted {} questions from official key.", questions.size());
        return questions;
    }

    public Map<Long, String> parseAnswerKey(MultipartFile file) throws IOException {
        Map<Long, ParsedQuestion> extended = parseAnswerKeyExtended(file);
        Map<Long, String> simple = new HashMap<>();
        extended.forEach((id, q) -> simple.put(id, q.getOption()));
        return simple;
    }
}
