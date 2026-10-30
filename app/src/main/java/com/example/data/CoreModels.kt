package com.example.data

import androidx.room.*

// Secondary reconstruction: original APK data classes and AppDatabase_Impl schema.
@Entity(tableName = "dhikr_table", indices = [Index("isFavorite"), Index("isEnabled")])
data class Dhikr(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val isFavorite: Boolean = false,
    val reminderTimes: List<String> = listOf("09:00"),
    val isEnabled: Boolean = false,
    val readCount: Int = 0,
    val lastReadTime: Long = 0
)

@Entity(tableName = "tag_table")
data class Tag(@PrimaryKey(autoGenerate = true) val tagId: Long = 0, val name: String, val colorHex: String = "#008080")

@Entity(tableName = "dhikr_tag_cross_ref", primaryKeys = ["dhikrId", "tagId"], indices = [Index("dhikrId"), Index("tagId")])
data class DhikrTagCrossRef(val dhikrId: Int, val tagId: Long)

data class DhikrWithTags(
    @Embedded val dhikr: Dhikr,
    @Relation(parentColumn = "id", entityColumn = "tagId", associateBy = Junction(DhikrTagCrossRef::class, parentColumn = "dhikrId", entityColumn = "tagId"))
    val tags: List<Tag> = emptyList()
)

@Entity(tableName = "dhikr_history")
data class DhikrHistory(@PrimaryKey(autoGenerate = true) val historyId: Long = 0, val dhikrId: Int, val timestamp: Long = System.currentTimeMillis(), val dateString: String)

class Converters {
    @TypeConverter fun fromString(value: String): List<String> = value.split(",").filterNot { it.isBlank() }
    @TypeConverter fun toString(value: List<String>): String = value.joinToString(",")
}
