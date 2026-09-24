package com.petbreath.app.domain

/**
 * Pure state machine for a tap-to-count respiratory-rate measurement.
 *
 * The timer starts on the first tap, which also counts as the first breath.
 * Every later tap inside the window counts one more breath. Once the window has
 * elapsed the session is [State.Finished] and further taps are ignored.
 */
class MeasurementSession(val mode: MeasurementMode) {

    sealed interface State {
        data object Idle : State
        data class Running(val startedAtMillis: Long, val breaths: Int) : State
        data class Finished(val breaths: Int, val bpm: Int) : State
    }

    var state: State = State.Idle
        private set

    private val durationMillis: Long get() = mode.seconds * 1000L

    /** Registers a breath at [nowMillis]. Returns the updated state. */
    fun tap(nowMillis: Long): State {
        state = when (val s = advance(nowMillis)) {
            State.Idle -> State.Running(startedAtMillis = nowMillis, breaths = 1)
            is State.Running -> s.copy(breaths = s.breaths + 1)
            is State.Finished -> s
        }
        return state
    }

    /** Removes the last counted breath (e.g. after an accidental tap). */
    fun undo(nowMillis: Long): State {
        val s = advance(nowMillis)
        if (s is State.Running && s.breaths > 0) state = s.copy(breaths = s.breaths - 1)
        return state
    }

    /** Moves the session to [State.Finished] once the window has elapsed. */
    fun advance(nowMillis: Long): State {
        val s = state
        if (s is State.Running && nowMillis - s.startedAtMillis >= durationMillis) {
            state = State.Finished(
                breaths = s.breaths,
                bpm = RespiratoryRate.breathsPerMinute(s.breaths, mode.seconds),
            )
        }
        return state
    }

    fun remainingMillis(nowMillis: Long): Long = when (val s = state) {
        State.Idle -> durationMillis
        is State.Running -> (durationMillis - (nowMillis - s.startedAtMillis)).coerceIn(0, durationMillis)
        is State.Finished -> 0
    }

    fun reset() {
        state = State.Idle
    }
}
