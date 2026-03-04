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
     * Parse a response file (CSV or JSON) and return a map of questionNumber → answer.
     */
    public Map<Long, String> parseResponseFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) throw new InvalidFileException("File name is required");

        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();

        return switch (ext) {
            case "csv" -> parseCsv(file);
            case "json" -> parseJson(file);
            case "pdf" -> parsePdf(file);
            default -> throw new InvalidFileException("Unsupported file format: " + ext + ". Supported: CSV, JSON, PDF");
        };
    }

    private Map<Long, String> parsePdf(MultipartFile file) {
        Map<Long, String> answers = new LinkedHashMap<>();
        try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(file.getInputStream())) {
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            String text = stripper.getText(document);
            
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
                    // Extract question number
                    String qNumStr = req.substring(0, req.indexOf('\n')).trim();
                    long qNum = Long.parseLong(qNumStr);

                    // Extract chosen option or given answer
                    java.util.regex.Matcher m1 = java.util.regex.Pattern.compile("Chosen Option : ([A-D])").matcher(req);
                    java.util.regex.Matcher m2 = java.util.regex.Pattern.compile("Given Answer : (.*)").matcher(req);
                    
                    if (m1.find()) {
                        answers.put(qNum, m1.group(1).trim());
                    } else if (m2.find()) {
                        answers.put(qNum, m2.group(1).trim());
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
        return answers;
    }

    private Map<Long, String> parseCsv(MultipartFile file) {
        Map<Long, String> answers = new LinkedHashMap<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            List<String[]> rows = reader.readAll();
            boolean hasHeader = false;

            for (String[] row : rows) {
                if (row.length < 2) continue;

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
                    long qNum = Long.parseLong(row[0].trim());
                    String answer = row[1].trim();
                    if (!answer.isEmpty()) {
                        answers.put(qNum, answer);
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

    private Map<Long, String> parseJson(MultipartFile file) {
        Map<Long, String> answers = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> items = objectMapper.readValue(
                    file.getInputStream(), new TypeReference<>() {});

            for (Map<String, Object> item : items) {
                Object qId = item.getOrDefault("questionId",
                        item.getOrDefault("questionNumber",
                                item.getOrDefault("question_id",
                                        item.get("q"))));
                Object ans = item.getOrDefault("answer",
                        item.getOrDefault("selectedAnswer",
                                item.get("a")));

                if (qId != null && ans != null) {
                    long qNum = qId instanceof Number ? ((Number) qId).longValue() : Long.parseLong(qId.toString());
                    answers.put(qNum, ans.toString().trim());
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
