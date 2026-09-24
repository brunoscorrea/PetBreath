package com.petbreath.app.ui.measure

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petbreath.app.data.db.MeasurementContext
import com.petbreath.app.data.db.MeasurementEntity
import com.petbreath.app.data.db.PetEntity
import com.petbreath.app.data.db.normalRange
import com.petbreath.app.data.repository.MeasurementRepository
import com.petbreath.app.data.repository.PetRepository
import com.petbreath.app.data.settings.SettingsRepository
import com.petbreath.app.domain.MeasurementMode
import com.petbreath.app.domain.MeasurementSession
import com.petbreath.app.domain.RangeStatus
import com.petbreath.app.ui.appViewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class MeasurePhase { IDLE, RUNNING, FINISHED }

data class MeasureUiState(
    val pet: PetEntity? = null,
    val mode: MeasurementMode = MeasurementMode.SECONDS_30,
    val phase: MeasurePhase = MeasurePhase.IDLE,
    val breaths: Int = 0,
    val remainingMillis: Long = MeasurementMode.SECONDS_30.seconds * 1000L,
    val bpm: Int? = null,
    val status: RangeStatus? = null,
    val context: MeasurementContext = MeasurementContext.SLEEPING,
    val notes: String = "",
    val hapticsEnabled: Boolean = true,
    val saving: Boolean = false,
)

sealed interface MeasureEvent {
    data object Finished : MeasureEvent
    data object Saved : MeasureEvent
}

class MeasureViewModel(
    private val petId: Long,
    private val pets: PetRepository,
    private val measurements: MeasurementRepository,
    private val settings: SettingsRepository,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    private val wallClock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _state = MutableStateFlow(MeasureUiState())
    val state: StateFlow<MeasureUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<MeasureEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<MeasureEvent> = _events.asSharedFlow()

    private var session = MeasurementSession(MeasurementMode.SECONDS_30)
    private var ticker: Job? = null
    private var modeChosen = false

    init {
        viewModelScope.launch {
            val appSettings = settings.settings.first()
            _state.update { it.copy(hapticsEnabled = appSettings.hapticsEnabled) }
            // Apply the preferred mode unless the user already picked one or started counting.
            if (!modeChosen && _state.value.phase == MeasurePhase.IDLE) {
                session = MeasurementSession(appSettings.defaultMode)
                _state.update {
                    it.copy(mode = appSettings.defaultMode, remainingMillis = appSettings.defaultMode.seconds * 1000L)
                }
            }
        }
        viewModelScope.launch {
            pets.observePet(petId).collect { pet -> _state.update { it.copy(pet = pet) } }
        }
    }

    fun setMode(mode: MeasurementMode) {
        if (_state.value.phase == MeasurePhase.RUNNING) return
        modeChosen = true
        ticker?.cancel()
        session = MeasurementSession(mode)
        _state.update {
            it.copy(mode = mode, phase = MeasurePhase.IDLE, breaths = 0, remainingMillis = mode.seconds * 1000L, bpm = null, status = null)
        }
    }

    fun tap() {
        if (_state.value.phase == MeasurePhase.FINISHED) return
        session.tap(clock())
        publish()
        if (ticker?.isActive != true) startTicker()
    }

    fun undo() {
        session.undo(clock())
        publish()
    }

    fun reset() {
        ticker?.cancel()
        session.reset()
        _state.update {
            it.copy(phase = MeasurePhase.IDLE, breaths = 0, remainingMillis = it.mode.seconds * 1000L, bpm = null, status = null, notes = "")
        }
    }

    fun setContext(context: MeasurementContext) = _state.update { it.copy(context = context) }
    fun setNotes(notes: String) = _state.update { it.copy(notes = notes.take(500)) }

    fun save() {
        val s = _state.value
        val bpm = s.bpm ?: return
        if (s.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            measurements.add(
                MeasurementEntity(
                    petId = petId,
                    takenAt = wallClock(),
                    bpm = bpm,
                    breathCount = s.breaths,
                    durationSeconds = s.mode.seconds,
                    context = s.context,
                    notes = s.notes.trim(),
                ),
            )
            _events.emit(MeasureEvent.Saved)
        }
    }

    private fun startTicker() {
        ticker = viewModelScope.launch {
            while (isActive) {
                delay(TICK_MILLIS)
                val before = _state.value.phase
                session.advance(clock())
                publish()
                if (before == MeasurePhase.RUNNING && _state.value.phase == MeasurePhase.FINISHED) {
                    _events.emit(MeasureEvent.Finished)
                    break
                }
            }
        }
    }

    private fun publish() {
        val now = clock()
        val remaining = session.remainingMillis(now)
        _state.update { current ->
            when (val s = session.state) {
                MeasurementSession.State.Idle -> current.copy(phase = MeasurePhase.IDLE, breaths = 0, remainingMillis = remaining)
                is MeasurementSession.State.Running -> current.copy(phase = MeasurePhase.RUNNING, breaths = s.breaths, remainingMillis = remaining)
                is MeasurementSession.State.Finished -> current.copy(
                    phase = MeasurePhase.FINISHED,
                    breaths = s.breaths,
                    remainingMillis = 0,
                    bpm = s.bpm,
                    status = current.pet?.normalRange?.classify(s.bpm),
                )
            }
        }
    }

    companion object {
        const val ARG_PET_ID = "petId"
        private const val TICK_MILLIS = 100L

        val Factory = appViewModelFactory { c, handle ->
            MeasureViewModel(
                petId = checkNotNull(handle.get<Long>(ARG_PET_ID)),
                pets = c.petRepository,
                measurements = c.measurementRepository,
                settings = c.settingsRepository,
            )
        }
    }
}
