package com.example.project_vending_machines.entity

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "vending_machines")
data class VendingMachine(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(nullable = false)
    var name: String = "",

    var model: String? = null,

    var company: String? = null,

    var modem: String? = null,

    @Column(columnDefinition = "text")
    var address: String? = null,

    var location: String? = null,

    @Column(name = "installation_date")
    var installationDate: LocalDate? = null
)