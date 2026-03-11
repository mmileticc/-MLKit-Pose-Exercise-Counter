package dev.milinko.workoutapp.db

import androidx.room.TypeConverter
import java.util.*
import kotlin.let

class DateConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun toTimestamp(date: Date?): Long? {
        return date?.time?.toLong()
    }
}