package com.petbreath.app.ui.pet

import android.content.ActivityNotFoundException
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petbreath.app.R
import com.petbreath.app.data.db.MeasurementContext
import com.petbreath.app.data.db.MeasurementEntity
import com.petbreath.app.data.db.PetEntity
import com.petbreath.app.data.db.normalRange
import com.petbreath.app.data.settings.WeightUnit
import com.petbreath.app.domain.MeasurementStats
import com.petbreath.app.export.ReportExporter
import com.petbreath.app.export.ReportFormat
import com.petbreath.app.ui.components.ChartPoint
import com.petbreath.app.ui.components.ConfirmDeleteDialog
import com.petbreath.app.ui.components.DisclaimerBanner
import com.petbreath.app.ui.components.EmptyState
import com.petbreath.app.ui.components.PetBreathTopBar
import com.petbreath.app.ui.components.SectionHeader
import com.petbreath.app.ui.components.StatusChip
import com.petbreath.app.ui.components.TrendChart
import com.petbreath.app.ui.components.rangeText
import com.petbreath.app.ui.components.speciesLabel
import com.petbreath.app.ui.formatDateTime
import com.petbreath.app.ui.history.MeasurementEditDialog
import java.time.Instant

@Composable
fun PetDetailScreen(
    onBack: () -> Unit,
    onMeasure: () -> Unit,
    onEditPet: () -> Unit,
    onMedications: () -> Unit,
    onReminders: () -> Unit,
    viewModel: PetDetailViewModel = viewModel(factory = PetDetailViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDeletePet by rememberSaveable { mutableStateOf(false) }
    var showExport by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<MeasurementEntity?>(null) }
    var confirmDeleteMeasurement by remember { mutableStateOf<MeasurementEntity?>(null) }
    val currentOnBack by rememberUpdatedState(onBack)

    val deletedMessage = stringResource(R.string.measurement_deleted)
    val undoLabel = stringResource(R.string.action_undo)
    val exportFailed = stringResource(R.string.export_failed)
    val noShareApp = stringResource(R.string.export_no_app)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is PetDetailEvent.Share -> try {
                    context.startActivity(event.intent)
                } catch (_: ActivityNotFoundException) {
                    snackbar.showSnackbar(noShareApp)
                }
                PetDetailEvent.ExportFailed -> snackbar.showSnackbar(exportFailed)
                PetDetailEvent.PetDeleted -> currentOnBack()
                is PetDetailEvent.MeasurementDeleted -> {
                    val result = snackbar.showSnackbar(deletedMessage, actionLabel = undoLabel, withDismissAction = true)
                    if (result == SnackbarResult.ActionPerformed) viewModel.restoreMeasurement(event.measurement)
                }
            }
        }
    }

    val pet = state.pet
    Scaffold(
        topBar = {
            PetBreathTopBar(
                title = pet?.name.orEmpty(),
                onBack = onBack,
                actions = {
                    IconButton(onClick = onEditPet, enabled = pet != null) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit_pet))
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_more))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_export)) },
                                leadingIcon = { Icon(Icons.Filled.IosShare, null) },
                                onClick = { menuOpen = false; showExport = true },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_delete_pet)) },
                                leadingIcon = { Icon(Icons.Filled.Delete, null) },
                                onClick = { menuOpen = false; confirmDeletePet = true },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (pet != null) {
                ExtendedFloatingActionButton(
                    onClick = onMeasure,
                    icon = { Icon(Icons.Filled.Air, contentDescription = null) },
                    text = { Text(stringResource(R.string.action_measure_now)) },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (pet == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (state.loading) CircularProgressIndicator()
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        ) {
            item { PetInfoCard(pet, state.settings.weightUnit) }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                ) {
                    item {
                        AssistChip(
                            onClick = onMedications,
                            label = { Text(stringResource(R.string.medications_title)) },
                            leadingIcon = { Icon(Icons.Filled.Medication, null) },
                        )
                    }
                    item {
                        AssistChip(
                            onClick = onReminders,
                            label = { Text(stringResource(R.string.reminders_title)) },
                            leadingIcon = { Icon(Icons.Filled.NotificationsActive, null) },
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { showExport = true },
                            label = { Text(stringResource(R.string.action_export_short)) },
                            leadingIcon = { Icon(Icons.Filled.IosShare, null) },
                        )
                    }
                }
            }
            item { SectionHeader(stringResource(R.string.trend_title)) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChartPeriod.entries.forEach { p ->
                        FilterChip(
                            selected = state.period == p,
                            onClick = { viewModel.setPeriod(p) },
                            label = {
                                Text(
                                    stringResource(
                                        when (p) {
                                            ChartPeriod.WEEK -> R.string.period_7d
                                            ChartPeriod.MONTH -> R.string.period_30d
                                            ChartPeriod.QUARTER -> R.string.period_90d
                                            ChartPeriod.ALL -> R.string.period_all
                                        },
                                    ),
                                )
                            },
                        )
                    }
                }
            }
            item {
                val chartData = state.chartMeasurements
                Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        if (chartData.isEmpty()) {
                            Text(stringResource(R.string.chart_empty), style = MaterialTheme.typography.bodyMedium)
                        } else {
                            TrendChart(
                                points = chartData.map { ChartPoint(Instant.ofEpochMilli(it.takenAt), it.bpm) },
                                range = pet.normalRange,
                            )
                            val stats = MeasurementStats.from(chartData.map { it.bpm }, pet.normalRange)
                            Spacer(Modifier.size(8.dp))
                            Text(
                                stringResource(
                                    R.string.stats_line,
                                    stats.averageBpm ?: 0, stats.minBpm ?: 0, stats.maxBpm ?: 0, stats.count,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            if (stats.aboveRangeCount > 0) {
                                Text(
                                    stringResource(R.string.stats_above, stats.aboveRangeCount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionHeader(stringResource(R.string.history_title), Modifier.weight(1f))
                    TextButton(onClick = {
                        editing = MeasurementEntity(petId = pet.id, takenAt = System.currentTimeMillis(), bpm = 0)
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(stringResource(R.string.action_add_manually))
                    }
                }
            }
            if (state.measurements.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.AutoMirrored.Filled.ShowChart,
                        title = stringResource(R.string.history_empty_title),
                        body = stringResource(R.string.history_empty_body),
                    )
                }
            }
            items(state.measurements, key = { it.id }) { m ->
                MeasurementRow(m, pet, onClick = { editing = m })
                HorizontalDivider()
            }
            item { DisclaimerBanner(Modifier.padding(top = 16.dp)) }
        }
    }

    editing?.let { m ->
        MeasurementEditDialog(
            initial = m,
            isNew = m.id == 0L,
            onSave = { viewModel.saveMeasurement(it); editing = null },
            onDelete = if (m.id == 0L) null else ({ confirmDeleteMeasurement = m; editing = null }),
            onDismiss = { editing = null },
        )
    }
    confirmDeleteMeasurement?.let { m ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.measurement_delete_title),
            message = stringResource(R.string.measurement_delete_body, m.bpm, formatDateTime(m.takenAt)),
            onConfirm = { viewModel.deleteMeasurement(m); confirmDeleteMeasurement = null },
            onDismiss = { confirmDeleteMeasurement = null },
        )
    }
    if (confirmDeletePet && pet != null) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.pet_delete_title, pet.name),
            message = stringResource(R.string.pet_delete_body),
            onConfirm = { confirmDeletePet = false; viewModel.deletePet() },
            onDismiss = { confirmDeletePet = false },
        )
    }
    if (showExport) {
        ExportDialog(
            exporting = state.exporting,
            onExport = { format, days -> viewModel.export(format, days); showExport = false },
            onDismiss = { showExport = false },
        )
    }
}

