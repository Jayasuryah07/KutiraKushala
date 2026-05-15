package com.example.kutirakushala.models

data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val isSeller: Boolean = false,
    val businessName: String = "",
    val businessAddress: String = "",
    val isAvailable: Boolean = true
)