package com.karoslabs.deardiary.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class JournalEntry(
    val id: String,
    val title: String,
    val transcript: String,
    val createdAt: Instant,
    val durationMs: Long,
    val audioFileName: String?,
    val tags: List<String>,
)

/** Safe basenames for on-device audio/export files. Backup JSON is untrusted. */
object StorageNames {
    private val SAFE = Regex("^[A-Za-z0-9._-]{1,80}$")

    fun isSafeBasename(name: String): Boolean {
        if (name.isEmpty() || name == "." || name == "..") return false
        if (name.any { it == '/' || it == 92.toChar() || it == 0.toChar() }) return false
        return SAFE.matches(name)
    }

    fun requireSafeBasename(name: String): String {
        require(isSafeBasename(name)) { "illegal storage name: $name" }
        return name
    }

    fun exportZipName(id: String): String = "dear-diary-${requireSafeBasename(id)}.zip"
}

/** Tiny field reader for Vosk's {"text":"..."} / {"partial":"..."} lines. */
object VoskJson {
    fun text(json: String): String = field(json, "text")
    fun partial(json: String): String = field(json, "partial")

    fun field(json: String, key: String): String {
        val needle = "\"$key\""
        val keyAt = json.indexOf(needle)
        if (keyAt < 0) return ""
        val colon = json.indexOf(':', keyAt + needle.length)
        if (colon < 0) return ""
        val q1 = json.indexOf('"', colon + 1)
        if (q1 < 0) return ""
        val q2 = json.indexOf('"', q1 + 1)
        if (q2 < 0) return ""
        return json.substring(q1 + 1, q2).trim()
    }
}

enum class ThemeMode { System, Dark, Light }

object TitleGenerator {
    fun fromTranscript(transcript: String, createdAt: Instant, zone: ZoneId = ZoneId.systemDefault()): String {
        val cleaned = transcript.trim().replace(Regex("\\s+"), " ")
        if (cleaned.isNotEmpty()) {
            val firstSentence = cleaned.split(Regex("(?<=[.!?])\\s+")).first()
            return firstSentence.take(42).trim().trimEnd('.', ',', ';', ':')
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
        val hour = createdAt.atZone(zone).hour
        return when (hour) {
            in 5..11 -> "Morning notes"
            in 12..16 -> "Afternoon notes"
            in 17..21 -> "Evening notes"
            else -> "Late night notes"
        }
    }
}

object StreakCalculator {
    fun daysKept(entryDates: Collection<LocalDate>, today: LocalDate = LocalDate.now()): Int {
        val days = entryDates.toSet()
        if (days.isEmpty()) return 0
        var cursor = if (days.contains(today)) today else today.minusDays(1)
        if (!days.contains(cursor)) return 0
        var count = 0
        while (days.contains(cursor)) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }
}

data class HeatmapCell(
    val date: LocalDate,
    val count: Int,
    val level: Int,
)

object HeatmapBuilder {
    fun lastWeeks(entryDates: Collection<LocalDate>, weeks: Int = 20, today: LocalDate = LocalDate.now()): List<HeatmapCell> {
        val counts = entryDates.groupingBy { it }.eachCount()
        val start = today.minusWeeks((weeks - 1).toLong()).with(java.time.DayOfWeek.SUNDAY)
        val alignedStart = if (start.isAfter(today.minusWeeks((weeks - 1).toLong()))) {
            start.minusWeeks(1)
        } else start
        val cells = mutableListOf<HeatmapCell>()
        var day = alignedStart
        val total = weeks * 7
        repeat(total) {
            if (!day.isAfter(today)) {
                val count = counts[day] ?: 0
                cells += HeatmapCell(day, count, level(count))
            }
            day = day.plusDays(1)
        }
        return cells
    }

    fun level(count: Int): Int = when {
        count <= 0 -> 0
        count == 1 -> 1
        count == 2 -> 2
        count == 3 -> 3
        else -> 4
    }
}

object SearchHighlight {
    fun ftsQuery(raw: String): String {
        val token = raw.trim().replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()
        if (token.isEmpty()) return ""
        return token.split(Regex("\\s+")).joinToString(" ") { "$it*" }
    }
}

object ReflectionPrompts {
    val all = listOf(
        "What do you want to remember from this?",
        "What felt true when you said it out loud?",
        "If tomorrow-you reads this, what should they notice?",
        "What can stay unfinished until morning?",
        "Who or what are you grateful you named?",
        "What would a kinder sentence look like?",
        "What is one thing that can wait?",
    )

    fun forEntry(entryId: String): String {
        val index = (entryId.hashCode().toLong() and 0x7fffffffL).toInt() % all.size
        return all[index]
    }

    const val HONESTY_NOTE =
        "A fixed prompt. Android has no on-device writing model like Apple Intelligence, so dear diary keeps this honest and local."
}

object TimeFormat {
    private val time = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val dateTime = DateTimeFormatter.ofPattern("M/d/yyyy, h:mm a", Locale.US)
    private val monthDay = DateTimeFormatter.ofPattern("MMM d", Locale.US)

    fun timeOfDay(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
        instant.atZone(zone).format(time)

    fun fullStamp(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
        instant.atZone(zone).format(dateTime)

    fun duration(ms: Long): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val m = totalSec / 60
        val s = totalSec % 60
        return "$m:${s.toString().padStart(2, '0')}"
    }

    fun sectionLabel(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
        today -> "TODAY"
        today.minusDays(1) -> "YESTERDAY"
        else -> date.format(monthDay).uppercase(Locale.US)
    }

    fun bytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format(Locale.US, "%.2f GB", gb)
    }
}
