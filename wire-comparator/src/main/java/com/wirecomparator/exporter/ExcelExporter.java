package com.wirecomparator.exporter;

import com.wirecomparator.model.ComparisonResult;
import com.wirecomparator.model.WireRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Exports comparison results to a color-coded Excel file.
 *
 * Color coding:
 * - Green  = Matched (same record in both files)
 * - Yellow = Different (same key, values differ)
 * - Red    = Only in File 1 or Only in File 2
 */
public class ExcelExporter {

    public void export(List<ComparisonResult> results, Path outputPath) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Wire Comparison");

            // --- Styles ---
            CellStyle headerStyle  = createStyle(wb, IndexedColors.GREY_50_PERCENT, true);
            CellStyle matchedStyle = createStyle(wb, IndexedColors.LIGHT_GREEN, false);
            CellStyle diffStyle    = createStyle(wb, IndexedColors.LIGHT_YELLOW, false);
            CellStyle missingStyle = createStyle(wb, IndexedColors.ROSE, false);

            // --- Header Row ---
            String[] headers = {
                "Status",
                "Match Key Used",
                "File 1 – Ref for Beneficiary",
                "File 1 – Reference Number",
                "File 1 – Amount",
                "File 1 – Beneficiary",
                "File 1 – Account Number",
                "File 1 – Page",
                "File 2 – Ref for Beneficiary",
                "File 2 – Reference Number",
                "File 2 – Amount",
                "File 2 – Beneficiary",
                "File 2 – Account Number",
                "File 2 – Page",
                "Difference Detail"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // --- Data Rows ---
            int rowIdx = 1;
            for (ComparisonResult result : results) {
                Row row = sheet.createRow(rowIdx++);
                CellStyle style = styleFor(result.getStatus(), matchedStyle, diffStyle, missingStyle);

                WireRecord r1 = result.getFile1Record();
                WireRecord r2 = result.getFile2Record();

                setCell(row, 0, statusLabel(result.getStatus()), style);
                setCell(row, 1, result.getMatchKey(), style);

                // File 1 fields
                setCell(row, 2,  r1 != null ? r1.getReferenceForBeneficiary() : "", style);
                setCell(row, 3,  r1 != null ? r1.getReferenceNumber()         : "", style);
                setCell(row, 4,  r1 != null ? r1.getAmount()                  : "", style);
                setCell(row, 5,  r1 != null ? r1.getBeneficiary()             : "", style);
                setCell(row, 6,  r1 != null ? r1.getAccountNumber()           : "", style);
                setCell(row, 7,  r1 != null ? String.valueOf(r1.getPageNumber()) : "", style);

                // File 2 fields
                setCell(row, 8,  r2 != null ? r2.getReferenceForBeneficiary() : "", style);
                setCell(row, 9,  r2 != null ? r2.getReferenceNumber()         : "", style);
                setCell(row, 10, r2 != null ? r2.getAmount()                  : "", style);
                setCell(row, 11, r2 != null ? r2.getBeneficiary()             : "", style);
                setCell(row, 12, r2 != null ? r2.getAccountNumber()           : "", style);
                setCell(row, 13, r2 != null ? String.valueOf(r2.getPageNumber()) : "", style);

                setCell(row, 14, result.getDifferenceDetail(), style);
            }

            // --- Summary Sheet ---
            Sheet summary = wb.createSheet("Summary");
            long matched   = results.stream().filter(r -> r.getStatus() == ComparisonResult.Status.MATCHED).count();
            long different = results.stream().filter(r -> r.getStatus() == ComparisonResult.Status.DIFFERENT).count();
            long onlyIn1   = results.stream().filter(r -> r.getStatus() == ComparisonResult.Status.ONLY_IN_FILE1).count();
            long onlyIn2   = results.stream().filter(r -> r.getStatus() == ComparisonResult.Status.ONLY_IN_FILE2).count();

            CellStyle sumHeader = createStyle(wb, IndexedColors.GREY_50_PERCENT, true);
            addSummaryRow(summary, 0, "Metric", "Count", sumHeader);
            addSummaryRow(summary, 1, "Total Records Compared", String.valueOf(results.size()), null);
            addSummaryRow(summary, 2, "Matched (identical)",    String.valueOf(matched),   null);
            addSummaryRow(summary, 3, "Different (same key, values differ)", String.valueOf(different), null);
            addSummaryRow(summary, 4, "Only in File 1 (missing from File 2)", String.valueOf(onlyIn1), null);
            addSummaryRow(summary, 5, "Only in File 2 (missing from File 1)", String.valueOf(onlyIn2), null);

            // Auto-size columns on main sheet
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
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
            case MATCHED      -> matched;
            case DIFFERENT    -> diff;
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
