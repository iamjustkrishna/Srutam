package space.iamjustkrishna.srutam.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_query_cache")
data class AiQueryCache(
    @PrimaryKey
    val cacheKey: String,
    val queryType: String, // "SINGLE" or "GLOBAL"
    val normalizedQuery: String,
    val contextFingerprint: String,
    val answer: String,
    val citedNotesJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis()
)
