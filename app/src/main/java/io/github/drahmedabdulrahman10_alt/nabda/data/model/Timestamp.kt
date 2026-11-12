package io.github.drahmedabdulrahman10_alt.nabda.data.model

import java.util.Date

/** A point in time stored as epoch milliseconds. */
data class Timestamp(val millis: Long) {
    constructor(date: Date) : this(date.time)

    fun toDate(): Date = Date(millis)

    companion object {
        fun now(): Timestamp = Timestamp(System.currentTimeMillis())
    }
}
