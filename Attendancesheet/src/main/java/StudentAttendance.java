package com.example.attendancesheet;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class StudentAttendance {
    private final StringProperty studentId;
    private final StringProperty studentName;
    private final DoubleProperty attendancePercentage;

    public StudentAttendance(String studentId, String studentName, double percentage) {
        this.studentId = new SimpleStringProperty(studentId);
        this.studentName = new SimpleStringProperty(studentName);
        this.attendancePercentage = new SimpleDoubleProperty(percentage);
    }

    public String getStudentId() { return studentId.get(); }
    public StringProperty studentIdProperty() { return studentId; }

    public String getStudentName() { return studentName.get(); }
    public StringProperty studentNameProperty() { return studentName; }

    public double getAttendancePercentage() { return attendancePercentage.get(); }
    public DoubleProperty attendancePercentageProperty() { return attendancePercentage; }
}