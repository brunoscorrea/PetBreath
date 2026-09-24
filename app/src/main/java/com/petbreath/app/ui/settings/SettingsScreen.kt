package com.petbreath.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petbreath.app.BuildConfig
import com.petbreath.app.R
import com.petbreath.app.data.settings.AppSettings
import com.petbreath.app.data.settings.SettingsRepository
import com.petbreath.app.data.settings.ThemeMode
import com.petbreath.app.data.settings.WeightUnit
import com.petbreath.app.domain.MeasurementMode
import com.petbreath.app.ui.appViewModelFactory
import com.petbreath.app.ui.components.DisclaimerBanner
import com.petbreath.app.ui.components.PetBreathTopBar
import com.petbreath.app.ui.components.SectionHeader
import com.petbreath.app.ui.pet.RadioRow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repo: SettingsRepository) : ViewModel() {
    val settings: StateFlow<AppSettings> =
        repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setMode(mode: MeasurementMode) = viewModelScope.launch { repo.setDefaultMode(mode) }
    fun setHaptics(enabled: Boolean) = viewModelScope.launch { repo.setHaptics(enabled) }
    fun setWeightUnit(unit: WeightUnit) = viewModelScope.launch { repo.setWeightUnit(unit) }
    fun setTheme(mode: ThemeMode) = viewModelScope.launch { repo.setThemeMode(mode) }

    companion object {
        val Factory = appViewModelFactory { c, _ -> SettingsViewModel(c.settingsRepository) }
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    Scaffold(topBar = { PetBreathTopBar(stringResource(R.string.settings_title), onBack = onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            SectionHeader(stringResource(R.string.settings_measurement))
            Column(Modifier.selectableGroup()) {
                MeasurementMode.entries.forEach { mode ->
                    RadioRow(
                        stringResource(R.string.settings_mode_option, mode.seconds),
                        settings.defaultMode == mode,
                    ) { viewModel.setMode(mode) }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .toggleable(
                        value = settings.hapticsEnabled,
                        role = Role.Switch,
                        onValueChange = viewModel::setHaptics,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_haptics))
                    Text(
                        stringResource(R.string.settings_haptics_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = settings.hapticsEnabled, onCheckedChange = null)
            }

            SectionHeader(stringResource(R.string.settings_units))
            Column(Modifier.selectableGroup()) {
                RadioRow(stringResource(R.string.unit_kg), settings.weightUnit == WeightUnit.KG) { viewModel.setWeightUnit(WeightUnit.KG) }
                RadioRow(stringResource(R.string.unit_lb), settings.weightUnit == WeightUnit.LB) { viewModel.setWeightUnit(WeightUnit.LB) }
            }

            SectionHeader(stringResource(R.string.settings_theme))
            Column(Modifier.selectableGroup()) {
                RadioRow(stringResource(R.string.theme_system), settings.themeMode == ThemeMode.SYSTEM) { viewModel.setTheme(ThemeMode.SYSTEM) }
                RadioRow(stringResource(R.string.theme_light), settings.themeMode == ThemeMode.LIGHT) { viewModel.setTheme(ThemeMode.LIGHT) }
                RadioRow(stringResource(R.string.theme_dark), settings.themeMode == ThemeMode.DARK) { viewModel.setTheme(ThemeMode.DARK) }
            }

            SectionHeader(stringResource(R.string.settings_privacy))
            Text(stringResource(R.string.settings_privacy_body), style = MaterialTheme.typography.bodyMedium)

            SectionHeader(stringResource(R.string.settings_about))
            Text(stringResource(R.string.settings_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyMedium)
            DisclaimerBanner(Modifier.padding(vertical = 16.dp))
        }
    }
}