@Composable
private fun PetInfoCard(pet: PetEntity, weightUnit: WeightUnit) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val subtitle = listOf(
                speciesLabel(pet.species),
                pet.breed,
                ReportExporter.ageText(context, pet, Instant.now()),
                ReportExporter.weightText(context, pet.weightKg, weightUnit),
            ).filter { it.isNotBlank() }.joinToString(" · ")
            Text(subtitle, style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.pet_normal_range, rangeText(pet.normalRange)),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (pet.medicalNotes.isNotBlank()) {
                Text(
                    pet.medicalNotes,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MeasurementRow(m: MeasurementEntity, pet: PetEntity, onClick: () -> Unit) {
    val contextLabel = stringResource(
        if (m.context == MeasurementContext.SLEEPING) R.string.context_sleeping else R.string.context_resting,
    )
    ListItem(
        modifier = Modifier.clickable(onClickLabel = stringResource(R.string.action_edit), onClick = onClick),
        headlineContent = {
            Text(stringResource(R.string.bpm_value, m.bpm), fontWeight = FontWeight.SemiBold)
        },
        supportingContent = {
            Text(
                listOf(formatDateTime(m.takenAt), contextLabel, m.notes).filter { it.isNotBlank() }.joinToString(" · "),
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        },
        trailingContent = { StatusChip(pet.normalRange.classify(m.bpm)) },
    )
}

@Composable
private fun ExportDialog(
    exporting: Boolean,
    onExport: (ReportFormat, Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    var format by rememberSaveable { mutableStateOf(ReportFormat.PDF) }
    var days by rememberSaveable { mutableStateOf<Int?>(90) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.export_title)) },
        text = {
            Column {
                Text(stringResource(R.string.export_body), style = MaterialTheme.typography.bodyMedium)
                SectionHeader(stringResource(R.string.export_format))
                Column(Modifier.selectableGroup()) {
                    RadioRow(stringResource(R.string.export_pdf), format == ReportFormat.PDF) { format = ReportFormat.PDF }
                    RadioRow(stringResource(R.string.export_csv), format == ReportFormat.CSV) { format = ReportFormat.CSV }
                }
                SectionHeader(stringResource(R.string.export_period))
                Column(Modifier.selectableGroup()) {
                    RadioRow(stringResource(R.string.period_30d_long), days == 30) { days = 30 }
                    RadioRow(stringResource(R.string.period_90d_long), days == 90) { days = 90 }
                    RadioRow(stringResource(R.string.period_all_long), days == null) { days = null }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onExport(format, days) }, enabled = !exporting) {
                Text(stringResource(R.string.action_share))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
fun RadioRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}
