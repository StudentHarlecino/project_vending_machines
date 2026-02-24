package com.example.project_vending_machines.entity

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "maintenance")
data class Maintenance(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "device_id", nullable = false)
    var deviceId: Int = 0,

    @Column(name = "service_date", nullable = false)
    var serviceDate: LocalDate = LocalDate.now(),

    @Column(columnDefinition = "text")
    var description: String? = null,

    @Column(name = "performed_by")
    var performedBy: Int? = null,

    @ManyToOne
    @JoinColumn(name = "performed_by", insertable = false, updatable = false)
    var technician: User? = null
)