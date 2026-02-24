package com.example.project_vending_machines.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "products")
data class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(nullable = false)
    var name: String = "",

    @Column(columnDefinition = "text")
    var description: String? = null,

    @Column(nullable = false, precision = 10, scale = 2)
    var price: BigDecimal = BigDecimal.ZERO,

    @Column(name = "quantity_in_stock", nullable = false)
    var quantityInStock: Int = 0,

    @Column(name = "minimal_stock", nullable = false)
    var minimalStock: Int = 0,

    @Column(name = "sales_trends", columnDefinition = "text")
    var salesTrends: String? = null
)