package com.newton.couplespace.util

import java.time.ZoneId
import java.util.TimeZone

/**
 * Extension function to convert java.util.TimeZone to java.time.ZoneId
 */
fun TimeZone.toZoneId(): ZoneId {
    return ZoneId.of(this.id)
}
