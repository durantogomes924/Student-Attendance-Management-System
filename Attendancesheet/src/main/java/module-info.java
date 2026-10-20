module com.example.attendancesheet {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.attendancesheet to javafx.fxml;
    exports com.example.attendancesheet;
}