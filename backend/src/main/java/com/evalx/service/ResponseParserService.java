package com.evalx.service;

import com.evalx.constants.FileConstants;
import com.evalx.constants.LogConstants;
import com.evalx.exception.InvalidFileException;
import com.evalx.util.HashUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ResponseParserService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Parse a response file (PDF, CSV, or JSON) and return structured ResponseData.
     */
    public ResponseData parseResponseFile(MultipartFile file) {
        log.info(LogConstants.START_METHOD, "parseResponseFile");
        String filename = file.getOriginalFilename();
        if (filename == null) {
            log.error("File upload failed: filename is null");
            throw new InvalidFileException("File name is required");
        }

        log.info("Starting response file parsing: fileName={}, size={} bytes", filename, file.getSize());

        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();

        ResponseData result = switch (ext) {
            case FileConstants.EXT_PDF -> parsePdf(file);
            case FileConstants.EXT_CSV -> new ResponseData(parseCsv(file), null);
            case FileConstants.EXT_JSON -> new ResponseData(parseJson(file), null);
            default -> {
                log.error("Unsupported file extension: {}", ext);
                throw new InvalidFileException("Unsupported file format: " + ext);
            }
        };

        log.info("Successfully parsed {} answers from {}", result.getAnswers().size(), filename);
        log.info(LogConstants.END_METHOD, "parseResponseFile");
        return result;
    }

    @Getter
    @AllArgsConstructor
    public static class ResponseData {
        private Map<String, String> answers;
        private Map<String, String> metadata;
    }

    private ResponseData parsePdf(MultipartFile file) {
        log.debug("Entering parsePdf for {}", file.getOriginalFilename());
        Map<String, String> answers = new LinkedHashMap<>();
        Map<String, String> metadata = new HashMap<>();
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            // Extract Metadata for Auto-Detection
            Matcher metaMatcher = Pattern.compile("(GATE|SSC|CAT)\\s+(\\d{4})\\s+(.*)").matcher(text);
            if (metaMatcher.find()) {
                metadata.put("exam", metaMatcher.group(1));
                metadata.put("year", metaMatcher.group(2));
                metadata.put("raw_shift", metaMatcher.group(3).trim());
                log.debug("Metadata auto-detected from PDF: exam={}, year={}", metadata.get("exam"),
                        metadata.get("year"));
            }

            Matcher shiftMatcher = Pattern.compile("(S1|S2|Shift \\d|Morning|Afternoon)").matcher(text);
            if (shiftMatcher.find()) {
                metadata.put("shift", shiftMatcher.group(1));
                log.debug("Shift auto-detected from PDF: {}", metadata.get("shift"));
            }

            String[] blocks = text.split("Q\\.");
            log.debug("Found {} question blocks in PDF", blocks.length - 1);

            for (int i = 1; i < blocks.length; i++) {
                String req = blocks[i];
                try {
                    int firstNewline = req.indexOf('\n');
                    int typeMarker = req.indexOf("Question Type :");

                    String qText = "";
                    if (firstNewline != -1 && typeMarker != -1 && typeMarker > firstNewline) {
                        qText = req.substring(firstNewline, typeMarker).trim();
                        // Clean up footers
                        qText = qText.replaceAll("(?i)Organizing Institute:.*Page \\d+ of \\d+", "").trim();
                    }

                    if (qText.isEmpty())
                        continue;

                    String qHash = HashUtil.generateHash(qText);
                    Matcher m1 = Pattern.compile("Chosen Option : ([A-D])").matcher(req);
                    Matcher m2 = Pattern.compile("Given Answer : (.*)").matcher(req);

                    if (m1.find()) {
                        String ans = m1.group(1).trim();
                        answers.put(qHash, ans);
                        log.debug("Parsed MCQ answer: hash={}, value={}", qHash, ans);
                    } else if (m2.find()) {
                        String ans = m2.group(1).trim();
                        answers.put(qHash, ans);
                        log.debug("Parsed NAT/MSQ answer: hash={}, value={}", qHash, ans);
                    }
                } catch (Exception e) {
                    log.warn("Error parsing question block {} in PDF: {}", i, e.getMessage());
                }
            }

        } catch (IOException e) {
            log.error("IO Error while parsing PDF response", e);
            throw new InvalidFileException("Failed to read PDF: " + e.getMessage());
        }

        if (answers.isEmpty()) {
            log.warn("PDF parsing completed but 0 answers were extracted");
            throw new InvalidFileException("Could not extract any completed answers from the PDF.");
        }
        return new ResponseData(answers, metadata);
    }

    private Map<String, String> parseCsv(MultipartFile file) {
        log.debug("Entering parseCsv for {}", file.getOriginalFilename());
        Map<String, String> answers = new LinkedHashMap<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            List<String[]> rows = reader.readAll();
            boolean hasHeader = false;

            for (String[] row : rows) {
                if (row.length < 2)
                    continue;

                if (!hasHeader && isHeaderRow(row[0])) {
                    hasHeader = true;
                    continue;
                }
                hasHeader = true;

                try {
                    String qHash = HashUtil.generateHash(row[0].trim());
                    String answer = row[1].trim();
                    if (!answer.isEmpty()) {
                        answers.put(qHash, answer);
                        log.debug("CSV entry: hash={}, answer={}", qHash, answer);
                    }
                } catch (Exception e) {
                    log.warn("Skipping invalid CSV row: {}", Arrays.toString(row));
                }
            }
        } catch (IOException | CsvException e) {
            log.error("Failed to parse CSV response", e);
            throw new InvalidFileException("Failed to parse CSV: " + e.getMessage());
        }

        if (answers.isEmpty()) {
            throw new InvalidFileException("No valid responses found in CSV file");
        }
        return answers;
    }

    private boolean isHeaderRow(String firstCell) {
        return firstCell.equalsIgnoreCase("questionId") ||
                firstCell.equalsIgnoreCase("questionNumber") ||
                firstCell.equalsIgnoreCase("question_id") ||
                firstCell.equalsIgnoreCase("q");
    }

    private Map<String, String> parseJson(MultipartFile file) {
        log.debug("Entering parseJson for {}", file.getOriginalFilename());
        Map<String, String> answers = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> items = objectMapper.readValue(file.getInputStream(), new TypeReference<>() {
            });

            for (Map<String, Object> item : items) {
                Object qId = getQuestionIdentifier(item);
                Object ans = getAnswerValue(item);

                if (qId != null && ans != null) {
                    String qHash = HashUtil.generateHash(qId.toString());
                    String answerStr = ans.toString().trim();
                    answers.put(qHash, answerStr);
                    log.debug("JSON entry: hash={}, answer={}", qHash, answerStr);
                }
            }
        } catch (IOException e) {
            log.error("Failed to parse JSON response", e);
            throw new InvalidFileException("Failed to parse JSON: " + e.getMessage());
        }

        if (answers.isEmpty()) {
            throw new InvalidFileException("No valid responses found in JSON file");
        }
        return answers;
    }

    private Object getQuestionIdentifier(Map<String, Object> item) {
        return item.getOrDefault("questionId",
                item.getOrDefault("questionNumber",
                        item.getOrDefault("question_id",
                                item.get("q"))));
    }

    private Object getAnswerValue(Map<String, Object> item) {
        return item.getOrDefault("answer",
                item.getOrDefault("selectedAnswer",
                        item.get("a")));
    }
}
