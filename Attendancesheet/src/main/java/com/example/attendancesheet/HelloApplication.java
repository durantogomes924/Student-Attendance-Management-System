package com.example.attendancesheet;

import javafx.application.Application;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class HelloApplication extends Application {

    public static class StudentAttendance {
        private final StringProperty studentId;
        private final StringProperty studentName;
        private final Map<Integer, IntegerProperty> weekMap = new HashMap<>();
        private final DoubleProperty attendancePercentage;
        private final int totalWeeks;

        public StudentAttendance(String studentId, String studentName, int totalWeeks) {
            this.studentId = new SimpleStringProperty(studentId);
            this.studentName = new SimpleStringProperty(studentName);
            this.totalWeeks = totalWeeks;
            this.attendancePercentage = new SimpleDoubleProperty(0.0);

            javafx.beans.value.ChangeListener<Number> listener = (obs, oldVal, newVal) -> calculatePercentage();
            for (int i = 1; i <= totalWeeks; i++) {
                IntegerProperty prop = new SimpleIntegerProperty(0);
                prop.addListener(listener);
                weekMap.put(i, prop);
            }
        }

        private void calculatePercentage() {
            int attended = 0;
            for (int i = 1; i <= totalWeeks; i++) {
                if (weekMap.get(i).get() == 1) attended++;
            }
            attendancePercentage.set((attended / (double) totalWeeks) * 100.0);
        }

        public String getStudentId() { return studentId.get(); }
        public void setStudentId(String id) { this.studentId.set(id); }
        public StringProperty studentIdProperty() { return studentId; }

        public String getStudentName() { return studentName.get(); }
        public void setStudentName(String name) { this.studentName.set(name); }
        public StringProperty studentNameProperty() { return studentName; }

        public IntegerProperty weekProperty(int week) { return weekMap.get(week); }

        public double getAttendancePercentage() { return attendancePercentage.get(); }
        public DoubleProperty attendancePercentageProperty() { return attendancePercentage; }
    }

    private static class CopiedStudentData {
        String id;
        String name;
        Map<Integer, Integer> weekStates;

        CopiedStudentData(String id, String name, Map<Integer, Integer> weekStates) {
            this.id = id;
            this.name = name;
            this.weekStates = weekStates;
        }
    }
    private static List<CopiedStudentData> clipboardData = null;

    private final ObservableList<String> subjects = FXCollections.observableArrayList(
            "Comprehensive Design of Data Structures"
    );
    private final Map<String, String> subjectTeacherMap = new HashMap<>();
    private final Map<String, ObservableList<StudentAttendance>> subjectDataMap = new HashMap<>();
    private final Map<String, FilteredList<StudentAttendance>> subjectFilteredMap = new HashMap<>();

    private ListView<String> classList;
    private Label sidebarTitle;
    private TextField subjectSearchField;
    private TextField newSubjectField;
    private TextField teacherNameField;
    private Button addSubjectBtn;
    private Label enrollLabel;
    private TextField idField;
    private TextField nameField;
    private Button addStudentBtn;
    private TextField searchField;
    private Button modeToggleBtn;
    private Button translateBtn;
    private Label middleSubjectHeader;
    private Label middleTeacherHeader;
    private Button copyAllBtn;
    private Button pasteAllBtn;
    private VBox centerContainer;

    private FilteredList<String> filteredSubjects;
    private boolean isDarkMode = false;
    private boolean isChinese = false;
    private String customAccentColor = "#3A5A40";

    // Configured to 10 weeks here:
    private int totalConfiguredWeeks = 10;

    private String globalFontSize = "13px";
    private String customFontColor = "#111827";

    private BorderPane root;
    private VBox sidebar;
    private HBox topBar;
    private TableView<StudentAttendance> table;
    private Stage mainStage;

    private String tr(String en, String zh) {
        return isChinese ? zh : en;
    }

    @Override
    public void start(Stage stage) {
        this.mainStage = stage;
        root = new BorderPane();
        applyTheme(root, false);

        Scale scaleTransform = new Scale(1.0, 1.0, 0, 0);
        root.getTransforms().add(scaleTransform);

        subjectTeacherMap.put("Comprehensive Design of Data Structures", "Prof. Lord");
        for (String subject : subjects) {
            ObservableList<StudentAttendance> dataList = FXCollections.observableArrayList();
            subjectDataMap.put(subject, dataList);
            subjectFilteredMap.put(subject, new FilteredList<>(dataList, p -> true));
        }

        filteredSubjects = new FilteredList<>(subjects, s -> true);

        // --- LEFT SIDEBAR ---
        sidebar = new VBox(10);
        sidebar.setPadding(new Insets(20, 16, 20, 16));
        updateSidebarStyle(sidebar, false);

        sidebarTitle = new Label(tr("COURSE MODULES", "课程模块"));
        sidebarTitle.setStyle("-fx-text-fill: " + (isDarkMode ? "#ffffff" : customFontColor) + "; -fx-font-weight: bold; -fx-font-size: 11px; -fx-letter-spacing: 1.5px;");

        subjectSearchField = new TextField();
        subjectSearchField.setPromptText(tr("🔍 Filter subjects...", "🔍 筛选科目..."));
        updateTextFieldStyle(subjectSearchField, false);
        subjectSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredSubjects.setPredicate(subject -> {
                if (newValue == null || newValue.isEmpty()) return true;
                return subject.toLowerCase().contains(newValue.toLowerCase());
            });
        });

        classList = new ListView<>(filteredSubjects);
        updateListStyle(classList, false);

        ContextMenu subjectContextMenu = new ContextMenu();
        MenuItem editSubjectItem = new MenuItem(tr("✏ Rename Subject", "✏ 重命名科目"));
        MenuItem deleteSubjectItem = new MenuItem(tr("🗑 Delete Subject", "🗑 删除科目"));
        subjectContextMenu.getItems().addAll(editSubjectItem, deleteSubjectItem);

        classList.setCellFactory(lv -> {
            ListCell<String> cell = new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("-fx-background-color: transparent;");
                    } else {
                        if (isChinese && item.equals("Comprehensive Design of Data Structures")) {
                            setText("数据结构综合课程设计");
                        } else if (!isChinese && item.equals("数据结构综合课程设计")) {
                            setText("Comprehensive Design of Data Structures");
                        } else {
                            setText(item);
                        }
                        String textColor = isDarkMode ? "#ffffff" : customFontColor;
                        setStyle("-fx-text-fill: " + textColor + "; -fx-padding: 10px 12px; -fx-font-size: " + globalFontSize + "; -fx-font-weight: 500; -fx-background-color: transparent;");
                    }
                }
            };
            cell.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(cell.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(subjectContextMenu)
            );
            return cell;
        });

        editSubjectItem.setOnAction(e -> {
            String selectedSubject = classList.getSelectionModel().getSelectedItem();
            if (selectedSubject != null) editSubject(selectedSubject);
        });

        deleteSubjectItem.setOnAction(e -> {
            String selectedSubject = classList.getSelectionModel().getSelectedItem();
            if (selectedSubject != null) {
                if (subjects.size() <= 1) {
                    showAlert(tr("You must keep at least one active subject.", "您必须保留至少一个活跃科目。"));
                    return;
                }
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, tr("Delete subject '", "删除科目 '") + selectedSubject + "'?", ButtonType.YES, ButtonType.NO);
                confirm.showAndWait();
                if (confirm.getResult() == ButtonType.YES) {
                    subjects.remove(selectedSubject);
                    subjectDataMap.remove(selectedSubject);
                    subjectFilteredMap.remove(selectedSubject);
                    subjectTeacherMap.remove(selectedSubject);
                    if (!subjects.isEmpty()) classList.getSelectionModel().select(0);
                }
            }
        });

        newSubjectField = new TextField();
        newSubjectField.setPromptText(tr("New subject name...", "新科目名称..."));
        updateTextFieldStyle(newSubjectField, false);

        teacherNameField = new TextField();
        teacherNameField.setPromptText(tr("Instructor / Teacher name...", "授课教师姓名..."));
        updateTextFieldStyle(teacherNameField, false);

        addSubjectBtn = new Button(tr("＋ Create Subject", "＋ 创建科目"));
        updatePrimaryButtonStyle(addSubjectBtn, false);
        addSubjectBtn.setOnAction(e -> {
            String subjectName = newSubjectField.getText().trim();
            String teacherName = teacherNameField.getText().trim();
            if (!subjectName.isEmpty() && !teacherName.isEmpty() && !subjects.contains(subjectName)) {
                subjects.add(subjectName);
                subjectTeacherMap.put(subjectName, teacherName);
                subjectDataMap.put(subjectName, FXCollections.observableArrayList());
                subjectFilteredMap.put(subjectName, new FilteredList<>(subjectDataMap.get(subjectName), p -> true));
                newSubjectField.clear();
                teacherNameField.clear();
                classList.getSelectionModel().select(subjectName);
            } else {
                showAlert(tr("Please enter both Subject Name and Teacher Name.", "请输入科目名称和教师姓名。"));
            }
        });

        sidebar.getChildren().addAll(sidebarTitle, subjectSearchField, classList, newSubjectField, teacherNameField, addSubjectBtn);
        root.setLeft(sidebar);

        // --- TOP TOOLBAR ---
        topBar = new HBox(12);
        updateTopBarStyle(topBar, false);
        topBar.setPadding(new Insets(14, 20, 14, 20));
        topBar.setAlignment(Pos.CENTER_LEFT);

        enrollLabel = new Label(tr("ENROLL:", "注册:"));
        enrollLabel.setStyle("-fx-text-fill: " + (isDarkMode ? "#ffffff" : customFontColor) + "; -fx-font-weight: bold; -fx-font-size: 11px;");

        idField = new TextField();
        idField.setPromptText(tr("Student ID", "学生编号"));
        updateTextFieldStyle(idField, false);
        idField.setPrefWidth(115);

        nameField = new TextField();
        nameField.setPromptText(tr("Student Full Name", "学生全名"));
        updateTextFieldStyle(nameField, false);
        nameField.setPrefWidth(180);

        addStudentBtn = new Button(tr("＋ Add Student", "＋ 添加学生"));
        addStudentBtn.setStyle("-fx-background-color: " + customAccentColor + "; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 8px 12px; -fx-cursor: hand;");

        Separator separator = new Separator(javafx.geometry.Orientation.VERTICAL);
        separator.setPrefHeight(26);

        searchField = new TextField();
        searchField.setPromptText(tr("🔍 Quick search record...", "🔍 快速搜索记录..."));
        updateTextFieldStyle(searchField, false);
        searchField.setPrefWidth(160);

        Separator zoomSeparator = new Separator(javafx.geometry.Orientation.VERTICAL);
        zoomSeparator.setPrefHeight(26);

        Button zoomOutBtn = new Button("🔍-");
        updateToolButtonStyle(zoomOutBtn, false);
        Button zoomResetBtn = new Button("100%");
        updateToolButtonStyle(zoomResetBtn, false);
        Button zoomInBtn = new Button("🔍+");
        updateToolButtonStyle(zoomInBtn, false);

        Separator modeSeparator = new Separator(javafx.geometry.Orientation.VERTICAL);
        modeSeparator.setPrefHeight(26);

        modeToggleBtn = new Button(isDarkMode ? tr("☀️ Light Mode", "☀️ 浅色模式") : tr("🌙 Dark Mode", "🌙 深色模式"));
        updateToolButtonStyle(modeToggleBtn, false);

        Separator langSeparator = new Separator(javafx.geometry.Orientation.VERTICAL);
        langSeparator.setPrefHeight(26);

        translateBtn = new Button(isChinese ? "🌐 English" : "🌐 中文");
        updateToolButtonStyle(translateBtn, false);

        zoomOutBtn.setOnAction(e -> {
            double currentScale = scaleTransform.getX();
            if (currentScale > 0.05) {
                double newScale = Math.max(0.0, currentScale - 0.1);
                scaleTransform.setX(newScale);
                scaleTransform.setY(newScale);
                zoomResetBtn.setText(Math.round(newScale * 100) + "%");
            }
        });

        zoomResetBtn.setOnAction(e -> {
            scaleTransform.setX(1.0);
            scaleTransform.setY(1.0);
            zoomResetBtn.setText("100%");
        });

        zoomInBtn.setOnAction(e -> {
            double currentScale = scaleTransform.getX();
            if (currentScale < 2.0) {
                double newScale = Math.min(2.0, currentScale + 0.1);
                scaleTransform.setX(newScale);
                scaleTransform.setY(newScale);
                zoomResetBtn.setText(Math.round(newScale * 100) + "%");
            }
        });

        modeToggleBtn.setOnAction(e -> {
            isDarkMode = !isDarkMode;
            modeToggleBtn.setText(isDarkMode ? tr("☀️ Light Mode", "☀️ 浅色模式") : tr("🌙 Dark Mode", "🌙 深色模式"));
            refreshUITheme();
        });
        translateBtn.setOnAction(e -> {
            isChinese = !isChinese;
            translateBtn.setText(isChinese ? "🌐 English" : "🌐 中文");
            refreshLanguageUI();
        });

        topBar.getChildren().addAll(enrollLabel, idField, nameField, addStudentBtn, separator, searchField, zoomSeparator, zoomOutBtn, zoomResetBtn, zoomInBtn, modeSeparator, modeToggleBtn, langSeparator, translateBtn);
        root.setTop(topBar);

        // --- CENTER TABLE SECTION ---
        centerContainer = new VBox(10);
        centerContainer.setPadding(new Insets(24));
        updateCenterContainerStyle(centerContainer, false);

        middleSubjectHeader = new Label("Comprehensive Design of Data Structures");
        middleSubjectHeader.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + (isDarkMode ? "#ffffff" : customFontColor) + ";");

        middleTeacherHeader = new Label(tr("Instructor: ", "讲师: ") + "Prof. Alexander Wright");
        middleTeacherHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: " + (isDarkMode ? "#9ca3af" : "#334155") + ";");

        table = new TableView<>();
        table.setEditable(true);
        updateTableStyle(table, false);
        table.setPrefHeight(430);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Label placeholderLabel = new Label(tr("No content in table", "表格中暂无内容"));
        placeholderLabel.setStyle("-fx-text-fill: " + (isDarkMode ? "#9ca3af" : "#334155") + "; -fx-font-size: " + globalFontSize + ";");
        table.setPlaceholder(placeholderLabel);

        rebuildTableColumns();

        ContextMenu studentContextMenu = new ContextMenu();
        MenuItem editStudentItem = new MenuItem(tr("✏ Edit Student Info", "✏ 编辑学生信息"));
        MenuItem deleteStudentItem = new MenuItem(tr("🗑 Remove Student", "🗑 移除学生"));
        studentContextMenu.getItems().addAll(editStudentItem, deleteStudentItem);

        table.setRowFactory(tv -> {
            TableRow<StudentAttendance> row = new TableRow<>();
            row.contextMenuProperty().bind(javafx.beans.binding.Bindings.when(row.emptyProperty()).then((ContextMenu) null).otherwise(studentContextMenu));
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 3 && !row.isEmpty()) {
                    StudentAttendance student = row.getItem();
                    if (student != null) {
                        showStudentStatisticsInSidebar(student);
                    }
                }
            });
            return row;
        });

        editStudentItem.setOnAction(e -> {
            StudentAttendance selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) editStudent(selected);
        });

        deleteStudentItem.setOnAction(e -> {
            StudentAttendance selected = table.getSelectionModel().getSelectedItem();
            String currentSubj = classList.getSelectionModel().getSelectedItem();
            if (selected != null && currentSubj != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, tr("Remove ", "移除 ") + selected.getStudentName() + "?", ButtonType.YES, ButtonType.NO);
                confirm.showAndWait();
                if (confirm.getResult() == ButtonType.YES) subjectDataMap.get(currentSubj).remove(selected);
            }
        });

        // --- IMPORT / EXPORT FILE MENUBUTTONS & COPY/PASTE CONTAINER (Bottom Right) ---
        HBox fileButtonBox = new HBox(10);
        fileButtonBox.setAlignment(Pos.CENTER_RIGHT);

        MenuButton importFileBtn = new MenuButton(tr("📂 Import File", "📂 导入文件"));
        updateMenuButtonStyle(importFileBtn, isDarkMode);
        MenuItem impWord = new MenuItem(tr("Word Document (.docx / .doc / .txt)", "Word 文档 (.docx / .doc / .txt)"));
        MenuItem impExcel = new MenuItem(tr("Excel Spreadsheet (.xlsx / .xls / .csv)", "Excel 表格 (.xlsx / .xls / .csv)"));
        MenuItem impCsv = new MenuItem(tr("CSV File (.csv)", "CSV 文件 (.csv)"));
        MenuItem impPdf = new MenuItem(tr("PDF Document (.pdf)", "PDF 文档 (.pdf)"));
        importFileBtn.getItems().addAll(impWord, impExcel, impCsv, impPdf);

        MenuButton exportFileBtn = new MenuButton(tr("💾 Export File", "💾 导出文件"));
        updateMenuButtonStyle(exportFileBtn, isDarkMode);
        MenuItem expWord = new MenuItem(tr("Word Document (.docx / .doc)", "Word 文档 (.docx / .doc)"));
        MenuItem expExcel = new MenuItem(tr("Excel Spreadsheet (.xlsx / .xls)", "Excel 表格 (.xlsx / .xls)"));
        MenuItem expCsv = new MenuItem(tr("CSV File (.csv)", "CSV 文件 (.csv)"));
        MenuItem expPdf = new MenuItem(tr("PDF Document (.pdf)", "PDF 文档 (.pdf)"));
        exportFileBtn.getItems().addAll(expWord, expExcel, expCsv, expPdf);

        impWord.setOnAction(e -> importDataFromFile("Word"));
        impExcel.setOnAction(e -> importDataFromFile("Excel"));
        impCsv.setOnAction(e -> importDataFromFile("CSV"));
        impPdf.setOnAction(e -> importDataFromFile("PDF"));

        expWord.setOnAction(e -> exportDataToFile("Word"));
        expExcel.setOnAction(e -> exportDataToFile("Excel"));
        expCsv.setOnAction(e -> exportDataToFile("CSV"));
        expPdf.setOnAction(e -> exportDataToFile("PDF"));

        copyAllBtn = new Button(tr("📋 Copy All", "📋 复制全部"));
        updateToolButtonStyle(copyAllBtn, isDarkMode);
        copyAllBtn.setOnAction(e -> handleCopyAll());

        pasteAllBtn = new Button(tr("📋 Paste All", "📋 粘贴全部"));
        updateToolButtonStyle(pasteAllBtn, isDarkMode);
        pasteAllBtn.setOnAction(e -> handlePasteAll());

        fileButtonBox.getChildren().addAll(importFileBtn, exportFileBtn, copyAllBtn, pasteAllBtn);
        centerContainer.getChildren().addAll(middleSubjectHeader, middleTeacherHeader, table, fileButtonBox);
        root.setCenter(centerContainer);

        classList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selectedSubject) -> {
            if (selectedSubject != null) {
                if (!sidebar.getChildren().contains(classList)) {
                    restoreCourseModulesSidebar();
                }
                if (isChinese && selectedSubject.equals("Comprehensive Design of Data Structures")) {
                    middleSubjectHeader.setText("数据结构综合课程设计");
                } else if (!isChinese && selectedSubject.equals("数据结构综合课程设计")) {
                    middleSubjectHeader.setText("Comprehensive Design of Data Structures");
                } else {
                    middleSubjectHeader.setText(selectedSubject);
                }
                String teacherName = subjectTeacherMap.getOrDefault(selectedSubject, "TBA");
                middleTeacherHeader.setText(tr("Instructor: ", "讲师: ") + teacherName);
                searchField.clear();
                table.setItems(subjectFilteredMap.get(selectedSubject));
            }
        });

        if (!subjects.isEmpty()) classList.getSelectionModel().select(0);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            String selectedSubject = classList.getSelectionModel().getSelectedItem();
            if (selectedSubject != null) {
                subjectFilteredMap.get(selectedSubject).setPredicate(student -> {
                    if (newValue == null || newValue.isEmpty()) return true;
                    String lower = newValue.toLowerCase();
                    return student.getStudentId().toLowerCase().contains(lower) || student.getStudentName().toLowerCase().contains(lower);
                });
            }
        });

        addStudentBtn.setOnAction(e -> {
            String selectedSubject = classList.getSelectionModel().getSelectedItem();
            if (selectedSubject == null) return;
            String id = idField.getText().trim();
            String name = nameField.getText().trim();
            if (!id.isEmpty() && !name.isEmpty()) {
                subjectDataMap.get(selectedSubject).add(new StudentAttendance(id, name.toUpperCase(), totalConfiguredWeeks));
                idField.clear(); nameField.clear(); searchField.clear();
            } else {
                showAlert(tr("Please enter both Student ID and Name.", "请输入学生编号和姓名。"));
            }
        });

        Scene scene = new Scene(root, 1520, 720);
        stage.setTitle(tr("Attendance Sheet - Professional Edition", "考勤表 - 专业版"));
        stage.setScene(scene);
        stage.show();
    }

    private void handleCopyAll() {
        String selectedSubject = classList.getSelectionModel().getSelectedItem();
        if (selectedSubject == null) {
            showAlert(tr("Please select a subject to copy from.", "请选择要复制的科目。"));
            return;
        }
        ObservableList<StudentAttendance> students = subjectDataMap.get(selectedSubject);
        if (students == null || students.isEmpty()) {
            showAlert(tr("No student records available to copy.", "没有可复制的学生记录。"));
            return;
        }
        clipboardData = new ArrayList<>();
        for (StudentAttendance student : students) {
            Map<Integer, Integer> weeks = new HashMap<>();
            for (int i = 1; i <= totalConfiguredWeeks; i++) {
                weeks.put(i, student.weekProperty(i).get());
            }
            clipboardData.add(new CopiedStudentData(student.getStudentId(), student.getStudentName(), weeks));
        }
        showAlert(tr("Successfully copied " + clipboardData.size() + " student records to clipboard!", "成功将 " + clipboardData.size() + " 条学生记录复制到剪贴板！"));
    }

    private void handlePasteAll() {
        String selectedSubject = classList.getSelectionModel().getSelectedItem();
        if (selectedSubject == null) {
            showAlert(tr("Please select a target subject first.", "请先选择一个目标科目。"));
            return;
        }
        if (clipboardData == null || clipboardData.isEmpty()) {
            showAlert(tr("No copied data found in clipboard. Please copy a table first.", "剪贴板中没有找到复制的数据。请先复制一个表格。"));
            return;
        }
        ObservableList<StudentAttendance> currentList = subjectDataMap.get(selectedSubject);
        currentList.clear();
        for (CopiedStudentData copied : clipboardData) {
            StudentAttendance newStudent = new StudentAttendance(copied.id, copied.name, totalConfiguredWeeks);
            for (Map.Entry<Integer, Integer> entry : copied.weekStates.entrySet()) {
                int week = entry.getKey();
                int val = entry.getValue();
                if (week <= totalConfiguredWeeks) {
                    newStudent.weekProperty(week).set(val);
                }
            }
            currentList.add(newStudent);
        }
        showAlert(tr("Successfully pasted table data (" + currentList.size() + " students)!", "成功粘贴表格数据 (" + currentList.size() + " 名学生)！"));
    }

    private void showStudentStatisticsInSidebar(StudentAttendance student) {
        sidebar.getChildren().clear();
        Label statsTitle = new Label(tr("STUDENT STATISTICS", "学生统计信息"));
        statsTitle.setStyle("-fx-text-fill: " + (isDarkMode ? "#ffffff" : customFontColor) + "; -fx-font-weight: bold; -fx-font-size: 11px; -fx-letter-spacing: 1.5px;");

        int totalClass = totalConfiguredWeeks;
        int presentCount = 0;
        int absentCount = 0;
        for (int i = 1; i <= totalConfiguredWeeks; i++) {
            int val = student.weekProperty(i).get();
            if (val == 1) presentCount++;
            else if (val == 2) absentCount++;
        }
        int remainingClass = totalClass - (presentCount + absentCount);
        double attendancePercent = student.getAttendancePercentage();

        VBox infoCard = new VBox(6);
        infoCard.setPadding(new Insets(12));
        String cardBg = isDarkMode ? "#1e293b" : "#ffffff";
        String cardBorder = isDarkMode ? "#334155" : "#cbd5e1";
        infoCard.setStyle("-fx-background-color: " + cardBg + "; -fx-border-color: " + cardBorder + "; -fx-border-radius: 8px; -fx-background-radius: 8px;");

        String textColor = isDarkMode ? "#ffffff" : customFontColor;
        Label nameLbl = new Label(tr("Name: ", "姓名: ") + student.getStudentName());
        nameLbl.setStyle("-fx-text-fill: " + textColor + "; -fx-font-weight: bold; -fx-font-size: " + globalFontSize + ";");
        Label idLbl = new Label(tr("ID: ", "编号: ") + student.getStudentId());
        idLbl.setStyle("-fx-text-fill: " + textColor + "; -fx-font-size: " + globalFontSize + ";");
        Label totalLbl = new Label(tr("Total Class: ", "总课次: ") + totalClass);
        totalLbl.setStyle("-fx-text-fill: " + textColor + "; -fx-font-size: " + globalFontSize + ";");
        Label presentLbl = new Label(tr("Present: ", "出勤: ") + presentCount);
        presentLbl.setStyle("-fx-text-fill: " + (isDarkMode ? "#34d399" : "#047857") + "; -fx-font-weight: bold; -fx-font-size: " + globalFontSize + ";");
        Label absentLbl = new Label(tr("Absent: ", "缺勤: ") + absentCount);
        absentLbl.setStyle("-fx-text-fill: " + (isDarkMode ? "#f87171" : "#dc2626") + "; -fx-font-weight: bold; -fx-font-size: " + globalFontSize + ";");
        Label remainingLbl = new Label(tr("Remaining Class: ", "剩余课次: ") + remainingClass);
        remainingLbl.setStyle("-fx-text-fill: " + textColor + "; -fx-font-size: " + globalFontSize + ";");
        Label percentLbl = new Label(String.format(tr("Attendance %%: %.1f%%", "出勤率 %%: %.1f%%"), attendancePercent));
        percentLbl.setStyle("-fx-text-fill: " + customAccentColor + "; -fx-font-weight: bold; -fx-font-size: " + globalFontSize + ";");

        infoCard.getChildren().addAll(nameLbl, idLbl, totalLbl, presentLbl, absentLbl, remainingLbl, percentLbl);

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                new PieChart.Data(tr("Present", "出勤") + " (" + presentCount + ")", presentCount),
                new PieChart.Data(tr("Absent", "缺勤") + " (" + absentCount + ")", absentCount),
                new PieChart.Data(tr("Unmarked", "未标记") + " (" + remainingClass + ")", Math.max(0, remainingClass))
        );
        PieChart pieChart = new PieChart(pieChartData);
        pieChart.setPrefSize(240, 220);
        pieChart.setLegendVisible(true);
        pieChart.setLabelsVisible(false);

        Button backBtn = new Button(tr("← Back to Course Modules", "← 返回课程模块"));
        updateToolButtonStyle(backBtn, isDarkMode);
        backBtn.setMaxWidth(Double.MAX_VALUE);
        backBtn.setOnAction(e -> restoreCourseModulesSidebar());

        sidebar.getChildren().addAll(statsTitle, infoCard, pieChart, backBtn);
    }

    private void restoreCourseModulesSidebar() {
        sidebar.getChildren().clear();
        sidebar.getChildren().addAll(sidebarTitle, subjectSearchField, classList, newSubjectField, teacherNameField, addSubjectBtn);
    }

    private void exportDataToFile(String formatType) {
        String selectedSubject = classList.getSelectionModel().getSelectedItem();
        if (selectedSubject == null) {
            showAlert(tr("Please select a subject to export.", "请选择要导出的科目。"));
            return;
        }
        ObservableList<StudentAttendance> students = subjectDataMap.get(selectedSubject);
        if (students == null || students.isEmpty()) {
            showAlert(tr("No student records available to export.", "没有可导出的学生记录。"));
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(tr("Export Attendance Report (" + formatType + ")", "导出考勤报告 (" + formatType + ")"));
        String ext = "*.csv";
        String description = "CSV Files";
        if (formatType.equals("Word")) {
            ext = "*.docx";
            description = "Word Document Files";
        } else if (formatType.equals("Excel")) {
            ext = "*.xlsx";
            description = "Excel Spreadsheet Files";
        } else if (formatType.equals("PDF")) {
            ext = "*.pdf";
            description = "PDF Document Files";
        } else if (formatType.equals("CSV")) {
            ext = "*.csv";
            description = "CSV Files";
        }
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, ext));
        fileChooser.setInitialFileName(selectedSubject.replaceAll("[^a-zA-Z0-9.-]", "_") + "_attendance." + ext.replace("*.", ""));
        File file = fileChooser.showSaveDialog(mainStage);
        if (file != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                if (formatType.equals("Word") || formatType.equals("PDF")) {
                    writer.write("========================================\n");
                    writer.write(" ATTENDANCE REPORT: " + selectedSubject + "\n");
                    writer.write(" INSTRUCTOR: " + subjectTeacherMap.getOrDefault(selectedSubject, "N/A") + "\n");
                    writer.write("========================================\n\n");
                    for (StudentAttendance student : students) {
                        writer.write(String.format("Student: %s (ID: %s) - Attendance: %.1f%%\n",
                                student.getStudentName(), student.getStudentId(), student.getAttendancePercentage()));
                    }
                } else {
                    StringBuilder header = new StringBuilder("Student Name,Student ID,Attendance %");
                    for (int i = 1; i <= totalConfiguredWeeks; i++) {
                        header.append(",W").append(i);
                    }
                    writer.write(header.toString());
                    writer.newLine();
                    for (StudentAttendance student : students) {
                        StringBuilder row = new StringBuilder();
                        row.append("\"").append(student.getStudentName()).append("\",");
                        row.append("\"").append(student.getStudentId()).append("\",");
                        row.append(String.format("%.2f", student.getAttendancePercentage()));
                        for (int i = 1; i <= totalConfiguredWeeks; i++) {
                            row.append(",").append(student.weekProperty(i).get());
                        }
                        writer.write(row.toString());
                        writer.newLine();
                    }
                }
                showAlert(tr("Successfully exported file as " + formatType + "!", "成功导出 " + formatType + " 文件！"));
            } catch (IOException ex) {
                showAlert(tr("Error exporting file: ", "导出文件出错: ") + ex.getMessage());
            }
        }
    }

    private void importDataFromFile(String formatType) {
        String selectedSubject = classList.getSelectionModel().getSelectedItem();
        if (selectedSubject == null) {
            showAlert(tr("Please select a subject first.", "请先选择一个科目。"));
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(tr("Import Attendance Data (" + formatType + ")", "导入考勤数据 (" + formatType + ")"));
        String ext = "*.csv";
        if (formatType.equals("Word")) ext = "*.docx;*.doc;*.txt";
        else if (formatType.equals("Excel")) ext = "*.xlsx;*.xls;*.csv";
        else if (formatType.equals("PDF")) ext = "*.pdf";
        else if (formatType.equals("CSV")) ext = "*.csv";
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Supported Files", ext));
        File file = fileChooser.showOpenDialog(mainStage);
        if (file != null) {
            int importedCount = 0;
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line = reader.readLine();
                if (line == null) {
                    showAlert(tr("The imported file is empty.", "导入的文件为空。"));
                    return;
                }
                ObservableList<StudentAttendance> currentList = subjectDataMap.get(selectedSubject);
                while ((line = reader.readLine()) != null) {
                    String[] tokens = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                    if (tokens.length >= 2) {
                        String name = tokens[0].replaceAll("^\"|\"$", "").trim();
                        String id = tokens[1].replaceAll("^\"|\"$", "").trim();
                        if (!id.isEmpty() && !name.isEmpty() && !id.toLowerCase().contains("student")) {
                            StudentAttendance student = new StudentAttendance(id, name.toUpperCase(), totalConfiguredWeeks);
                            for (int i = 1; i <= totalConfiguredWeeks; i++) {
                                int tokenIdx = 2 + i;
                                if (tokenIdx < tokens.length) {
                                    try {
                                        int val = Integer.parseInt(tokens[tokenIdx].replaceAll("^\"|\"$", "").trim());
                                        if (val >= 0 && val <= 2) {
                                            student.weekProperty(i).set(val);
                                        }
                                    } catch (NumberFormatException ignored) {}
                                }
                            }
                            boolean exists = currentList.stream().anyMatch(s -> s.getStudentId().equalsIgnoreCase(id));
                            if (!exists) {
                                currentList.add(student);
                                importedCount++;
                            }
                        }
                    }
                }
                showAlert(tr("Successfully imported " + importedCount + " student records from " + formatType + "!", "成功从 " + formatType + " 导入 " + importedCount + " 条学生记录！"));
            } catch (IOException ex) {
                showAlert(tr("Error reading file: ", "读取文件出错: ") + ex.getMessage());
            }
        }
    }

    private void refreshLanguageUI() {
        mainStage.setTitle(tr("Attendance Sheet - Professional Edition", "考勤表 - 专业版"));
        sidebarTitle.setText(tr("COURSE MODULES", "课程模块"));
        subjectSearchField.setPromptText(tr("🔍 Filter subjects...", "🔍 筛选科目..."));
        newSubjectField.setPromptText(tr("New subject name...", "新科目名称..."));
        teacherNameField.setPromptText(tr("Instructor / Teacher name...", "授课教师姓名..."));
        addSubjectBtn.setText(tr("＋ Create Subject", "＋ 创建科目"));
        enrollLabel.setText(tr("ENROLL:", "注册:"));
        idField.setPromptText(tr("Student ID", "学生编号"));
        nameField.setPromptText(tr("Student Full Name", "学生全名"));
        addStudentBtn.setText(tr("＋ Add Student", "＋ 添加学生"));
        searchField.setPromptText(tr("🔍 Quick search record...", "🔍 快速搜索记录..."));
        modeToggleBtn.setText(isDarkMode ? tr("☀️ Light Mode", "☀️ 浅色模式") : tr("🌙 Dark Mode", "🌙 深色模式"));
        copyAllBtn.setText(tr("📋 Copy All", "📋 复制全部"));
        pasteAllBtn.setText(tr("📋 Paste All", "📋 粘贴全部"));

        Label placeholderLabel = (Label) table.getPlaceholder();
        if (placeholderLabel != null) {
            placeholderLabel.setText(tr("No content in table", "表格中暂无内容"));
            placeholderLabel.setStyle("-fx-text-fill: " + (isDarkMode ? "#9ca3af" : "#334155") + "; -fx-font-size: " + globalFontSize + ";");
        }

        String selectedSubject = classList.getSelectionModel().getSelectedItem();
        if (selectedSubject != null) {
            if (isChinese && selectedSubject.equals("Comprehensive Design of Data Structures")) {
                middleSubjectHeader.setText("数据结构综合课程设计");
            } else if (!isChinese && selectedSubject.equals("数据结构综合课程设计")) {
                middleSubjectHeader.setText("Comprehensive Design of Data Structures");
            } else {
                middleSubjectHeader.setText(selectedSubject);
            }
            String teacherName = subjectTeacherMap.getOrDefault(selectedSubject, "TBA");
            middleTeacherHeader.setText(tr("Instructor: ", "讲师: ") + teacherName);
        }
        classList.refresh();

        if (!table.getColumns().isEmpty()) {
            table.getColumns().get(0).setText(tr("Student Name", "学生姓名"));
            table.getColumns().get(1).setText(tr("Student ID", "学生编号"));
            table.getColumns().get(2).setText(tr("Attendance %", "出勤率 %"));
            for (int i = 1; i <= totalConfiguredWeeks; i++) {
                if (2 + i < table.getColumns().size()) {
                    table.getColumns().get(2 + i).setText(tr("W", "第") + i + tr("", "周"));
                }
            }
        }
        table.refresh();
    }

    private void rebuildTableColumns() {
        table.getColumns().clear();

        TableColumn<StudentAttendance, String> nameCol = new TableColumn<>(tr("Student Name", "学生姓名"));
        nameCol.setCellValueFactory(cell -> cell.getValue().studentNameProperty());
        nameCol.setMinWidth(110);
        nameCol.setPrefWidth(140);
        nameCol.setStyle("-fx-alignment: CENTER-LEFT; -fx-padding: 0 0 0 10px; -fx-font-size: " + globalFontSize + ";");

        TableColumn<StudentAttendance, String> idCol = new TableColumn<>(tr("Student ID", "学生编号"));
        idCol.setCellValueFactory(cell -> cell.getValue().studentIdProperty());
        idCol.setMinWidth(80);
        idCol.setPrefWidth(100);
        idCol.setStyle("-fx-alignment: CENTER; -fx-font-size: " + globalFontSize + ";");

        TableColumn<StudentAttendance, Number> percentCol = new TableColumn<>(tr("Attendance %", "出勤率 %"));
        percentCol.setCellValueFactory(cell -> cell.getValue().attendancePercentageProperty());
        percentCol.setMinWidth(85);
        percentCol.setPrefWidth(105);
        percentCol.setStyle("-fx-alignment: CENTER;");
        percentCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Number percentage, boolean empty) {
                super.updateItem(percentage, empty);
                if (empty || percentage == null) {
                    setText(null);
                    setStyle("");
                } else {
                    double val = percentage.doubleValue();
                    String textColor = val >= 75 ? (isDarkMode ? "#34d399" : "#047857") : (val >= 50 ? (isDarkMode ? "#fbbf24" : "#b45309") : (isDarkMode ? "#f87171" : "#b91c1c"));
                    setText(String.format("%.1f%%", val));
                    setStyle("-fx-alignment: CENTER; -fx-text-fill: " + textColor + "; -fx-font-weight: bold; -fx-font-size: " + globalFontSize + ";");
                }
            }
        });

        table.getColumns().addAll(nameCol, idCol, percentCol);

        for (int i = 1; i <= totalConfiguredWeeks; i++) {
            final int weekIndex = i;
            String weekHeader = tr("W", "第") + i + tr("", "周");
            TableColumn<StudentAttendance, Number> weekCol = new TableColumn<>(weekHeader);
            weekCol.setCellValueFactory(cell -> cell.getValue().weekProperty(weekIndex));
            weekCol.setPrefWidth(60);
            weekCol.setMinWidth(50);
            weekCol.setStyle("-fx-alignment: CENTER;");

            weekCol.setCellFactory(tc -> new TableCell<>() {
                @Override
                protected void updateItem(Number item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        int state = item.intValue();
                        String text;
                        String colorStyle;
                        if (state == 1) {
                            text = tr("✔", "✔");
                            colorStyle = isDarkMode ? "-fx-text-fill: #34d399; -fx-font-weight: bold;" : "-fx-text-fill: #047857; -fx-font-weight: bold;";
                        } else if (state == 2) {
                            text = tr("✖", "✖");
                            colorStyle = isDarkMode ? "-fx-text-fill: #f87171; -fx-font-weight: bold;" : "-fx-text-fill: #b91c1c; -fx-font-weight: bold;";
                        } else {
                            text = "-";
                            colorStyle = isDarkMode ? "-fx-text-fill: #6b7280;" : "-fx-text-fill: #475569;";
                        }
                        setText(text);
                        setStyle("-fx-alignment: CENTER; " + colorStyle + " -fx-font-size: " + globalFontSize + ";");

                        setOnMouseClicked(e -> {
                            StudentAttendance student = getTableView().getItems().get(getIndex());
                            IntegerProperty prop = student.weekProperty(weekIndex);
                            prop.set((prop.get() + 1) % 3);
                        });
                    }
                }
            });
            table.getColumns().add(weekCol);
        }
    }

    private void editSubject(String oldSubjectName) {
        TextInputDialog dialog = new TextInputDialog(oldSubjectName);
        dialog.setTitle(tr("Edit Subject Name", "编辑科目名称"));
        dialog.setHeaderText(tr("Rename subject:", "重命名科目:"));
        dialog.setContentText(tr("Subject:", "科目:"));
        dialog.showAndWait().ifPresent(newName -> {
            newName = newName.trim();
            if (!newName.isEmpty() && !subjects.contains(newName)) {
                int idx = subjects.indexOf(oldSubjectName);
                subjects.set(idx, newName);
                String teacher = subjectTeacherMap.remove(oldSubjectName);
                subjectTeacherMap.put(newName, teacher != null ? teacher : "TBA");
                ObservableList<StudentAttendance> data = subjectDataMap.remove(oldSubjectName);
                subjectDataMap.put(newName, data);
                FilteredList<StudentAttendance> filtered = subjectFilteredMap.remove(oldSubjectName);
                subjectFilteredMap.put(newName, filtered);
                classList.getSelectionModel().select(newName);
            }
        });
    }

    private void editStudent(StudentAttendance student) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(tr("Edit Student", "编辑学生"));
        dialog.setHeaderText(tr("Modify Student Information", "修改学生信息"));

        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        TextField idTf = new TextField(student.getStudentId());
        TextField nameTf = new TextField(student.getStudentName());
        content.getChildren().addAll(new Label(tr("Student ID:", "学生编号:")), idTf, new Label(tr("Student Name:", "学生姓名:")), nameTf);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                student.setStudentId(idTf.getText().trim());
                student.setStudentName(nameTf.getText().trim().toUpperCase());
                table.refresh();
            }
            return null;
        });
        dialog.showAndWait();
    }

    private void refreshUITheme() {
        applyTheme(root, isDarkMode);
        updateSidebarStyle(sidebar, isDarkMode);
        updateTopBarStyle(topBar, isDarkMode);
        updateCenterContainerStyle(centerContainer, isDarkMode);
        updateListStyle(classList, isDarkMode);
        updateTableStyle(table, isDarkMode);
        updateTextFieldStyle(subjectSearchField, isDarkMode);
        updateTextFieldStyle(newSubjectField, isDarkMode);
        updateTextFieldStyle(teacherNameField, isDarkMode);
        updateTextFieldStyle(idField, isDarkMode);
        updateTextFieldStyle(nameField, isDarkMode);
        updateTextFieldStyle(searchField, isDarkMode);
        updatePrimaryButtonStyle(addSubjectBtn, isDarkMode);
        updateToolButtonStyle(modeToggleBtn, isDarkMode);
        updateToolButtonStyle(translateBtn, isDarkMode);
        updateToolButtonStyle(copyAllBtn, isDarkMode);
        updateToolButtonStyle(pasteAllBtn, isDarkMode);

        middleSubjectHeader.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + (isDarkMode ? "#ffffff" : customFontColor) + ";");
        middleTeacherHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: " + (isDarkMode ? "#9ca3af" : "#334155") + ";");
        sidebarTitle.setStyle("-fx-text-fill: " + (isDarkMode ? "#ffffff" : customFontColor) + "; -fx-font-weight: bold; -fx-font-size: 11px; -fx-letter-spacing: 1.5px;");
        enrollLabel.setStyle("-fx-text-fill: " + (isDarkMode ? "#ffffff" : customFontColor) + "; -fx-font-weight: bold; -fx-font-size: 11px;");

        classList.refresh();
        table.refresh();
    }

    private void applyTheme(BorderPane pane, boolean dark) {
        pane.setStyle("-fx-background-color: " + (dark ? "#0f172a" : "#D8E2DC") + ";");
    }

    private void updateSidebarStyle(VBox box, boolean dark) {
        box.setStyle("-fx-background-color: " + (dark ? "#1e293b" : "#E5ECE9") + "; -fx-border-color: " + (dark ? "#334155" : "#cbd5e1") + "; -fx-border-width: 0 1 0 0;");
    }

    private void updateTopBarStyle(HBox box, boolean dark) {
        box.setStyle("-fx-background-color: " + (dark ? "#1e293b" : "#E5ECE9") + "; -fx-border-color: " + (dark ? "#334155" : "#cbd5e1") + "; -fx-border-width: 0 0 1 0;");
    }

    private void updateCenterContainerStyle(VBox box, boolean dark) {
        box.setStyle("-fx-background-color: " + (dark ? "#0f172a" : "#D8E2DC") + ";");
    }

    private void updateListStyle(ListView<String> list, boolean dark) {
        list.setStyle("-fx-background-color: transparent; -fx-control-inner-background: " + (dark ? "#1e293b" : "#ffffff") + "; -fx-font-size: " + globalFontSize + ";");
    }

    private void updateTableStyle(TableView<?> tbl, boolean dark) {
        tbl.setStyle("-fx-background-color: " + (dark ? "#1e293b" : "#ffffff") + "; -fx-font-size: " + globalFontSize + ";");
    }

    private void updateTextFieldStyle(TextField tf, boolean dark) {
        tf.setStyle("-fx-background-color: " + (dark ? "#0f172a" : "#ffffff") + "; -fx-text-fill: " + (dark ? "#ffffff" : "#111827") + "; -fx-border-color: " + (dark ? "#334155" : "#cbd5e1") + "; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 6px 10px; -fx-font-size: " + globalFontSize + ";");
    }

    private void updatePrimaryButtonStyle(Button btn, boolean dark) {
        btn.setStyle("-fx-background-color: " + customAccentColor + "; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 8px 12px; -fx-cursor: hand; -fx-font-size: " + globalFontSize + ";");
    }

    private void updateToolButtonStyle(Button btn, boolean dark) {
        btn.setStyle("-fx-background-color: " + (dark ? "#334155" : "#ffffff") + "; -fx-text-fill: " + (dark ? "#ffffff" : "#1f2937") + "; -fx-font-weight: 500; -fx-border-color: " + (dark ? "transparent" : "#cbd5e1") + "; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 7px 12px; -fx-cursor: hand; -fx-font-size: " + globalFontSize + ";");
    }

    private void updateMenuButtonStyle(MenuButton btn, boolean dark) {
        btn.setStyle("-fx-background-color: " + (dark ? "#334155" : "#ffffff") + "; -fx-text-fill: " + (dark ? "#ffffff" : "#1f2937") + "; -fx-font-weight: 500; -fx-border-color: " + (dark ? "transparent" : "#cbd5e1") + "; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 7px 12px; -fx-cursor: hand; -fx-font-size: " + globalFontSize + ";");
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}