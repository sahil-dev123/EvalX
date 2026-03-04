package com.evalx.service;

import com.evalx.exception.InvalidFileException;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class ResponseParserService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Parse a response file (CSV or JSON) and return a map of questionNumber →
     * answer.
     */
    public ResponseData parseResponseFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null)
            throw new InvalidFileException("File name is required");

        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();

        return switch (ext) {
            case "pdf" -> parsePdf(file);
            case "csv" -> new ResponseData(parseCsv(file), null);
            case "json" -> new ResponseData(parseJson(file), null);
            default ->
                throw new InvalidFileException("Unsupported file format: " + ext + ". Supported: CSV, JSON, PDF");
        };
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class ResponseData {
        private Map<String, String> answers;
        private Map<String, String> metadata;
    }

    private ResponseData parsePdf(MultipartFile file) {
        Map<String, String> answers = new LinkedHashMap<>();
        Map<String, String> metadata = new HashMap<>();
        try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.Loader.loadPDF(file.getBytes())) {
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            String text = stripper.getText(document);

            // Extract Metadata for Auto-Detection
            // Example: "GATE 2026 8th Feb 26 S2"
            java.util.regex.Matcher metaMatcher = java.util.regex.Pattern.compile("(GATE|SSC|CAT)\\s+(\\d{4})\\s+(.*)")
                    .matcher(text);
            if (metaMatcher.find()) {
                metadata.put("exam", metaMatcher.group(1));
                metadata.put("year", metaMatcher.group(2));
                metadata.put("raw_shift", metaMatcher.group(3).trim());
            }

            // Extract Shift Specifics (e.g., S1, S2, Morning, Afternoon)
            java.util.regex.Matcher shiftMatcher = java.util.regex.Pattern
                    .compile("(S1|S2|Shift \\d|Morning|Afternoon)").matcher(text);
            if (shiftMatcher.find()) {
                metadata.put("shift", shiftMatcher.group(1));
            }

            // GATE response sheet format:
            // "Q.1"
            // ...
            // "Question Type : MCQ"
            // "Question ID : 2284829154"
            // "Status : Answered"
            // "Chosen Option : A"
            // OR "Given Answer : 2.5"

            String[] blocks = text.split("Q\\.");
            for (int i = 1; i < blocks.length; i++) {
                String req = blocks[i];
                try {
                    // Extract question text: between the first newline and 'Question Type :'
                    int firstNewline = req.indexOf('\n');
                    int typeMarker = req.indexOf("Question Type :");

                    String qText = "";
                    if (firstNewline != -1 && typeMarker != -1 && typeMarker > firstNewline) {
                        qText = req.substring(firstNewline, typeMarker).trim();
                        // Clean up text to match master paper extraction (whitespace, footers)
                        qText = qText.replaceAll("(?i)Organizing Institute:.*Page \\d+ of \\d+", "").trim();
                    }

                    if (qText.isEmpty())
                        continue;

                    String qHash = com.evalx.util.HashUtil.generateHash(qText);

                    // Extract chosen option or given answer
                    java.util.regex.Matcher m1 = java.util.regex.Pattern.compile("Chosen Option : ([A-D])")
                            .matcher(req);
                    java.util.regex.Matcher m2 = java.util.regex.Pattern.compile("Given Answer : (.*)").matcher(req);

                    if (m1.find()) {
                        answers.put(qHash, m1.group(1).trim());
                    } else if (m2.find()) {
                        answers.put(qHash, m2.group(1).trim());
                    }
                } catch (Exception e) {
                    log.debug("Error parsing block: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            throw new InvalidFileException("Failed to read PDF: " + e.getMessage());
        }

        if (answers.isEmpty()) {
            throw new InvalidFileException("Could not extract any completed answers from the PDF.");
        }
        return new ResponseData(answers, metadata);
    }

    private Map<String, String> parseCsv(MultipartFile file) {
        Map<String, String> answers = new LinkedHashMap<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            List<String[]> rows = reader.readAll();
            boolean hasHeader = false;

            for (String[] row : rows) {
                if (row.length < 2)
                    continue;

                // Skip header row
                if (!hasHeader && (row[0].equalsIgnoreCase("questionId") ||
                        row[0].equalsIgnoreCase("questionNumber") ||
                        row[0].equalsIgnoreCase("question_id") ||
                        row[0].equalsIgnoreCase("q"))) {
                    hasHeader = true;
                    continue;
                }
                hasHeader = true;

                try {
                    String qHash = com.evalx.util.HashUtil.generateHash(row[0].trim());
                    String answer = row[1].trim();
                    if (!answer.isEmpty()) {
                        answers.put(qHash, answer);
                    }
                } catch (NumberFormatException e) {
                    log.warn("Skipping invalid row: {}", Arrays.toString(row));
                }
            }
        } catch (IOException | CsvException e) {
            throw new InvalidFileException("Failed to parse CSV: " + e.getMessage());
        }

        if (answers.isEmpty()) {
            throw new InvalidFileException("No valid responses found in CSV file");
        }
        return answers;
    }

    private Map<String, String> parseJson(MultipartFile file) {
        Map<String, String> answers = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> items = objectMapper.readValue(
                    file.getInputStream(), new TypeReference<>() {
                    });

            for (Map<String, Object> item : items) {
                Object qId = item.getOrDefault("questionId",
                        item.getOrDefault("questionNumber",
                                item.getOrDefault("question_id",
                                        item.get("q"))));
                Object ans = item.getOrDefault("answer",
                        item.getOrDefault("selectedAnswer",
                                item.get("a")));

                if (qId != null && ans != null) {
                    String qHash = com.evalx.util.HashUtil.generateHash(qId.toString());
                    answers.put(qHash, ans.toString().trim());
                }
            }
        } catch (IOException e) {
            throw new InvalidFileException("Failed to parse JSON: " + e.getMessage());
        }

        if (answers.isEmpty()) {
            throw new InvalidFileException("No valid responses found in JSON file");
        }
        return answers;
    }
}
