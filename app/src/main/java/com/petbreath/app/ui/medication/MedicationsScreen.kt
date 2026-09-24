package com.petbreath.app.ui.medication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petbreath.app.R
import com.petbreath.app.data.db.MedicationEntity
import com.petbreath.app.ui.components.ConfirmDeleteDialog
import com.petbreath.app.ui.components.EmptyState
import com.petbreath.app.ui.components.PetBreathTopBar
import com.petbreath.app.ui.formatDateTime
import com.petbreath.app.ui.formatTime
import com.petbreath.app.ui.reminder.NotificationPermissionBanner

@Composable
fun MedicationsScreen(
    onBack: () -> Unit,
    viewModel: MedicationsViewModel = viewModel(factory = MedicationsViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<MedicationItem?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<MedicationEntity?>(null) }

    Scaffold(
        topBar = {
            PetBreathTopBar(
                title = state.pet?.let { stringResource(R.string.medications_title_pet, it.name) }
                    ?: stringResource(R.string.medications_title),
                onBack = onBack,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(stringResource(R.string.action_add_medication)) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { NotificationPermissionBanner() }
            if (!state.loading && state.items.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Medication,
                        title = stringResource(R.string.medications_empty_title),
                        body = stringResource(R.string.medications_empty_body),
                    )
                }
            }
            items(state.items, key = { it.medication.id }) { item ->
                MedicationCard(
                    item = item,
                    onEdit = { editing = item },
                    onMarkGiven = { viewModel.markGiven(item.medication.id) },
                    onUndoDose = viewModel::undoDose,
                )
            }
        }
    }

    if (creating || editing != null) {
        val item = editing
        MedicationEditDialog(
            initial = item?.medication,
            initialTimes = item?.reminderTimes.orEmpty(),
            onSave = { med, times ->
                viewModel.save(med, times)
                creating = false
                editing = null
            },
            onDelete = item?.let { { deleting = it.medication; editing = null } },
            onDismiss = { creating = false; editing = null },
        )
    }
    deleting?.let { med ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.medication_delete_title, med.name),
            message = stringResource(R.string.medication_delete_body),
            onConfirm = { viewModel.delete(med.id); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MedicationCard(
    item: MedicationItem,
    onEdit: () -> Unit,
    onMarkGiven: () -> Unit,
    onUndoDose: (Long) -> Unit,
) {
    val med = item.medication
    Card(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Medication, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(med.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (!med.active) {
                    Text(stringResource(R.string.medication_inactive), style = MaterialTheme.typography.labelMedium)
                }
            }
            if (med.dosage.isNotBlank()) Text(med.dosage, style = MaterialTheme.typography.bodyLarge)
            if (med.instructions.isNotBlank()) {
                Text(med.instructions, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (item.reminderTimes.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Alarm, contentDescription = stringResource(R.string.medication_reminder_times))
                    Spacer(Modifier.width(6.dp))
                    Text(item.reminderTimes.joinToString(", ") { (h, m) -> formatTime(h, m) })
                }
            }
            val lastDose = item.recentDoses.firstOrNull()
            Text(
                if (lastDose == null) {
                    stringResource(R.string.medication_no_recent_dose)
                } else {
                    stringResource(R.string.medication_last_given, formatDateTime(lastDose.givenAt))
                },
                style = MaterialTheme.typography.bodySmall,
            )
            if (med.active) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(onClick = onMarkGiven) {
                        Icon(Icons.Filled.Check, null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.action_mark_given))
                    }
                    if (lastDose != null && System.currentTimeMillis() - lastDose.givenAt < UNDO_WINDOW_MILLIS) {
                        TextButton(onClick = { onUndoDose(lastDose.id) }) { Text(stringResource(R.string.action_undo)) }
                    }
                }
            }
        }
    }
}

private const val UNDO_WINDOW_MILLIS = 10 * 60 * 1000L

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MedicationEditDialog(
    initial: MedicationEntity?,
    initialTimes: List<Pair<Int, Int>>,
    onSave: (MedicationEntity, List<Pair<Int, Int>>) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
    var dosage by rememberSaveable { mutableStateOf(initial?.dosage.orEmpty()) }
    var instructions by rememberSaveable { mutableStateOf(initial?.instructions.orEmpty()) }
    var active by rememberSaveable { mutableStateOf(initial?.active ?: true) }
    val times = remember { mutableStateListOf<Pair<Int, Int>>().apply { addAll(initialTimes) } }
    var pickingTime by rememberSaveable { mutableStateOf(false) }
    var nameError by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.medication_add_title else R.string.medication_edit_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(80); nameError = false },
                    label = { Text(stringResource(R.string.field_medication_name)) },
                    isError = nameError,
                    supportingText = if (nameError) ({ Text(stringResource(R.string.error_name_required)) }) else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it.take(80) },
                    label = { Text(stringResource(R.string.field_dosage)) },
                    placeholder = { Text(stringResource(R.string.field_dosage_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it.take(500) },
                    label = { Text(stringResource(R.string.field_instructions)) },
                    placeholder = { Text(stringResource(R.string.field_instructions_hint)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.field_active), modifier = Modifier.weight(1f))
                    Switch(checked = active, onCheckedChange = { active = it })
                }
                Text(stringResource(R.string.medication_reminder_times), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    times.forEach { t ->
                        val label = formatTime(t.first, t.second)
                        val removeLabel = stringResource(R.string.action_remove_time, label)
                        InputChip(
                            selected = false,
                            onClick = { times.remove(t) },
                            label = { Text(label) },
                            trailingIcon = { Icon(Icons.Filled.Close, contentDescription = null) },
                            modifier = Modifier.semantics { contentDescription = removeLabel },
                        )
                    }
                }
                OutlinedButton(onClick = { pickingTime = true }) {
                    Icon(Icons.Filled.Alarm, null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_add_reminder_time))
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(R.string.action_delete_medication), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) {
                    nameError = true
                } else {
                    val base = initial ?: MedicationEntity(petId = 0, name = "", createdAt = 0)
                    onSave(
                        base.copy(name = name.trim(), dosage = dosage.trim(), instructions = instructions.trim(), active = active),
                        times.toList(),
                    )
                }
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )

    if (pickingTime) {
        val timeState = rememberTimePickerState(
            initialHour = 8,
            initialMinute = 0,
            is24Hour = android.text.format.DateFormat.is24HourFormat(LocalContext.current),
        )
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = timeState) } },
            confirmButton = {
                TextButton(onClick = {
                    val t = timeState.hour to timeState.minute
                    val sorted = (times + t).distinct().sortedWith(compareBy({ it.first }, { it.second }))
                    times.clear()
                    times.addAll(sorted)
                    pickingTime = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { pickingTime = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
