package com.karoslabs.deardiary.domain

import org.junit.Assert.assertEquals
import org.junit.Test
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
}
