package com.example.project_vending_machines.entity

import jakarta.persistence.*

@Entity
@Table(name = "roles")
data class Role(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0,

    @Column(nullable = false, unique = true)
    var name: String = "",

    var description: String? = null
)