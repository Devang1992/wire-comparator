# Wire Transfer File Comparator

A JavaFX desktop app that compares two wire transfer advisory files and exports differences to Excel.

---

## Requirements
- Java 17+ (JDK)
- Maven 3.8+

---

## Build

```bash
cd wire-comparator
mvn clean package
```

This produces a fat JAR at:
```
target/wire-comparator-1.0.0.jar
```

---

## Run

```bash
java -jar target/wire-comparator-1.0.0.jar
```

---

## Ship to End Users (Windows .exe)

Use `jpackage` (bundled with JDK 14+) to create a self-contained installer:

```bash
jpackage \
  --input target \
  --name "Wire Comparator" \
  --main-jar wire-comparator-1.0.0.jar \
  --main-class com.wirecomparator.ui.WireComparatorApp \
  --type exe \
  --win-shortcut \
  --win-menu
```

This bundles the JRE — users just double-click the installer, no Java needed.

---

## How It Works

1. User selects **File 1** and **File 2** (`.ADV`, `.TST`, `.txt`)
2. Parser extracts all wire records per file, splitting on `Page XX` markers
3. Each record is matched by:
   - **Reference for Beneficiary** (priority)
   - Falls back to **Reference Number**
   - Matching is case-insensitive with normalized whitespace
4. Results exported to Excel with:
   - **Green** = Matched (identical)
   - **Yellow** = Different (same key, values differ)
   - **Red** = Missing from one file
   - A **Summary** sheet with counts

---

## Project Structure

```
src/main/java/com/wirecomparator/
├── model/
│   ├── WireRecord.java          # One parsed wire transfer record
│   └── ComparisonResult.java    # Matched/different/missing pair
├── parser/
│   └── WireFileParser.java      # Parses multi-record .ADV/.TST files
├── comparator/
│   └── WireComparator.java      # Matches and diffs record lists
├── exporter/
│   └── ExcelExporter.java       # Apache POI Excel output
└── ui/
    └── WireComparatorApp.java   # JavaFX UI
```
