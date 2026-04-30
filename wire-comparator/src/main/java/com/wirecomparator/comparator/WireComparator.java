package com.wirecomparator.comparator;

import com.wirecomparator.model.ComparisonResult;
import com.wirecomparator.model.WireRecord;

import java.util.*;

/**
 * Compares two lists of WireRecords and produces ComparisonResults.
 *
 * Matching strategy:
 * - Match key = normalized (trimmed, collapsed whitespace, uppercased) value of
 *   "Reference for Beneficiary" if present, else "Reference Number"
 * - Records with same key are compared field by field
 * - Records with no matching key in the other file are flagged as missing
 */
public class WireComparator {

    public List<ComparisonResult> compare(List<WireRecord> file1Records, List<WireRecord> file2Records) {
        List<ComparisonResult> results = new ArrayList<>();

        // Index file2 records by their match key for O(1) lookup
        Map<String, WireRecord> file2Index = new LinkedHashMap<>();
        for (WireRecord r : file2Records) {
            String key = r.getMatchKey();
            if (key != null) {
                file2Index.put(key, r);
            }
        }

        // Track which file2 keys were matched
        Set<String> matchedFile2Keys = new HashSet<>();

        // Go through file1 records
        for (WireRecord r1 : file1Records) {
            String key = r1.getMatchKey();
            if (key == null) continue;

            WireRecord r2 = file2Index.get(key);
            if (r2 == null) {
                results.add(ComparisonResult.onlyInFile1(r1));
            } else {
                matchedFile2Keys.add(key);
                String diff = buildDiffDetail(r1, r2);
                if (diff.isEmpty()) {
                    results.add(ComparisonResult.matched(r1, r2));
                } else {
                    results.add(ComparisonResult.different(r1, r2, diff));
                }
            }
        }

        // File2 records that had no match in file1
        for (WireRecord r2 : file2Records) {
            String key = r2.getMatchKey();
            if (key != null && !matchedFile2Keys.contains(key)) {
                results.add(ComparisonResult.onlyInFile2(r2));
            }
        }

        // Sort: differences and missing first, matched last
        results.sort(Comparator.comparing(r -> r.getStatus().ordinal()));

        return results;
    }

    /**
     * Compares fields between two matched records.
     * Returns empty string if identical, otherwise a human-readable diff summary.
     */
    private String buildDiffDetail(WireRecord r1, WireRecord r2) {
        List<String> diffs = new ArrayList<>();

        checkField(diffs, "Amount", r1.getAmount(), r2.getAmount());
        checkField(diffs, "Beneficiary", r1.getBeneficiary(), r2.getBeneficiary());
        checkField(diffs, "Account Number", r1.getAccountNumber(), r2.getAccountNumber());
        checkField(diffs, "Reference for Beneficiary",
                r1.getReferenceForBeneficiary(), r2.getReferenceForBeneficiary());
        checkField(diffs, "Reference Number", r1.getReferenceNumber(), r2.getReferenceNumber());

        return String.join("; ", diffs);
    }

    private void checkField(List<String> diffs, String fieldName, String v1, String v2) {
        String n1 = normalize(v1);
        String n2 = normalize(v2);
        if (!Objects.equals(n1, n2)) {
            diffs.add(fieldName + " ['" + v1 + "' vs '" + v2 + "']");
        }
    }

    private String normalize(String v) {
        if (v == null) return "";
        return v.trim().replaceAll("\\s+", " ").toUpperCase();
    }
}
