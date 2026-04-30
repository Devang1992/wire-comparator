package com.wirecomparator.model;

/**
 * Represents the comparison result between a record from File 1 and File 2.
 */
public class ComparisonResult {

    public enum Status {
        MATCHED,            // Same key, same values
        DIFFERENT,          // Same key, but field values differ
        ONLY_IN_FILE1,      // Key exists in file 1 but not file 2
        ONLY_IN_FILE2       // Key exists in file 2 but not file 1
    }

    private WireRecord file1Record;
    private WireRecord file2Record;
    private Status status;
    private String differenceDetail;   // human-readable description of what differs

    public ComparisonResult(WireRecord file1Record, WireRecord file2Record, Status status, String differenceDetail) {
        this.file1Record = file1Record;
        this.file2Record = file2Record;
        this.status = status;
        this.differenceDetail = differenceDetail;
    }

    // Convenience factories
    public static ComparisonResult onlyInFile1(WireRecord r) {
        return new ComparisonResult(r, null, Status.ONLY_IN_FILE1, "Record not found in File 2");
    }

    public static ComparisonResult onlyInFile2(WireRecord r) {
        return new ComparisonResult(null, r, Status.ONLY_IN_FILE2, "Record not found in File 1");
    }

    public static ComparisonResult matched(WireRecord r1, WireRecord r2) {
        return new ComparisonResult(r1, r2, Status.MATCHED, "");
    }

    public static ComparisonResult different(WireRecord r1, WireRecord r2, String detail) {
        return new ComparisonResult(r1, r2, Status.DIFFERENT, detail);
    }

    // --- Getters ---
    public WireRecord getFile1Record() { return file1Record; }
    public WireRecord getFile2Record() { return file2Record; }
    public Status getStatus() { return status; }
    public String getDifferenceDetail() { return differenceDetail; }

    public String getMatchKey() {
        if (file1Record != null) return file1Record.getMatchKey();
        if (file2Record != null) return file2Record.getMatchKey();
        return "";
    }
}
