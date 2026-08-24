package com.karoslabs.deardiary.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "entries")
data class EntryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val transcript: String,
    val createdAtEpochMs: Long,
    val durationMs: Long,
    val audioFileName: String?,
)

@Fts4(contentEntity = EntryEntity::class)
@Entity(tableName = "entries_fts")
data class EntryFts(
    val title: String,
    val transcript: String,
)

@Entity(
    tableName = "tags",
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("entryId"), Index("name")],
)
data class TagEntity(
    @PrimaryKey val id: String,
    val entryId: String,
    val name: String,
)

data class EntryWithTags(
    @Embedded val entry: EntryEntity,
    @Relation(parentColumn = "id", entityColumn = "entryId")
    val tags: List<TagEntity>,
)

@Dao
interface JournalDao {
    @Transaction
    @Query("SELECT * FROM entries ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<EntryWithTags>>

    @Transaction
    @Query("SELECT * FROM entries WHERE id = :id")
    fun observeById(id: String): Flow<EntryWithTags?>

    @Transaction
    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun getById(id: String): EntryWithTags?

    @Transaction
    @Query("SELECT * FROM entries ORDER BY createdAtEpochMs DESC")
    suspend fun getAll(): List<EntryWithTags>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntry(entry: EntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTags(tags: List<TagEntity>)

    @Query("DELETE FROM tags WHERE entryId = :entryId")
    suspend fun deleteTagsFor(entryId: String)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun deleteEntry(id: String)

    @Query("DELETE FROM entries")
    suspend fun deleteAllEntries()

    @Query("DELETE FROM tags")
    suspend fun deleteAllTags()

    @Query("SELECT COUNT(*) FROM entries")
    fun observeCount(): Flow<Int>

    @Query(
        """
        SELECT DISTINCT e.* FROM entries e
        WHERE e.rowid IN (SELECT rowid FROM entries_fts WHERE entries_fts MATCH :fts)
           OR e.id IN (SELECT entryId FROM tags WHERE name LIKE '%' || :raw || '%')
        ORDER BY e.createdAtEpochMs DESC
        """,
    )
    suspend fun search(fts: String, raw: String): List<EntryEntity>

    @Transaction
    @Query(
        """
        SELECT DISTINCT e.* FROM entries e
        WHERE e.rowid IN (SELECT rowid FROM entries_fts WHERE entries_fts MATCH :fts)
           OR e.id IN (SELECT entryId FROM tags WHERE name LIKE '%' || :raw || '%')
        ORDER BY e.createdAtEpochMs DESC
        """,
    )
    suspend fun searchWithTags(fts: String, raw: String): List<EntryWithTags>

    @Query("SELECT createdAtEpochMs FROM entries")
    suspend fun allCreatedAt(): List<Long>
}

@Database(
    entities = [EntryEntity::class, EntryFts::class, TagEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao
}
