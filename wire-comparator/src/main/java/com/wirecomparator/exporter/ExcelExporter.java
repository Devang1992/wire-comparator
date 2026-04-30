package com.wirecomparator.exporter;

import com.wirecomparator.model.ComparisonResult;
import com.wirecomparator.model.WireRecord;
import com.wirecomparator.parser.DateExtractor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ExcelExporter {

    public void export(List<ComparisonResult> results, Path outputPath,
                       String file1Name, String file2Name) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Wire Comparison");

            CellStyle headerStyle  = createStyle(wb, IndexedColors.GREY_50_PERCENT, true);
            CellStyle matchedStyle = createStyle(wb, IndexedColors.LIGHT_GREEN, false);
            CellStyle diffStyle    = createStyle(wb, IndexedColors.LIGHT_YELLOW, false);
            CellStyle missingStyle = createStyle(wb, IndexedColors.ROSE, false);

            String[] headers = {
                "Status", "Match Key Used",
                "File 1 – Ref for Beneficiary", "File 1 – Reference Number",
                "File 1 – Amount", "File 1 – Beneficiary", "File 1 – Account Number", "File 1 – Page",
                "File 2 – Ref for Beneficiary", "File 2 – Reference Number",
                "File 2 – Amount", "File 2 – Beneficiary", "File 2 – Account Number", "File 2 – Page",
                "Difference Detail"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (ComparisonResult result : results) {
                Row row = sheet.createRow(rowIdx++);
                CellStyle style = styleFor(result.getStatus(), matchedStyle, diffStyle, missingStyle);
                WireRecord r1 = result.getFile1Record();
                WireRecord r2 = result.getFile2Record();

                setCell(row, 0, statusLabel(result.getStatus()), style);
                setCell(row, 1, result.getMatchKey(), style);
                setCell(row, 2,  r1 != null ? r1.getReferenceForBeneficiary()    : "", style);
                setCell(row, 3,  r1 != null ? r1.getReferenceNumber()            : "", style);
                setCell(row, 4,  r1 != null ? r1.getAmount()                     : "", style);
                setCell(row, 5,  r1 != null ? r1.getBeneficiary()                : "", style);
                setCell(row, 6,  r1 != null ? r1.getAccountNumber()              : "", style);
                setCell(row, 7,  r1 != null ? String.valueOf(r1.getPageNumber()) : "", style);
                setCell(row, 8,  r2 != null ? r2.getReferenceForBeneficiary()    : "", style);
                setCell(row, 9,  r2 != null ? r2.getReferenceNumber()            : "", style);
                setCell(row, 10, r2 != null ? r2.getAmount()                     : "", style);
                setCell(row, 11, r2 != null ? r2.getBeneficiary()                : "", style);
                setCell(row, 12, r2 != null ? r2.getAccountNumber()              : "", style);
                setCell(row, 13, r2 != null ? String.valueOf(r2.getPageNumber()) : "", style);
                setCell(row, 14, result.getDifferenceDetail(), style);
            }

            // --- Summary Sheet ---
            Sheet summary = wb.createSheet("Summary");
            long matched   = results.stream().filter(r -> r.getStatus() == ComparisonResult.Status.MATCHED).count();
            long different = results.stream().filter(r -> r.getStatus() == ComparisonResult.Status.DIFFERENT).count();
            long onlyIn1   = results.stream().filter(r -> r.getStatus() == ComparisonResult.Status.ONLY_IN_FILE1).count();
            long onlyIn2   = results.stream().filter(r -> r.getStatus() == ComparisonResult.Status.ONLY_IN_FILE2).count();

            CellStyle sumHeader  = createStyle(wb, IndexedColors.GREY_50_PERCENT, true);
            CellStyle sumSection = createStyle(wb, IndexedColors.PALE_BLUE, true);

            int sumRow = 0;

            // Section 1: overall counts
            addSummaryRow(summary, sumRow++, "Metric", "Count", sumHeader);
            addSummaryRow(summary, sumRow++, "Total Records Compared",               String.valueOf(results.size()), null);
            addSummaryRow(summary, sumRow++, "Matched (identical)",                  String.valueOf(matched),        null);
            addSummaryRow(summary, sumRow++, "Different (same key, values differ)",  String.valueOf(different),      null);
            addSummaryRow(summary, sumRow++, "Only in File 1 (missing from File 2)", String.valueOf(onlyIn1),        null);
            addSummaryRow(summary, sumRow++, "Only in File 2 (missing from File 1)", String.valueOf(onlyIn2),        null);
            sumRow++;

            // Section 2: per-file record counts with actual file names
            long file1Count = results.stream().filter(r -> r.getFile1Record() != null).count();
            long file2Count = results.stream().filter(r -> r.getFile2Record() != null).count();
            addSummaryRow(summary, sumRow++, "File", "Record Count", sumSection);
            addSummaryRow(summary, sumRow++, "File 1: " + file1Name, String.valueOf(file1Count), null);
            addSummaryRow(summary, sumRow++, "File 2: " + file2Name, String.valueOf(file2Count), null);
            sumRow++;

            // Section 3: combined date breakdown (deduplicated — matched records counted once)
            addSummaryRow(summary, sumRow++, "Date (from Reference Number)", "MID Count", sumSection);
            Map<String, Long> dateCountMap = new TreeMap<>();
            results.stream()
                    .filter(r -> r.getFile1Record() != null)
                    .map(r -> r.getFile1Record().getReferenceNumber())
                    .filter(ref -> ref != null && !ref.isBlank())
                    .forEach(ref -> dateCountMap.merge(DateExtractor.extractDate(ref), 1L, Long::sum));
            results.stream()
                    .filter(r -> r.getStatus() == ComparisonResult.Status.ONLY_IN_FILE2)
                    .filter(r -> r.getFile2Record() != null)
                    .map(r -> r.getFile2Record().getReferenceNumber())
                    .filter(ref -> ref != null && !ref.isBlank())
                    .forEach(ref -> dateCountMap.merge(DateExtractor.extractDate(ref), 1L, Long::sum));

            if (dateCountMap.isEmpty()) {
                addSummaryRow(summary, sumRow++, "No reference numbers found", "", null);
            } else {
                for (Map.Entry<String, Long> e : dateCountMap.entrySet())
                    addSummaryRow(summary, sumRow++, e.getKey(), String.valueOf(e.getValue()), null);
                long total = dateCountMap.values().stream().mapToLong(Long::longValue).sum();
                addSummaryRow(summary, sumRow++, "Total MIDs", String.valueOf(total), sumHeader);
            }
            sumRow++;

            // Section 4: per-file date breakdown
            addSummaryRow(summary, sumRow++, "Date Breakdown – File 1: " + file1Name, "Count", sumSection);
            Map<String, Long> file1Dates = results.stream()
                    .filter(r -> r.getFile1Record() != null)
                    .map(r -> r.getFile1Record().getReferenceNumber())
                    .filter(ref -> ref != null && !ref.isBlank())
                    .collect(Collectors.groupingBy(DateExtractor::extractDate, TreeMap::new, Collectors.counting()));
            if (file1Dates.isEmpty()) addSummaryRow(summary, sumRow++, "No reference numbers found", "", null);
            else for (Map.Entry<String, Long> e : file1Dates.entrySet())
                addSummaryRow(summary, sumRow++, e.getKey(), String.valueOf(e.getValue()), null);
            sumRow++;

            addSummaryRow(summary, sumRow++, "Date Breakdown – File 2: " + file2Name, "Count", sumSection);
            Map<String, Long> file2Dates = results.stream()
                    .filter(r -> r.getFile2Record() != null)
                    .map(r -> r.getFile2Record().getReferenceNumber())
                    .filter(ref -> ref != null && !ref.isBlank())
                    .collect(Collectors.groupingBy(DateExtractor::extractDate, TreeMap::new, Collectors.counting()));
            if (file2Dates.isEmpty()) addSummaryRow(summary, sumRow++, "No reference numbers found", "", null);
            else for (Map.Entry<String, Long> e : file2Dates.entrySet())
                addSummaryRow(summary, sumRow++, e.getKey(), String.valueOf(e.getValue()), null);

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            summary.autoSizeColumn(0);
            summary.autoSizeColumn(1);

            try (FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
                wb.write(fos);
            }
        }
    }

    private CellStyle styleFor(ComparisonResult.Status status,
                                CellStyle matched, CellStyle diff, CellStyle missing) {
        return switch (status) {
            case MATCHED                    -> matched;
            case DIFFERENT                  -> diff;
            case ONLY_IN_FILE1, ONLY_IN_FILE2 -> missing;
        };
    }

    private String statusLabel(ComparisonResult.Status status) {
        return switch (status) {
            case MATCHED       -> "✓ Matched";
            case DIFFERENT     -> "⚠ Different";
            case ONLY_IN_FILE1 -> "✗ Only in File 1";
            case ONLY_IN_FILE2 -> "✗ Only in File 2";
        };
    }

    private CellStyle createStyle(Workbook wb, IndexedColors color, boolean bold) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        if (bold) {
            Font font = wb.createFont();
            font.setBold(true);
            style.setFont(font);
        }
        return style;
    }

    private void setCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        if (style != null) cell.setCellStyle(style);
    }

    private void addSummaryRow(Sheet sheet, int rowIdx, String label, String value, CellStyle style) {
        Row row = sheet.createRow(rowIdx);
        Cell c0 = row.createCell(0);
        Cell c1 = row.createCell(1);
        c0.setCellValue(label);
        c1.setCellValue(value);
        if (style != null) { c0.setCellStyle(style); c1.setCellStyle(style); }
    }
}
