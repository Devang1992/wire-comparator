package com.wirecomparator.model;

/**
 * Represents a single wire transfer record parsed from an advisory file.
 * Either referenceForBeneficiary or referenceNumber is used as the match key,
 * with referenceForBeneficiary taking priority.
 */
public class WireRecord {

    private String referenceForBeneficiary;  // "Reference for Beneficiary:" field
    private String referenceNumber;           // "Reference Number:" field
    private String amount;
    private String beneficiary;
    private String accountNumber;
    private int pageNumber;                   // which page/record in the file
    private String rawMatchKey;               // normalized key actually used for matching

    public WireRecord() {}

    /**
     * Returns the normalized match key:
     * - Uses referenceForBeneficiary if present, otherwise referenceNumber.
     * - Normalized = trimmed, collapsed whitespace, uppercased.
     */
    public String getMatchKey() {
        String raw = (referenceForBeneficiary != null && !referenceForBeneficiary.isBlank())
                ? referenceForBeneficiary
                : referenceNumber;
        if (raw == null) return null;
        return raw.trim().replaceAll("\\s+", " ").toUpperCase();
    }

    public String getMatchKeySource() {
        return (referenceForBeneficiary != null && !referenceForBeneficiary.isBlank())
                ? "Reference for Beneficiary"
                : "Reference Number";
    }

    // --- Getters & Setters ---

    public String getReferenceForBeneficiary() { return referenceForBeneficiary; }
    public void setReferenceForBeneficiary(String v) { this.referenceForBeneficiary = v; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String v) { this.referenceNumber = v; }

    public String getAmount() { return amount; }
    public void setAmount(String v) { this.amount = v; }

    public String getBeneficiary() { return beneficiary; }
    public void setBeneficiary(String v) { this.beneficiary = v; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String v) { this.accountNumber = v; }

    public int getPageNumber() { return pageNumber; }
    public void setPageNumber(int v) { this.pageNumber = v; }

    public String getRawMatchKey() { return rawMatchKey; }
    public void setRawMatchKey(String v) { this.rawMatchKey = v; }

    @Override
    public String toString() {
        return "WireRecord{key='" + getMatchKey() + "', page=" + pageNumber + "}";
    }
}
