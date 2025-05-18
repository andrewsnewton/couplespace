package com.newton.couplespace.models

import com.google.firebase.firestore.DocumentId
import java.util.*

data class Message(
    @DocumentId
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Date = Date(),
    val read: Boolean = false
)
