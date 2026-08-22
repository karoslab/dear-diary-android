package com.karoslabs.deardiary.data.backup

import com.karoslabs.deardiary.data.audio.AudioStorage
import com.karoslabs.deardiary.domain.JournalEntry
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ImportedBackup(
    val entries: List<JournalEntry>,
    val audioFiles: Map<String, File>,
)

class BackupManager(private val storage: AudioStorage) {

    fun exportAll(entries: List<JournalEntry>, destination: File) {
        destination.parentFile?.mkdirs()
        ZipOutputStream(destination.outputStream().buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("journal.json"))
            zip.write(encode(entries).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            for (entry in entries) {
                val name = entry.audioFileName ?: continue
                val file = storage.fileFor(name)
                if (!file.exists()) continue
                zip.putNextEntry(ZipEntry("audio/$name"))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    fun exportOne(entry: JournalEntry, destination: File) {
        exportAll(listOf(entry), destination)
    }

    fun importFrom(stream: InputStream, stagingDir: File): ImportedBackup {
        stagingDir.mkdirs()
        var json: String? = null
        val stagedAudio = mutableMapOf<String, File>()
        ZipInputStream(stream.buffered()).use { zip ->
            var item = zip.nextEntry
            while (item != null) {
                val name = item.name.trimStart('/')
                if (!item.isDirectory) {
                    when {
                        name == "journal.json" || name.endsWith("/journal.json") -> {
                            json = zip.readBytes().toString(Charsets.UTF_8)
                        }
                        name.startsWith("audio/") -> {
                            val fileName = name.removePrefix("audio/").substringAfterLast('/')
                            if (fileName.isNotBlank()) {
                                val dest = File(stagingDir, fileName)
                                dest.outputStream().use { zip.copyTo(it) }
                                stagedAudio[fileName] = dest
                            }
                        }
                    }
                }
                zip.closeEntry()
                item = zip.nextEntry
            }
        }
        val payload = json ?: error("Backup is missing journal.json")
        val entries = decode(payload)
        return ImportedBackup(entries, stagedAudio)
    }

    fun encode(entries: List<JournalEntry>): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("app", "dear diary")
        root.put("package", "com.karoslabs.deardiary")
        root.put("exportedAt", Instant.now().toString())
        val array = JSONArray()
        for (entry in entries) {
            val obj = JSONObject()
            obj.put("id", entry.id)
            obj.put("title", entry.title)
            obj.put("transcript", entry.transcript)
            obj.put("createdAt", entry.createdAt.toString())
            obj.put("durationMs", entry.durationMs)
            obj.put("audioFile", entry.audioFileName)
            val tags = JSONArray()
            entry.tags.forEach { tags.put(it) }
            obj.put("tags", tags)
            array.put(obj)
        }
        root.put("entries", array)
        return root.toString(2)
    }

    fun decode(json: String): List<JournalEntry> {
        val root = JSONObject(json)
        val array = root.optJSONArray("entries") ?: JSONArray()
        val out = mutableListOf<JournalEntry>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val tagsJson = obj.optJSONArray("tags") ?: JSONArray()
            val tags = buildList {
                for (t in 0 until tagsJson.length()) add(tagsJson.getString(t))
            }
            val created = obj.optString("createdAt").takeIf { it.isNotBlank() }?.let {
                runCatching { Instant.parse(it) }.getOrNull()
            } ?: Instant.ofEpochMilli(obj.optLong("createdAtEpochMs"))
            out += JournalEntry(
                id = obj.getString("id"),
                title = obj.optString("title"),
                transcript = obj.optString("transcript"),
                createdAt = created,
                durationMs = obj.optLong("durationMs"),
                audioFileName = obj.optString("audioFile").takeIf { it.isNotBlank() && it != "null" },
                tags = tags,
            )
        }
        return out
    }
}
