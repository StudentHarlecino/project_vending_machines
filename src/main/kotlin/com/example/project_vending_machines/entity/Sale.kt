package com.example.project_vending_machines.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "sales")
data class Sale(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "device_id", nullable = false)
    var deviceId: Int = 0,

    @Column(name = "product_id", nullable = false)
    var productId: Int = 0,

    @Column(name = "quantity_sold", nullable = false)
    var quantitySold: Int = 0,

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    var totalAmount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "sale_datetime", nullable = false)
    var saleDatetime: LocalDateTime = LocalDateTime.now(),

    @Column(name = "payment_method")
    var paymentMethod: String? = null
)