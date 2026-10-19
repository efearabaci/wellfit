package com.denge.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// One versioned aggregate is committed atomically. A day, its snapshot and pending recap
// can never be split across a crash. Room/SQLite WAL supplies durability and transactions.
@Entity(tableName = "journal")
data class JournalEntity(@PrimaryKey val id: Int = 1, val json: String)
@Dao
interface JournalDao {
    @Query("SELECT * FROM journal WHERE id = 1") fun observe(): Flow<JournalEntity?>
    @Query("SELECT * FROM journal WHERE id = 1") suspend fun read(): JournalEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(entity: JournalEntity)
}
@Database(entities = [JournalEntity::class], version = 1, exportSchema = true)
abstract class DengeDatabase : RoomDatabase() { abstract fun journal(): JournalDao }
