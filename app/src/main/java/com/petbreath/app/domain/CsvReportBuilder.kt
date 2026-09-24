package com.petbreath.app.domain

import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Builds a spreadsheet-friendly CSV export (RFC 4180 quoting) of a pet's
 * resting respiratory rate history.
 */
object CsvReportBuilder {
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

    val HEADER = listOf(
        "Date", "Time", "Breaths per minute", "Status", "Breaths counted",
        "Counting seconds", "Context", "Notes",
    )

    fun build(data: ReportData, zone: ZoneId): String {
        val sb = StringBuilder()
        sb.appendRow(HEADER)
        data.measurements.sortedBy { it.takenAt }.forEach { m ->
            val local = m.takenAt.atZone(zone)
            sb.appendRow(
                listOf(
                    dateFormat.format(local),
                    timeFormat.format(local),
                    m.bpm.toString(),
                    statusLabel(data.range.classify(m.bpm)),
                    m.breathCount?.toString().orEmpty(),
                    m.durationSeconds?.toString().orEmpty(),
                    m.context,
                    m.notes,
                ),
            )
        }
        return sb.toString()
    }

    fun statusLabel(status: RangeStatus): String = when (status) {
        RangeStatus.NORMAL -> "Within range"
        RangeStatus.ABOVE -> "Above range"
        RangeStatus.BELOW -> "Below range"
    }

    fun escape(field: String): String {
        // Neutralise spreadsheet formula injection for user-entered text.
        val safe = if (field.isNotEmpty() && field[0] in "=+-@\t\r") "'$field" else field
        return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + safe.replace("\"", "\"\"") + "\""
        } else {
            safe
        }
    }

    private fun StringBuilder.appendRow(fields: List<String>) {
        fields.joinTo(this, separator = ",") { escape(it) }
        append("\r\n")
    }
}
