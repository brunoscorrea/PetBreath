package com.petbreath.app.ui

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
private val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

fun formatDateTime(millis: Long): String =
    dateTimeFormatter.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

fun formatDate(millis: Long): String =
    dateFormatter.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

fun formatTime(hour: Int, minute: Int): String =
    timeFormatter.format(java.time.LocalTime.of(hour, minute))

/** "5 minutes ago", "Yesterday, 21:30", … using the platform's localized rules. */
@Composable
fun formatRelativeDateTime(millis: Long): String {
    val context = LocalContext.current
    return DateUtils.getRelativeDateTimeString(
        context,
        millis,
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.WEEK_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
}
