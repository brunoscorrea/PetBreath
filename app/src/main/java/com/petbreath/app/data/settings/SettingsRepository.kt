package com.petbreath.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.petbreath.app.domain.MeasurementMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class WeightUnit { KG, LB }
enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val disclaimerAccepted: Boolean = false,
    val defaultMode: MeasurementMode = MeasurementMode.SECONDS_30,
    val hapticsEnabled: Boolean = true,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.applicationContext.dataStore)

    private object Keys {
        val DISCLAIMER = booleanPreferencesKey("disclaimer_accepted")
        val MODE_SECONDS = intPreferencesKey("default_mode_seconds")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val THEME = stringPreferencesKey("theme_mode")
    }

    val settings: Flow<AppSettings> = dataStore.data.map { p ->
        AppSettings(
            disclaimerAccepted = p[Keys.DISCLAIMER] ?: false,
            defaultMode = MeasurementMode.fromSeconds(p[Keys.MODE_SECONDS] ?: 30),
            hapticsEnabled = p[Keys.HAPTICS] ?: true,
            weightUnit = p[Keys.WEIGHT_UNIT]?.let { runCatching { WeightUnit.valueOf(it) }.getOrNull() }
                ?: WeightUnit.KG,
            themeMode = p[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
        )
    }

    suspend fun acceptDisclaimer() = dataStore.edit { it[Keys.DISCLAIMER] = true }
    suspend fun setDefaultMode(mode: MeasurementMode) = dataStore.edit { it[Keys.MODE_SECONDS] = mode.seconds }
    suspend fun setHaptics(enabled: Boolean) = dataStore.edit { it[Keys.HAPTICS] = enabled }
    suspend fun setWeightUnit(unit: WeightUnit) = dataStore.edit { it[Keys.WEIGHT_UNIT] = unit.name }
    suspend fun setThemeMode(mode: ThemeMode) = dataStore.edit { it[Keys.THEME] = mode.name }
}
