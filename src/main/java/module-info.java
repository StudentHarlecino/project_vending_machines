module com.example.project_vending_machines {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;

    requires java.naming;
    requires java.sql;

    requires org.hibernate.org.core;
    requires jakarta.peristence;

    requires org.postgresql.jdbc;

    opens com.example.project_vending_machines.models.entity to org.hibernate.orm.core;

    opens com.example.project_vending_machines to javafx.fxml;
    exports com.example.project_vending_machines;
}