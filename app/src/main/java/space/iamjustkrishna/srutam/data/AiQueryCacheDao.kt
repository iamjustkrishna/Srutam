package space.iamjustkrishna.srutam.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AiQueryCacheDao {

    @Query("SELECT * FROM ai_query_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun get(key: String): AiQueryCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: AiQueryCache)

    @Query("UPDATE ai_query_cache SET lastAccessedAt = :timestamp WHERE cacheKey = :key")
    suspend fun updateAccessTime(key: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM ai_query_cache WHERE cacheKey NOT IN (SELECT cacheKey FROM ai_query_cache ORDER BY lastAccessedAt DESC LIMIT :maxEntries)")
    suspend fun pruneOldEntries(maxEntries: Int = 500)

    @Query("DELETE FROM ai_query_cache")
    suspend fun clearAll()
}
