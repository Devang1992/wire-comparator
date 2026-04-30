package com.wirecomparator.ui;

import com.wirecomparator.comparator.WireComparator;
import com.wirecomparator.exporter.ExcelExporter;
import com.wirecomparator.model.ComparisonResult;
import com.wirecomparator.model.WireRecord;
import com.wirecomparator.parser.WireFileParser;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class WireComparatorApp extends Application {

    private Label file1Label;
    private Label file2Label;
    private Button compareBtn;
    private Label statusLabel;
    private ProgressBar progressBar;
    private TextArea logArea;

    private File selectedFile1;
    private File selectedFile2;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Wire Transfer File Comparator");

        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #f4f6f9;");

        // --- Title ---
        Label title = new Label("Wire Transfer File Comparator");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        title.setTextFill(Color.web("#2c3e50"));

        Label subtitle = new Label("Upload two wire transfer files to compare records");
        subtitle.setTextFill(Color.web("#7f8c8d"));

        // --- File Selection Cards ---
        HBox fileCards = new HBox(20);
        fileCards.setAlignment(Pos.CENTER);

        VBox card1 = buildFileCard("File 1", "Select first wire file (.ADV, .TST, .txt)",
                e -> selectFile(primaryStage, 1));
        file1Label = (Label) ((VBox) card1).getChildren().get(2);

        VBox card2 = buildFileCard("File 2", "Select second wire file (.ADV, .TST, .txt)",
                e -> selectFile(primaryStage, 2));
        file2Label = (Label) ((VBox) card2).getChildren().get(2);

        fileCards.getChildren().addAll(card1, card2);

        // --- Compare Button ---
        compareBtn = new Button("⚡  Compare Files");
        compareBtn.setFont(Font.font("System", FontWeight.BOLD, 14));
        compareBtn.setStyle("""
                -fx-background-color: #2980b9;
                -fx-text-fill: white;
                -fx-padding: 12 32 12 32;
                -fx-background-radius: 6;
                -fx-cursor: hand;
                """);
        compareBtn.setDisable(true);
        compareBtn.setOnAction(e -> runComparison());
        compareBtn.setOnMouseEntered(e -> compareBtn.setStyle("""
                -fx-background-color: #1a6ea8;
                -fx-text-fill: white;
                -fx-padding: 12 32 12 32;
                -fx-background-radius: 6;
                -fx-cursor: hand;
                """));
        compareBtn.setOnMouseExited(e -> compareBtn.setStyle("""
                -fx-background-color: #2980b9;
                -fx-text-fill: white;
                -fx-padding: 12 32 12 32;
                -fx-background-radius: 6;
                -fx-cursor: hand;
                """));

        // --- Progress ---
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(400);
        progressBar.setVisible(false);

        statusLabel = new Label("");
        statusLabel.setTextFill(Color.web("#27ae60"));
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 13));

        // --- Log Area ---
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(180);
        logArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12;");
        logArea.setPromptText("Comparison log will appear here...");

        VBox.setVgrow(logArea, Priority.ALWAYS);

        // --- Legend ---
        HBox legend = buildLegend();

        root.getChildren().addAll(
                title, subtitle,
                new Separator(),
                fileCards,
                compareBtn,
                progressBar,
                statusLabel,
                new Separator(),
                new Label("Log:"),
                logArea,
                legend
        );

        Scene scene = new Scene(root, 900, 650);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(700);
        primaryStage.setMinHeight(550);
        primaryStage.show();

        log("Ready. Please select two wire transfer files to compare.");
    }

    private VBox buildFileCard(String title, String hint,
                               javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        card.setPrefWidth(350);
        card.setStyle("""
                -fx-background-color: white;
                -fx-background-radius: 8;
                -fx-border-color: #dde1e7;
                -fx-border-radius: 8;
                -fx-border-width: 1.5;
                """);

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        titleLabel.setTextFill(Color.web("#2c3e50"));

        Button selectBtn = new Button("📂  Browse...");
        selectBtn.setStyle("""
                -fx-background-color: #ecf0f1;
                -fx-text-fill: #2c3e50;
                -fx-padding: 8 20 8 20;
                -fx-background-radius: 5;
                -fx-cursor: hand;
                """);
        selectBtn.setOnAction(handler);

        Label fileNameLabel = new Label(hint);
        fileNameLabel.setTextFill(Color.web("#95a5a6"));
        fileNameLabel.setFont(Font.font("System", 11));
        fileNameLabel.setWrapText(true);
        fileNameLabel.setMaxWidth(300);

        card.getChildren().addAll(titleLabel, selectBtn, fileNameLabel);
        return card;
    }

    private HBox buildLegend() {
        HBox legend = new HBox(16);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.getChildren().add(new Label("Legend:"));
        legend.getChildren().add(legendItem("✓ Matched", "#27ae60"));
        legend.getChildren().add(legendItem("⚠ Different", "#f39c12"));
        legend.getChildren().add(legendItem("✗ Missing", "#e74c3c"));
        return legend;
    }

    private Label legendItem(String text, String color) {
        Label l = new Label(text);
        l.setTextFill(Color.web(color));
        l.setFont(Font.font("System", FontWeight.BOLD, 12));
        return l;
    }

    private void selectFile(Stage stage, int fileNum) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Wire Transfer File " + fileNum);
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Wire Files", "*.ADV", "*.TST", "*.txt", "*.adv", "*.tst"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            if (fileNum == 1) {
                selectedFile1 = file;
                file1Label.setText("✓ " + file.getName());
                file1Label.setTextFill(Color.web("#27ae60"));
            } else {
                selectedFile2 = file;
                file2Label.setText("✓ " + file.getName());
                file2Label.setTextFill(Color.web("#27ae60"));
            }
            updateCompareButton();
            log("File " + fileNum + " selected: " + file.getAbsolutePath());
        }
    }

    private void updateCompareButton() {
        compareBtn.setDisable(selectedFile1 == null || selectedFile2 == null);
    }

    private void runComparison() {
        compareBtn.setDisable(true);
        progressBar.setVisible(true);
        progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        statusLabel.setText("Parsing files...");
        statusLabel.setTextFill(Color.web("#2980b9"));

        Task<Path> task = new Task<>() {
            @Override
            protected Path call() throws Exception {
                WireFileParser parser = new WireFileParser();

                Platform.runLater(() -> log("Parsing File 1: " + selectedFile1.getName()));
                List<WireRecord> records1 = parser.parse(selectedFile1.toPath());
                Platform.runLater(() -> log("  → Found " + records1.size() + " record(s)"));

                Platform.runLater(() -> log("Parsing File 2: " + selectedFile2.getName()));
                List<WireRecord> records2 = parser.parse(selectedFile2.toPath());
                Platform.runLater(() -> log("  → Found " + records2.size() + " record(s)"));

                Platform.runLater(() -> log("Comparing records..."));
                WireComparator comparator = new WireComparator();
                List<ComparisonResult> results = comparator.compare(records1, records2);

                long matched   = results.stream().filter(r -> r.getStatus() == ComparisonResult.Status.MATCHED).count();
                long different = results.stream().filter(r -> r.getStatus() == ComparisonResult.Status.DIFFERENT).count();
                long onlyIn1   = results.stream().filter(r -> r.getStatus() == ComparisonResult.Status.ONLY_IN_FILE1).count();
                long onlyIn2   = results.stream().filter(r -> r.getStatus() == ComparisonResult.Status.ONLY_IN_FILE2).count();

                Platform.runLater(() -> {
                    log("Results: " + matched + " matched | " + different + " different | "
                            + onlyIn1 + " only in File 1 | " + onlyIn2 + " only in File 2");
                    log("Exporting to Excel...");
                });

                // Save next to File 1
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                Path outputPath = selectedFile1.toPath().getParent()
                        .resolve("wire_comparison_" + timestamp + ".xlsx");

                ExcelExporter exporter = new ExcelExporter();
                exporter.export(results, outputPath, selectedFile1.getName(), selectedFile2.getName());

                return outputPath;
            }
        };

        task.setOnSucceeded(e -> {
            Path output = task.getValue();
            progressBar.setVisible(false);
            compareBtn.setDisable(false);
            statusLabel.setText("✓ Done! Saved to: " + output.getFileName());
            statusLabel.setTextFill(Color.web("#27ae60"));
            log("✓ Excel saved: " + output.toAbsolutePath());

            // Offer to open the file
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Comparison Complete");
            alert.setHeaderText("Comparison finished successfully!");
            alert.setContentText("Output saved to:\n" + output.toAbsolutePath());
            alert.showAndWait();
        });

        task.setOnFailed(e -> {
            progressBar.setVisible(false);
            compareBtn.setDisable(false);
            statusLabel.setText("✗ Error: " + task.getException().getMessage());
            statusLabel.setTextFill(Color.web("#e74c3c"));
            log("ERROR: " + task.getException().getMessage());
            task.getException().printStackTrace();
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        logArea.appendText("[" + timestamp + "] " + message + "\n");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
