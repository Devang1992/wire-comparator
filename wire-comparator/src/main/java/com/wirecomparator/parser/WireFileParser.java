package com.wirecomparator.parser;

import com.wirecomparator.model.WireRecord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses wire transfer advisory text files (.ADV, .TST, .txt).
 *
 * Handles:
 * - Multiple records per file (split on "Page XX" markers or letter header blocks)
 * - Inconsistent spacing between label and value
 * - Either "Reference for Beneficiary" or "Reference Number" as match key
 */
public class WireFileParser {

    // Matches "Page 01", "Page 02", etc. — used to split multi-record files
    private static final Pattern PAGE_PATTERN = Pattern.compile("(?i)^\\s*Page\\s+\\d+\\s*$");

    // Field patterns — use flexible whitespace between label and value
    // Each pattern captures the value after the colon, trimmed
    private static final Pattern REF_FOR_BENEFICIARY = Pattern.compile(
            "(?i)Reference\\s+for\\s+Beneficiary\\s*:\\s*(.+)");
    private static final Pattern REF_NUMBER = Pattern.compile(
            "(?i)Reference\\s+Number\\s*:\\s*(.+)");
    private static final Pattern AMOUNT = Pattern.compile(
            "(?i)Amount\\s*:\\s*(.+)");
    private static final Pattern BENEFICIARY = Pattern.compile(
            "(?i)Beneficiary\\s*:\\s*(.+)");
    private static final Pattern ACCOUNT_NUMBER = Pattern.compile(
            "(?i)Account\\s+Number\\s*:\\s*(.+)");

    /**
     * Parse all records from a file.
     */
    public List<WireRecord> parse(Path filePath) throws IOException {
        List<String> lines = Files.readAllLines(filePath);
        List<List<String>> blocks = splitIntoBlocks(lines);

        List<WireRecord> records = new ArrayList<>();
        int pageNum = 1;
        for (List<String> block : blocks) {
            WireRecord record = parseBlock(block, pageNum++);
            // Only add if we got at least a match key
            if (record.getMatchKey() != null) {
                records.add(record);
            }
        }
        return records;
    }

    /**
     * Split file lines into per-record blocks.
     * Strategy: split on "Page XX" lines. If no page markers found,
     * treat the whole file as one record.
     */
    private List<List<String>> splitIntoBlocks(List<String> lines) {
        List<List<String>> blocks = new ArrayList<>();
        List<String> current = new ArrayList<>();
        boolean hasPageMarkers = lines.stream().anyMatch(l -> PAGE_PATTERN.matcher(l).matches());

        for (String line : lines) {
            if (hasPageMarkers && PAGE_PATTERN.matcher(line).matches()) {
                if (!current.isEmpty()) {
                    blocks.add(new ArrayList<>(current));
                    current.clear();
                }
                // Start next block — include this line so page number is trackable
            } else {
                current.add(line);
            }
        }
        if (!current.isEmpty()) {
            blocks.add(current);
        }

        // If no page markers and nothing was split, treat whole file as one block
        if (blocks.isEmpty() && !lines.isEmpty()) {
            blocks.add(lines);
        }

        return blocks;
    }

    /**
     * Parse a single block of lines into a WireRecord.
     * Multi-line field values (like Beneficiary spanning multiple lines) are
     * concatenated until the next labeled field is detected.
     */
    private WireRecord parseBlock(List<String> lines, int pageNum) {
        WireRecord record = new WireRecord();
        record.setPageNumber(pageNum);

        String currentField = null;
        StringBuilder currentValue = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            // Try to match a known field label
            String[] parsed = matchField(trimmed);
            if (parsed != null) {
                // Save previous field
                if (currentField != null) {
                    assignField(record, currentField, currentValue.toString().trim());
                }
                currentField = parsed[0];
                currentValue = new StringBuilder(parsed[1]);
            } else if (currentField != null) {
                // Continuation of a multi-line value (like long Beneficiary names)
                currentValue.append(" ").append(trimmed);
            }
        }

        // Save last field
        if (currentField != null) {
            assignField(record, currentField, currentValue.toString().trim());
        }

        return record;
    }

    /**
     * Try to match the line against known field patterns.
     * Returns [fieldName, value] or null if no match.
     */
    private String[] matchField(String line) {
        Matcher m;

        m = REF_FOR_BENEFICIARY.matcher(line);
        if (m.matches()) return new String[]{"REF_FOR_BENE", m.group(1).trim()};

        m = REF_NUMBER.matcher(line);
        if (m.matches()) return new String[]{"REF_NUMBER", m.group(1).trim()};

        m = AMOUNT.matcher(line);
        if (m.matches()) return new String[]{"AMOUNT", m.group(1).trim()};

        m = BENEFICIARY.matcher(line);
        if (m.matches()) return new String[]{"BENEFICIARY", m.group(1).trim()};

        m = ACCOUNT_NUMBER.matcher(line);
        if (m.matches()) return new String[]{"ACCOUNT_NUMBER", m.group(1).trim()};

        return null;
    }

    private void assignField(WireRecord record, String field, String value) {
        switch (field) {
            case "REF_FOR_BENE"   -> record.setReferenceForBeneficiary(value);
            case "REF_NUMBER"     -> record.setReferenceNumber(value);
            case "AMOUNT"         -> record.setAmount(value);
            case "BENEFICIARY"    -> record.setBeneficiary(value);
            case "ACCOUNT_NUMBER" -> record.setAccountNumber(value);
        }
    }
}
