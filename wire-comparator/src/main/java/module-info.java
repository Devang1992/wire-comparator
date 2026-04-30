module com.wirecomparator {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.poi.ooxml;
    requires org.slf4j;

    opens com.wirecomparator.ui to javafx.fxml;
    exports com.wirecomparator.ui;
}
