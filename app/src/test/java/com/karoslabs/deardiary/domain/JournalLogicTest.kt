package com.karoslabs.deardiary.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import com.karoslabs.deardiary.data.backup.BackupManager
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class JournalLogicTest {
    @Test
    fun streakCountsConsecutiveDaysEndingToday() {
        val today = LocalDate.of(2026, 8, 22)
        val days = listOf(today, today.minusDays(1), today.minusDays(2), today.minusDays(5))
        assertEquals(3, StreakCalculator.daysKept(days, today))
    }

    @Test
    fun streakAllowsYesterdayIfTodayEmpty() {
        val today = LocalDate.of(2026, 8, 22)
        val days = listOf(today.minusDays(1), today.minusDays(2))
        assertEquals(2, StreakCalculator.daysKept(days, today))
    }

    @Test
    fun streakIsZeroWhenBroken() {
        val today = LocalDate.of(2026, 8, 22)
        assertEquals(0, StreakCalculator.daysKept(listOf(today.minusDays(3)), today))
    }

    @Test
    fun titleUsesFirstSentence() {
        val title = TitleGenerator.fromTranscript(
            "woke up before the alarm again. coffee went cold.",
            Instant.parse("2026-07-24T11:00:00Z"),
            ZoneOffset.UTC,
        )
        assertEquals("Woke up before the alarm again", title)
    }

    @Test
    fun titleFallsBackToTimeOfDay() {
        val title = TitleGenerator.fromTranscript(
            "   ",
            Instant.parse("2026-07-24T11:00:00Z"),
            ZoneOffset.UTC,
        )
        assertEquals("Morning notes", title)
    }

    @Test
    fun ftsQueryIsPrefixSafe() {
        assertEquals("amma*", SearchHighlight.ftsQuery("amma"))
        assertEquals("", SearchHighlight.ftsQuery("   "))
    }

    @Test
    fun durationFormatsMinutesAndSeconds() {
        assertEquals("0:05", TimeFormat.duration(5_000))
        assertEquals("1:02", TimeFormat.duration(62_000))
    }

    @Test
    fun heatmapLevelsIncreaseWithCount() {
        assertEquals(0, HeatmapBuilder.level(0))
        assertEquals(1, HeatmapBuilder.level(1))
        assertEquals(4, HeatmapBuilder.level(9))
    }

    @Test
    fun storageNamesAcceptsUuidAudioBasename() {
        val name = "8f14e45f-ceea-467c-9d73-aa7d99b4db0f.m4a"
        assertEquals(name, StorageNames.requireSafeBasename(name))
        assertEquals(true, StorageNames.isSafeBasename(name))
    }

    @Test
    fun storageNamesRejectsPathTraversal() {
        assertEquals(false, StorageNames.isSafeBasename("../secret.m4a"))
        assertEquals(false, StorageNames.isSafeBasename("/tmp/x.m4a"))
        assertEquals(false, StorageNames.isSafeBasename("foo/bar.m4a"))
        assertEquals(false, StorageNames.isSafeBasename("foo\\\\bar.m4a"))
        assertEquals(false, StorageNames.isSafeBasename(".."))
        assertEquals(false, StorageNames.isSafeBasename(""))
        assertEquals(false, StorageNames.isSafeBasename("evil.zip\u0000.m4a"))
    }

    @Test
    fun exportZipNameUsesOnlySafeId() {
        val id = "8f14e45f-ceea-467c-9d73-aa7d99b4db0f"
        assertEquals("dear-diary-$id.zip", StorageNames.exportZipName(id))
    }

    @Test(expected = IllegalArgumentException::class)
    fun exportZipNameRejectsTraversalId() {
        StorageNames.exportZipName("../passwd")
    }

    @Test
    fun backupDecodeDropsTraversalEntriesAndAudioNames() {
        val json = """
            {"version":1,"entries":[
              {"id":"8f14e45f-ceea-467c-9d73-aa7d99b4db0f","title":"ok","transcript":"hi","createdAt":"2026-08-22T12:00:00Z","durationMs":1000,"audioFile":"8f14e45f-ceea-467c-9d73-aa7d99b4db0f.m4a","tags":[]},
              {"id":"../escape","title":"bad","transcript":"no","createdAt":"2026-08-22T12:00:00Z","durationMs":1,"audioFile":"ok.m4a","tags":[]},
              {"id":"9f14e45f-ceea-467c-9d73-aa7d99b4db0f","title":"audio-bad","transcript":"x","createdAt":"2026-08-22T12:00:00Z","durationMs":1,"audioFile":"../../tmp/x.m4a","tags":[]}
            ]}
        """.trimIndent()
        val entries = BackupManager.decodeEntries(json)
        assertEquals(2, entries.size)
        assertEquals("8f14e45f-ceea-467c-9d73-aa7d99b4db0f", entries[0].id)
        assertEquals("8f14e45f-ceea-467c-9d73-aa7d99b4db0f.m4a", entries[0].audioFileName)
        assertEquals("9f14e45f-ceea-467c-9d73-aa7d99b4db0f", entries[1].id)
        assertEquals(null, entries[1].audioFileName)
    }

    @Test
    fun backupEncodeDecodeRoundTripKeepsSafeEntry() {
        val original = listOf(
            JournalEntry(
                id = "8f14e45f-ceea-467c-9d73-aa7d99b4db0f",
                title = "Morning notes",
                transcript = "hi",
                createdAt = Instant.parse("2026-08-22T12:00:00Z"),
                durationMs = 1500,
                audioFileName = "8f14e45f-ceea-467c-9d73-aa7d99b4db0f.m4a",
                tags = listOf("home"),
            ),
        )
        val json = org.json.JSONObject().apply {
            put("version", 1)
            put("entries", org.json.JSONArray().put(org.json.JSONObject().apply {
                put("id", original[0].id)
                put("title", original[0].title)
                put("transcript", original[0].transcript)
                put("createdAt", original[0].createdAt.toString())
                put("durationMs", original[0].durationMs)
                put("audioFile", original[0].audioFileName)
                put("tags", org.json.JSONArray().put("home"))
            }))
        }.toString()
        val back = BackupManager.decodeEntries(json)
        assertEquals(1, back.size)
        assertEquals(original[0].id, back[0].id)
        assertEquals(original[0].audioFileName, back[0].audioFileName)
        assertEquals(original[0].transcript, back[0].transcript)
    }
}
