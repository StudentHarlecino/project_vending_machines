module com.example.project_vending_machines {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;


    opens com.example.project_vending_machines to javafx.fxml;
    exports com.example.project_vending_machines;
}