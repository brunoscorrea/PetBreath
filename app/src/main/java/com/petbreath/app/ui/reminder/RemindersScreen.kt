package com.petbreath.app.ui.reminder

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petbreath.app.R
import com.petbreath.app.data.db.PetEntity
import com.petbreath.app.data.db.ReminderEntity
import com.petbreath.app.data.db.ReminderType
import com.petbreath.app.data.repository.PetRepository
import com.petbreath.app.data.repository.ReminderRepository
import com.petbreath.app.domain.DaysOfWeek
import com.petbreath.app.reminders.Notifications
import com.petbreath.app.ui.appViewModelFactory
import com.petbreath.app.ui.components.EmptyState
import com.petbreath.app.ui.components.PetBreathTopBar
import com.petbreath.app.ui.formatTime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

data class RemindersUiState(
    val loading: Boolean = true,
    val pet: PetEntity? = null,
    val reminders: List<ReminderEntity> = emptyList(),
)

class RemindersViewModel(
    private val petId: Long,
    pets: PetRepository,
    private val reminders: ReminderRepository,
) : ViewModel() {
    val state: StateFlow<RemindersUiState> = combine(
        pets.observePet(petId),
        reminders.observeForPet(petId),
    ) { pet, list ->
        RemindersUiState(false, pet, list.filter { it.type == ReminderType.RESPIRATORY_RATE })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RemindersUiState())

    fun save(reminder: ReminderEntity) = viewModelScope.launch {
        reminders.save(reminder.copy(petId = petId, type = ReminderType.RESPIRATORY_RATE))
    }

    fun setEnabled(reminder: ReminderEntity, enabled: Boolean) = viewModelScope.launch {
        reminders.save(reminder.copy(enabled = enabled))
    }

    fun delete(reminder: ReminderEntity) = viewModelScope.launch { reminders.delete(reminder.id) }

    companion object {
        const val ARG_PET_ID = "petId"

        val Factory = appViewModelFactory { c, handle ->
            RemindersViewModel(checkNotNull(handle.get<Long>(ARG_PET_ID)), c.petRepository, c.reminderRepository)
        }
    }
}

@Composable
fun RemindersScreen(
    onBack: () -> Unit,
    viewModel: RemindersViewModel = viewModel(factory = RemindersViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<ReminderEntity?>(null) }

    Scaffold(
        topBar = {
            PetBreathTopBar(
                title = state.pet?.let { stringResource(R.string.reminders_title_pet, it.name) }
                    ?: stringResource(R.string.reminders_title),
                onBack = onBack,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editing = ReminderEntity(petId = 0, type = ReminderType.RESPIRATORY_RATE, hour = 21, minute = 0)
                },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(stringResource(R.string.action_add_reminder)) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.reminders_help),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item { NotificationPermissionBanner() }
            if (!state.loading && state.reminders.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.NotificationsActive,
                        title = stringResource(R.string.reminders_empty_title),
                        body = stringResource(R.string.reminders_empty_body),
                    )
                }
            }
            items(state.reminders, key = { it.id }) { reminder ->
                Card(onClick = { editing = reminder }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                formatTime(reminder.hour, reminder.minute),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(daysSummary(reminder.daysMask), style = MaterialTheme.typography.bodyMedium)
                        }
                        Switch(
                            checked = reminder.enabled,
                            onCheckedChange = { viewModel.setEnabled(reminder, it) },
                        )
                        IconButton(onClick = { viewModel.delete(reminder) }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete_reminder))
                        }
                    }
                }
            }
        }
    }

    editing?.let { r ->
        ReminderEditDialog(
            initial = r,
            onSave = { viewModel.save(it); editing = null },
            onDismiss = { editing = null },
        )
    }
}

@Composable
fun daysSummary(mask: Int): String = when (mask and DaysOfWeek.EVERY_DAY) {
    DaysOfWeek.EVERY_DAY -> stringResource(R.string.days_every_day)
    DaysOfWeek.NONE -> stringResource(R.string.days_none)
    else -> DaysOfWeek.days(mask).joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ReminderEditDialog(
    initial: ReminderEntity,
    onSave: (ReminderEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    val timeState = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = android.text.format.DateFormat.is24HourFormat(LocalContext.current),
    )
    var mask by rememberSaveable { mutableIntStateOf(initial.daysMask) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reminder_edit_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = timeState)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DayOfWeek.entries.forEach { day ->
                        FilterChip(
                            selected = DaysOfWeek.contains(mask, day),
                            onClick = { mask = DaysOfWeek.toggle(mask, day) },
                            label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault())) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = mask != DaysOfWeek.NONE,
                onClick = { onSave(initial.copy(hour = timeState.hour, minute = timeState.minute, daysMask = mask, enabled = true)) },
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/**
 * Explains why notifications are needed and requests the runtime permission on
 * Android 13+, or links to system settings if notifications are turned off.
 */
@Composable
fun NotificationPermissionBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(Notifications.canPost(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { allowed = Notifications.canPost(context) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        allowed = granted && Notifications.canPost(context)
        if (!allowed) {
            // Permanently denied or disabled: send the user to the app's notification settings.
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
    if (allowed) return
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.NotificationsOff, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.notifications_off_title), style = MaterialTheme.typography.titleSmall)
            }
            Text(stringResource(R.string.notifications_off_body), style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
                modifier = Modifier.padding(top = 8.dp),
            ) { Text(stringResource(R.string.action_enable_notifications)) }
        }
    }
}
