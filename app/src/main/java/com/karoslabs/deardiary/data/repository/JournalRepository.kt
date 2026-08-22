package com.karoslabs.deardiary.data.repository

import android.os.StatFs
import com.karoslabs.deardiary.data.audio.AudioStorage
import com.karoslabs.deardiary.data.backup.BackupManager
import com.karoslabs.deardiary.data.db.EntryEntity
import com.karoslabs.deardiary.data.db.EntryWithTags
import com.karoslabs.deardiary.data.db.JournalDao
import com.karoslabs.deardiary.data.db.TagEntity
import com.karoslabs.deardiary.domain.HeatmapBuilder
import com.karoslabs.deardiary.domain.JournalEntry
import com.karoslabs.deardiary.domain.SearchHighlight
import com.karoslabs.deardiary.domain.StreakCalculator
import com.karoslabs.deardiary.domain.TitleGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.InputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class DeviceStorageStats(
    val freeBytes: Long,
    val totalBytes: Long,
    val appUsedBytes: Long,
    val audioBytes: Long,
    val entryCount: Int,
)

class JournalRepository(
    private val dao: JournalDao,
    private val storage: AudioStorage,
    private val backup: BackupManager,
) {
    val entries: Flow<List<JournalEntry>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observe(id: String): Flow<JournalEntry?> = dao.observeById(id).map { it?.toDomain() }

    suspend fun get(id: String): JournalEntry? = dao.getById(id)?.toDomain()

    suspend fun saveNew(
        transcript: String,
        durationMs: Long,
        tempAudio: File?,
        createdAt: Instant = Instant.now(),
        tags: List<String> = emptyList(),
    ): JournalEntry {
        val id = UUID.randomUUID().toString()
        val audioName = if (tempAudio != null && tempAudio.exists()) {
            val name = storage.newFileName(id)
            tempAudio.copyTo(storage.fileFor(name), overwrite = true)
            tempAudio.delete()
            name
        } else null
        val title = TitleGenerator.fromTranscript(transcript, createdAt)
        val entry = JournalEntry(
            id = id,
            title = title,
            transcript = transcript,
            createdAt = createdAt,
            durationMs = durationMs,
            audioFileName = audioName,
            tags = tags,
        )
        persist(entry)
        return entry
    }

    suspend fun updateTranscript(id: String, transcript: String) {
        val current = dao.getById(id) ?: return
        val updated = current.entry.copy(transcript = transcript)
        dao.upsertEntry(updated)
    }

    suspend fun updateTitle(id: String, title: String) {
        val current = dao.getById(id) ?: return
        dao.upsertEntry(current.entry.copy(title = title))
    }

    suspend fun setTags(id: String, tags: List<String>) {
        dao.deleteTagsFor(id)
        dao.upsertTags(
            tags.map { raw ->
                val name = raw.trim().lowercase()
                TagEntity(id = UUID.randomUUID().toString(), entryId = id, name = name)
            }.filter { it.name.isNotEmpty() },
        )
    }

    suspend fun addTag(id: String, tag: String) {
        val current = get(id) ?: return
        val name = tag.trim().lowercase()
        if (name.isEmpty() || current.tags.contains(name)) return
        setTags(id, current.tags + name)
    }

    suspend fun removeTag(id: String, tag: String) {
        val current = get(id) ?: return
        setTags(id, current.tags.filterNot { it.equals(tag, ignoreCase = true) })
    }

    suspend fun delete(id: String) {
        val current = dao.getById(id)
        storage.deleteAudio(current?.entry?.audioFileName)
        dao.deleteEntry(id)
    }

    suspend fun wipeEverything() {
        storage.deleteAllAudio()
        dao.deleteAllTags()
        dao.deleteAllEntries()
    }

    suspend fun search(query: String): List<JournalEntry> {
        val raw = query.trim()
        if (raw.isEmpty()) return emptyList()
        val fts = SearchHighlight.ftsQuery(raw)
        if (fts.isEmpty()) return emptyList()
        return dao.searchWithTags(fts, raw).map { it.toDomain() }
    }

    suspend fun streak(today: LocalDate = LocalDate.now()): Int {
        val dates = dao.allCreatedAt().map { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
        return StreakCalculator.daysKept(dates, today)
    }

    fun heatmap(entries: List<JournalEntry>, weeks: Int = 20, today: LocalDate = LocalDate.now()) =
        HeatmapBuilder.lastWeeks(
            entries.map { it.createdAt.atZone(ZoneId.systemDefault()).toLocalDate() },
            weeks,
            today,
        )

    suspend fun exportEverything(destination: File) {
        backup.exportAll(dao.getAll().map { it.toDomain() }, destination)
    }

    suspend fun exportOne(id: String, destination: File) {
        val entry = get(id) ?: error("Entry gone")
        backup.exportOne(entry, destination)
    }

    suspend fun importBackup(stream: InputStream) {
        val staging = File(storage.exportCacheDir, "import-${UUID.randomUUID()}")
        try {
            val imported = backup.importFrom(stream, staging)
            for (entry in imported.entries) {
                val audioName = entry.audioFileName
                if (audioName != null) {
                    val staged = imported.audioFiles[audioName]
                    if (staged != null && staged.exists()) {
                        staged.copyTo(storage.fileFor(audioName), overwrite = true)
                    }
                }
                persist(entry)
            }
        } finally {
            staging.deleteRecursively()
        }
    }

    suspend fun deviceStats(): DeviceStorageStats {
        val stat = StatFs(storage.audioDir.absolutePath)
        return DeviceStorageStats(
            freeBytes = stat.availableBytes,
            totalBytes = stat.totalBytes,
            appUsedBytes = storage.usedBytes(),
            audioBytes = storage.audioBytes(),
            entryCount = dao.getAll().size,
        )
    }

    suspend fun seedSampleJournal() {
        if (dao.getAll().isNotEmpty()) return
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        persist(
            JournalEntry(
                id = "sample-morning",
                title = "Morning before the rush",
                transcript = "Woke up before the alarm again and just lay there listening to the birds. I want to remember that the day does not actually start with email. It starts here, with coffee going cold because I forgot it, and a list of three things that actually matter. Today those are the design review, calling Amma, and a real lunch away from the desk.",
                createdAt = today.atTime(7, 0).atZone(zone).toInstant(),
                durationMs = 5_000,
                audioFileName = null,
                tags = listOf("morning", "work"),
            ),
        )
        persist(
            JournalEntry(
                id = "sample-amma",
                title = "Amma made obbattu",
                transcript = "Amma made obbattu and the whole house smelled like ghee and cardamom. I ate two before I remembered to say thank you.",
                createdAt = today.minusDays(1).atTime(8, 0).atZone(zone).toInstant(),
                durationMs = 0,
                audioFileName = null,
                tags = listOf("family", "food", "gratitude"),
            ),
        )
        persist(
            JournalEntry(
                id = "sample-rain",
                title = "I woke up before the alarm",
                transcript = "I woke up before the alarm and just listened to the rain for a while. It felt like the day was mine before anyone asked for it.",
                createdAt = today.minusDays(2).atTime(7, 30).atZone(zone).toInstant(),
                durationMs = 48_000,
                audioFileName = null,
                tags = listOf("quiet"),
            ),
        )
    }

    private suspend fun persist(entry: JournalEntry) {
        dao.upsertEntry(
            EntryEntity(
                id = entry.id,
                title = entry.title,
                transcript = entry.transcript,
                createdAtEpochMs = entry.createdAt.toEpochMilli(),
                durationMs = entry.durationMs,
                audioFileName = entry.audioFileName,
            ),
        )
        dao.deleteTagsFor(entry.id)
        if (entry.tags.isNotEmpty()) {
            dao.upsertTags(
                entry.tags.map {
                    TagEntity(id = UUID.randomUUID().toString(), entryId = entry.id, name = it)
                },
            )
        }
    }

    private fun EntryWithTags.toDomain() = JournalEntry(
        id = entry.id,
        title = entry.title,
        transcript = entry.transcript,
        createdAt = Instant.ofEpochMilli(entry.createdAtEpochMs),
        durationMs = entry.durationMs,
        audioFileName = entry.audioFileName,
        tags = tags.map { it.name },
    )
}
