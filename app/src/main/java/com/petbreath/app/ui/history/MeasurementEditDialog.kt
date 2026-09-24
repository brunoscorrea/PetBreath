package com.petbreath.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.petbreath.app.R
import com.petbreath.app.data.db.MeasurementContext
import com.petbreath.app.data.db.MeasurementEntity
import com.petbreath.app.domain.RespiratoryRate
import com.petbreath.app.ui.formatDate
import com.petbreath.app.ui.formatTime
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Add or edit a single reading. Editing changes the stored breaths/minute
 * directly; the original tap count is kept only when the value is unchanged.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementEditDialog(
    initial: MeasurementEntity,
    isNew: Boolean,
    onSave: (MeasurementEntity) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    var bpmText by rememberSaveable { mutableStateOf(if (isNew) "" else initial.bpm.toString()) }
    var takenAt by rememberSaveable { mutableLongStateOf(initial.takenAt) }
    var context by rememberSaveable { mutableStateOf(initial.context) }
    var notes by rememberSaveable { mutableStateOf(initial.notes) }
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }

    val bpm = bpmText.toIntOrNull()
    val bpmValid = bpm != null && RespiratoryRate.isPlausible(bpm)
    val inFuture = takenAt > System.currentTimeMillis() + 60_000

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isNew) R.string.measurement_add_title else R.string.measurement_edit_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = bpmText,
                    onValueChange = { v -> bpmText = v.filter(Char::isDigit).take(3) },
                    label = { Text(stringResource(R.string.breaths_per_minute)) },
                    isError = bpmText.isNotEmpty() && !bpmValid,
                    supportingText = if (bpmText.isNotEmpty() && !bpmValid) {
                        { Text(stringResource(R.string.error_bpm_range, RespiratoryRate.MAX_PLAUSIBLE_BPM)) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showDate = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                        Text(" " + formatDate(takenAt))
                    }
                    val local = Instant.ofEpochMilli(takenAt).atZone(zone)
                    OutlinedButton(onClick = { showTime = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Schedule, contentDescription = null)
                        Text(" " + formatTime(local.hour, local.minute))
                    }
                }
                if (inFuture) {
                    Text(stringResource(R.string.error_future_date), color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = context == MeasurementContext.SLEEPING,
                        onClick = { context = MeasurementContext.SLEEPING },
                        label = { Text(stringResource(R.string.context_sleeping)) },
                    )
                    FilterChip(
                        selected = context == MeasurementContext.RESTING,
                        onClick = { context = MeasurementContext.RESTING },
                        label = { Text(stringResource(R.string.context_resting)) },
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it.take(500) },
                    label = { Text(stringResource(R.string.field_notes_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(R.string.action_delete_measurement), color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = bpmValid && !inFuture,
                onClick = {
                    val newBpm = bpm ?: return@TextButton
                    val keepCount = !isNew && newBpm == initial.bpm
                    onSave(
                        initial.copy(
                            bpm = newBpm,
                            takenAt = takenAt,
                            context = context,
                            notes = notes.trim(),
                            breathCount = if (keepCount) initial.breathCount else null,
                            durationSeconds = if (keepCount) initial.durationSeconds else null,
                        ),
                    )
                },
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )

    if (showDate) {
        val local = Instant.ofEpochMilli(takenAt).atZone(zone)
        // DatePicker works in UTC midnight millis.
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = local.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { utc ->
                        val date = Instant.ofEpochMilli(utc).atZone(ZoneOffset.UTC).toLocalDate()
                        takenAt = combine(date, local.toLocalTime(), zone)
                    }
                    showDate = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) { DatePicker(state = pickerState) }
    }

    if (showTime) {
        val local = Instant.ofEpochMilli(takenAt).atZone(zone)
        val timeState = rememberTimePickerState(
            initialHour = local.hour,
            initialMinute = local.minute,
            is24Hour = android.text.format.DateFormat.is24HourFormat(LocalContext.current),
        )
        AlertDialog(
            onDismissRequest = { showTime = false },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    takenAt = combine(local.toLocalDate(), LocalTime.of(timeState.hour, timeState.minute), zone)
                    showTime = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

private fun combine(date: LocalDate, time: LocalTime, zone: ZoneId): Long =
    ZonedDateTime.of(date, time.withSecond(0).withNano(0), zone).toInstant().toEpochMilli()
