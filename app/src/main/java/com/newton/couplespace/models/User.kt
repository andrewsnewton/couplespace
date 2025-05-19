package com.newton.couplespace.models

import com.google.firebase.firestore.DocumentId
import java.util.*

data class User(
    @DocumentId
    val id: String = "",
    val email: String = "", // Added email field
    val name: String = "",
    val age: Int = 0,
    val height: Int = 0, // in cm
    val weight: Double = 0.0, // in kg
    val coupleCode: String = "",
    val partnerId: String = "",
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val isProfileComplete: Boolean = false // Added profile completion flag
)
