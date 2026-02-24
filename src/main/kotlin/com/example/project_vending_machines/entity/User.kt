package com.example.project_vending_machines.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "role_id", nullable = false)
    var roleId: Int = 0,

    @Column(name = "first_name", nullable = false)
    var firstName: String = "",

    @Column(name = "last_name", nullable = false)
    var lastName: String = "",

    var patronymic: String? = null,

    @Column(unique = true)
    var email: String? = null,

    var phone: String? = null,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String = "",

    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "last_login")
    var lastLogin: LocalDateTime? = null
)