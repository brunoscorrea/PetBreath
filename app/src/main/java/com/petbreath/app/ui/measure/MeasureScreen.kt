package com.petbreath.app.ui.measure

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petbreath.app.R
import com.petbreath.app.data.db.MeasurementContext
import com.petbreath.app.data.db.normalRange
import com.petbreath.app.domain.MeasurementMode
import com.petbreath.app.ui.components.PetBreathTopBar
import com.petbreath.app.ui.components.rangeText
import com.petbreath.app.ui.components.statusStyle
import kotlin.math.ceil

@Composable
fun MeasureScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: MeasureViewModel = viewModel(factory = MeasureViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    var confirmExit by rememberSaveable { mutableStateOf(false) }

    // Keep the display awake while counting so the screen never dims mid-measurement.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val currentOnSaved by rememberUpdatedState(onSaved)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                MeasureEvent.Finished -> if (viewModel.state.value.hapticsEnabled) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                MeasureEvent.Saved -> currentOnSaved()
            }
        }
    }

    val hasUnsavedWork = state.phase == MeasurePhase.RUNNING || state.phase == MeasurePhase.FINISHED
    BackHandler(enabled = hasUnsavedWork) { confirmExit = true }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text(stringResource(R.string.measure_discard_title)) },
            text = { Text(stringResource(R.string.measure_discard_body)) },
            confirmButton = {
                TextButton(onClick = { confirmExit = false; onBack() }) { Text(stringResource(R.string.action_discard)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmExit = false }) { Text(stringResource(R.string.action_keep)) }
            },
        )
    }

    Scaffold(
        topBar = {
            PetBreathTopBar(
                title = state.pet?.name?.let { stringResource(R.string.measure_title_pet, it) }
                    ?: stringResource(R.string.measure_title),
                onBack = { if (hasUnsavedWork) confirmExit = true else onBack() },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            ModeSelector(
                selected = state.mode,
                enabled = state.phase != MeasurePhase.RUNNING,
                onSelect = viewModel::setMode,
            )
            Spacer(Modifier.height(12.dp))
            AnimatedContent(
                targetState = state.phase == MeasurePhase.FINISHED,
                modifier = Modifier.weight(1f),
                label = "measure",
            ) { finished ->
                if (finished) {
                    ResultPanel(
                        state = state,
                        onContextChange = viewModel::setContext,
                        onNotesChange = viewModel::setNotes,
                        onSave = viewModel::save,
                        onRetry = viewModel::reset,
                        onDiscard = onBack,
                    )
                } else {
                    CounterPanel(
                        state = state,
                        onTap = {
                            if (state.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.tap()
                        },
                        onUndo = viewModel::undo,
                        onReset = viewModel::reset,
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeSelector(selected: MeasurementMode, enabled: Boolean, onSelect: (MeasurementMode) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        MeasurementMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                enabled = enabled,
                shape = SegmentedButtonDefaults.itemShape(index, MeasurementMode.entries.size),
            ) {
                Text(stringResource(R.string.mode_seconds, mode.seconds))
            }
        }
    }
}

@Composable
private fun CounterPanel(
    state: MeasureUiState,
    onTap: () -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit,
) {
    val running = state.phase == MeasurePhase.RUNNING
    val secondsLeft = ceil(state.remainingMillis / 1000.0).toInt()
    val totalMillis = state.mode.seconds * 1000f
    val currentOnTap by rememberUpdatedState(onTap)
    val tapLabel = stringResource(R.string.measure_tap_action)
    val tapDescription = if (running) {
        pluralStringResource(R.plurals.measure_a11y_running, state.breaths, state.breaths, secondsLeft)
    } else {
        stringResource(R.string.measure_a11y_idle)
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                // Count on finger-down for the fastest, most reliable response.
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        currentOnTap()
                    }
                }
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = tapDescription
                    onClick(label = tapLabel) { currentOnTap(); true }
                },
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (!running) {
                    Icon(Icons.Filled.TouchApp, contentDescription = null, modifier = Modifier.size(72.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.measure_idle_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.measure_idle_body, state.mode.seconds),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { (state.remainingMillis / totalMillis).coerceIn(0f, 1f) },
                            modifier = Modifier.size(220.dp),
                            strokeWidth = 10.dp,
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                state.breaths.toString(),
                                fontSize = 88.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                pluralStringResource(R.plurals.breaths_label, state.breaths),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        stringResource(R.string.measure_seconds_left, secondsLeft),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        stringResource(R.string.measure_keep_tapping),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onUndo,
                enabled = running && state.breaths > 0,
                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_undo_breath))
            }
            OutlinedButton(
                onClick = onReset,
                enabled = running,
                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_restart))
            }
        }
    }
}

@Composable
private fun ResultPanel(
    state: MeasureUiState,
    onContextChange: (MeasurementContext) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    onDiscard: () -> Unit,
) {
    val bpm = state.bpm ?: return
    val range = state.pet?.normalRange
    val status = state.status ?: range?.classify(bpm)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (status != null && range != null) {
            val style = statusStyle(status)
            Surface(
                color = style.container,
                contentColor = style.content,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier
                        .padding(24.dp)
                        .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.measure_result_heading),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(bpm.toString(), fontSize = 80.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.breaths_per_minute), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(style.icon, contentDescription = null, tint = style.accent, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(style.label),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        stringResource(R.string.measure_normal_range, rangeText(range)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        pluralStringResource(R.plurals.measure_counted, state.breaths, state.breaths, state.mode.seconds),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (status != com.petbreath.app.domain.RangeStatus.NORMAL) {
                Text(
                    stringResource(R.string.measure_out_of_range_advice),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Text(stringResource(R.string.measure_context_label), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.context == MeasurementContext.SLEEPING,
                onClick = { onContextChange(MeasurementContext.SLEEPING) },
                label = { Text(stringResource(R.string.context_sleeping)) },
            )
            FilterChip(
                selected = state.context == MeasurementContext.RESTING,
                onClick = { onContextChange(MeasurementContext.RESTING) },
                label = { Text(stringResource(R.string.context_resting)) },
            )
        }
        OutlinedTextField(
            value = state.notes,
            onValueChange = onNotesChange,
            label = { Text(stringResource(R.string.field_notes_optional)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )
        Button(
            onClick = onSave,
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) { Text(stringResource(R.string.action_save_measurement)) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onRetry, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text(stringResource(R.string.action_measure_again))
            }
            TextButton(onClick = onDiscard, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text(stringResource(R.string.action_discard))
            }
        }
    }
}
