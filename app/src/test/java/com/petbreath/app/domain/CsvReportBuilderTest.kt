package com.petbreath.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class CsvReportBuilderTest {

    private fun data(measurements: List<ReportMeasurement>) = ReportData(
        generatedAt = Instant.parse("2025-03-12T10:00:00Z"),
        pet = ReportPet("Rex", "Dog", "Boxer", "8 years", "30.0 kg", ""),
        range = NormalRange(30),
        periodStart = null,
        periodEnd = Instant.parse("2025-03-12T10:00:00Z"),
        measurements = measurements,
        medications = emptyList(),
    )

    private fun m(time: String, bpm: Int, notes: String = "", count: Int? = null, secs: Int? = null) = ReportMeasurement(
        takenAt = LocalDateTime.parse(time).toInstant(ZoneOffset.UTC),
        bpm = bpm,
        breathCount = count,
        durationSeconds = secs,
        context = "Sleeping",
        notes = notes,
    )

    @Test
    fun writesHeaderAndRowsSortedByTime() {
        val csv = CsvReportBuilder.build(
            data(listOf(m("2025-03-11T22:15", 34, count = 17, secs = 30), m("2025-03-10T21:00", 22))),
            ZoneOffset.UTC,
        )
        val lines = csv.split("\r\n")
        assertEquals(
            "Date,Time,Breaths per minute,Status,Breaths counted,Counting seconds,Context,Notes",
            lines[0],
        )
        assertEquals("2025-03-10,21:00,22,Within range,,,Sleeping,", lines[1])
        assertEquals("2025-03-11,22:15,34,Above range,17,30,Sleeping,", lines[2])
        assertEquals("", lines[3])
    }

    @Test
    fun quotesFieldsWithCommasQuotesAndNewlines() {
        assertEquals("\"a, b\"", CsvReportBuilder.escape("a, b"))
        assertEquals("\"say \"\"hi\"\"\"", CsvReportBuilder.escape("say \"hi\""))
        assertEquals("\"line1\nline2\"", CsvReportBuilder.escape("line1\nline2"))
        assertEquals("plain", CsvReportBuilder.escape("plain"))
    }

    @Test
    fun neutralisesSpreadsheetFormulas() {
        assertEquals("'=SUM(A1)", CsvReportBuilder.escape("=SUM(A1)"))
        assertEquals("'+1", CsvReportBuilder.escape("+1"))
        assertEquals("'@cmd", CsvReportBuilder.escape("@cmd"))
    }

    @Test
    fun usesProvidedTimeZone() {
        val csv = CsvReportBuilder.build(data(listOf(m("2025-03-11T23:30", 20))), ZoneOffset.ofHours(2))
        assertEquals("2025-03-12,01:30,20,Within range,,,Sleeping,", csv.split("\r\n")[1])
    }

    @Test
    fun statsAreComputedFromMeasurements() {
        val report = data(listOf(m("2025-03-10T21:00", 20), m("2025-03-11T21:00", 40)))
        assertEquals(30, report.stats.averageBpm)
        assertEquals(1, report.stats.aboveRangeCount)
    }
}
