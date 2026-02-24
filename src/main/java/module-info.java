module com.example.project_vending_machines {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.hibernate.orm.core;
    requires jakarta.persistence;
    requires java.naming;
    requires java.sql;
    requires org.postgresql.jdbc;
    requires kotlin.stdlib;

    opens com.example.project_vending_machines to javafx.fxml, org.hibernate.orm.core;
    opens com.example.project_vending_machines.entity to org.hibernate.orm.core, javafx.base;
    opens com.example.project_vending_machines.dao to org.hibernate.orm.core;

    exports com.example.project_vending_machines;
    exports com.example.project_vending_machines.entity;
    exports com.example.project_vending_machines.dao;
}