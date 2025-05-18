package com.newton.couplespace.models

import com.google.firebase.firestore.DocumentId
import java.util.*

data class TimelineEntry(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val date: Date = Date(),
    val type: EntryType = EntryType.EVENT,
    val nudgeEnabled: Boolean = false,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

enum class EntryType {
    EVENT, MEMORY
}
