package com.newton.couplespace.models

import com.google.firebase.firestore.DocumentId
import java.util.*

data class CoupleQuestion(
    @DocumentId
    val id: String = "",
    val topic: QuestionTopic = QuestionTopic.FUNNY,
    val questionText: String = "",
    val userResponses: Map<String, String> = mapOf(),
    val timestamp: Date = Date()
)

enum class QuestionTopic {
    NAUGHTY, FUNNY, EMOTIONAL
}
