package com.helpme.commonmarket.user.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class User(
    @Id
    val id: String,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false, unique = true)
    var email: String,

    @Column(nullable = false)
    var password: String,

    @Column(nullable = false)
    var role: String = "USER",

    @Column(name = "create_dt", nullable = false, updatable = false)
    val createDt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "update_dt", nullable = false)
    var updateDt: LocalDateTime = LocalDateTime.now()
)