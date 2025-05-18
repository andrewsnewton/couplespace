package com.newton.couplespace.models

import com.google.firebase.firestore.DocumentId
import java.util.*

data class HealthLog(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val foodName: String = "",
    val quantity: String = "",
    val calories: Int = 0,
    val time: Date = Date(),
    val notes: String = "",
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
