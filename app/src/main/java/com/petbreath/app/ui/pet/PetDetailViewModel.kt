package com.petbreath.app.ui.pet

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petbreath.app.data.db.MeasurementEntity
import com.petbreath.app.data.db.PetEntity
import com.petbreath.app.data.repository.MeasurementRepository
import com.petbreath.app.data.repository.PetRepository
import com.petbreath.app.data.settings.AppSettings
import com.petbreath.app.data.settings.SettingsRepository
import com.petbreath.app.export.ReportExporter
import com.petbreath.app.export.ReportFormat
import com.petbreath.app.ui.appViewModelFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration

/** Chart window options; null days means "all time". */
enum class ChartPeriod(val days: Int?) { WEEK(7), MONTH(30), QUARTER(90), ALL(null) }

data class PetDetailUiState(
    val loading: Boolean = true,
    val pet: PetEntity? = null,
    val measurements: List<MeasurementEntity> = emptyList(),
    val period: ChartPeriod = ChartPeriod.MONTH,
    val settings: AppSettings = AppSettings(),
    val exporting: Boolean = false,
) {
    val chartMeasurements: List<MeasurementEntity>
        get() {
            val days = period.days ?: return measurements
            val from = System.currentTimeMillis() - Duration.ofDays(days.toLong()).toMillis()
            return measurements.filter { it.takenAt >= from }
        }
}

sealed interface PetDetailEvent {
    data class Share(val intent: Intent) : PetDetailEvent
    data object ExportFailed : PetDetailEvent
    data object PetDeleted : PetDetailEvent
    data class MeasurementDeleted(val measurement: MeasurementEntity) : PetDetailEvent
}

class PetDetailViewModel(
    private val petId: Long,
    private val pets: PetRepository,
    private val measurements: MeasurementRepository,
    settings: SettingsRepository,
    private val exporter: ReportExporter,
) : ViewModel() {

    private val period = MutableStateFlow(ChartPeriod.MONTH)
    private val exporting = MutableStateFlow(false)

    val state: StateFlow<PetDetailUiState> = combine(
        pets.observePet(petId),
        measurements.observeForPet(petId),
        period,
        settings.settings,
        exporting,
    ) { pet, list, p, s, e ->
        PetDetailUiState(loading = false, pet = pet, measurements = list, period = p, settings = s, exporting = e)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PetDetailUiState())

    private val _events = MutableSharedFlow<PetDetailEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<PetDetailEvent> = _events.asSharedFlow()

    fun setPeriod(value: ChartPeriod) {
        period.value = value
    }

    fun saveMeasurement(measurement: MeasurementEntity) = viewModelScope.launch {
        if (measurement.id == 0L) measurements.add(measurement.copy(petId = petId)) else measurements.update(measurement)
    }

    fun deleteMeasurement(measurement: MeasurementEntity) = viewModelScope.launch {
        measurements.delete(measurement)
        _events.emit(PetDetailEvent.MeasurementDeleted(measurement))
    }

    /** Restores a measurement removed by mistake (from the snackbar "Undo"). */
    fun restoreMeasurement(measurement: MeasurementEntity) = viewModelScope.launch {
        measurements.add(measurement)
    }

    fun deletePet() = viewModelScope.launch {
        pets.delete(petId)
        _events.emit(PetDetailEvent.PetDeleted)
    }

    fun export(format: ReportFormat, periodDays: Int?) {
        if (exporting.value) return
        viewModelScope.launch {
            val pet = pets.getPet(petId) ?: return@launch
            exporting.value = true
            try {
                val unit = state.value.settings.weightUnit
                val file = exporter.export(pet, periodDays, format, unit)
                _events.emit(PetDetailEvent.Share(exporter.shareIntent(file, format, pet.name)))
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _events.emit(PetDetailEvent.ExportFailed)
            } finally {
                exporting.value = false
            }
        }
    }

    companion object {
        const val ARG_PET_ID = "petId"

        val Factory = appViewModelFactory { c, handle ->
            PetDetailViewModel(
                petId = checkNotNull(handle.get<Long>(ARG_PET_ID)),
                pets = c.petRepository,
                measurements = c.measurementRepository,
                settings = c.settingsRepository,
                exporter = c.reportExporter,
            )
        }
    }
}
