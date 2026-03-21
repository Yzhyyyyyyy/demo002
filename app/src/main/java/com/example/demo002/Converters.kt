package com.example.demo002

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {

    // ── LocalDate ──────────────────────────────
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? =
        value?.let { LocalDate.parse(it) }

    // ── Priority ───────────────────────────────
    @TypeConverter
    fun fromPriority(priority: Priority): String = priority.name

    @TypeConverter
    fun toPriority(value: String): Priority = Priority.valueOf(value)

    // ── Tags（只存预设标签的 label，用逗号分隔）──
    @TypeConverter
    fun fromTags(tags: List<String>): String = tags.joinToString(",")

    @TypeConverter
    fun toTags(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split(",")
}