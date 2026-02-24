package com.example.project_vending_machines.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "vending_machines")
data class VendingMachine(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var location: String? = null,
    var model: String? = null,
    var type: String? = null,

    @Column(name = "total_income")
    var totalIncome: Double? = 0.0,

    @Column(name = "serial_number", unique = true)
    var serialNumber: String? = null,

    @Column(name = "inventory_number", unique = true)
    var inventoryNumber: String? = null,

    var manufacturer: String? = null,

    @Column(name = "manufacture_date")
    var manufactureDate: LocalDate? = null,

    @Column(name = "commissioning_date")
    var commissioningDate: LocalDate? = null,

    @Column(name = "last_verification_date")
    var lastVerificationDate: LocalDate? = null,

    @Column(name = "verification_interval")
    var verificationInterval: Int? = null,

    @Column(name = "resource_hours")
    var resourceHours: Int? = null,

    @Column(name = "next_maintenance_date")
    var nextMaintenanceDate: LocalDate? = null,

    @Column(name = "maintenance_time")
    var maintenanceTime: Int? = null,

    var status: String? = null,
    var country: String? = null,

    @Column(name = "inventory_check_date")
    var inventoryCheckDate: LocalDate? = null,

    @Column(name = "last_verification_worker")
    var lastVerificationWorker: String? = null
)