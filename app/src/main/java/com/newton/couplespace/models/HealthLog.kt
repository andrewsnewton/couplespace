package com.newton.couplespace.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import java.util.*

data class HealthLog(
    @DocumentId
    var id: String = "",
    val userId: String = "",
    val foodName: String = "",
    var quantity: Double = 0.0,
    var calories: Int = 0,
    var time: Date = Date(),
    var notes: String = ""
) {
    constructor() : this("", "", "", 0.0, 0, Date(), "")
    
    companion object {
        fun fromMap(map: Map<String, Any>): HealthLog {
            return HealthLog(
                id = map["id"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                foodName = map["foodName"] as? String ?: "",
                quantity = (map["quantity"] as? Number)?.toDouble() ?: 0.0,
                calories = (map["calories"] as? Number)?.toInt() ?: 0,
                time = (map["time"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                notes = map["notes"] as? String ?: ""
            )
        }
    }
    
    fun toMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "foodName" to foodName,
            "quantity" to quantity,
            "calories" to calories,
            "time" to time,
            "notes" to notes
        )
    }
}
