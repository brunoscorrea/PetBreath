package com.petbreath.app.domain

import com.petbreath.app.domain.MeasurementSession.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurementSessionTest {

    @Test
    fun startsIdleWithFullDuration() {
        val session = MeasurementSession(MeasurementMode.SECONDS_30)
        assertEquals(State.Idle, session.state)
        assertEquals(30_000L, session.remainingMillis(123L))
    }

    @Test
    fun firstTapStartsTimerAndCountsOneBreath() {
        val session = MeasurementSession(MeasurementMode.SECONDS_30)
        val state = session.tap(1_000L)
        assertEquals(State.Running(startedAtMillis = 1_000L, breaths = 1), state)
        assertEquals(25_000L, session.remainingMillis(6_000L))
    }

    @Test
    fun countsBreathsAndFinishesAfterWindow() {
        val session = MeasurementSession(MeasurementMode.SECONDS_30)
        // A breath every 2 seconds for 30 seconds = 15 breaths = 30 bpm.
        for (i in 0 until 15) session.tap(i * 2_000L)
        assertTrue(session.advance(29_999L) is State.Running)
        val finished = session.advance(30_000L)
        assertEquals(State.Finished(breaths = 15, bpm = 30), finished)
        assertEquals(0L, session.remainingMillis(31_000L))
    }

    @Test
    fun sixtySecondModeReportsCountDirectly() {
        val session = MeasurementSession(MeasurementMode.SECONDS_60)
        repeat(18) { session.tap(it * 3_000L) }
        assertEquals(State.Finished(breaths = 18, bpm = 18), session.advance(60_000L))
    }

    @Test
    fun tapsAfterWindowAreIgnored() {
        val session = MeasurementSession(MeasurementMode.SECONDS_30)
        session.tap(0L)
        session.tap(1_000L)
        val state = session.tap(30_500L)
        assertEquals(State.Finished(breaths = 2, bpm = 4), state)
        assertEquals(state, session.tap(31_000L))
    }

    @Test
    fun undoRemovesLastBreathButNotBelowZero() {
        val session = MeasurementSession(MeasurementMode.SECONDS_30)
        session.tap(0L)
        session.tap(1_000L)
        assertEquals(1, (session.undo(2_000L) as State.Running).breaths)
        assertEquals(0, (session.undo(2_500L) as State.Running).breaths)
        assertEquals(0, (session.undo(3_000L) as State.Running).breaths)
    }

    @Test
    fun undoDoesNothingWhenIdleOrFinished() {
        val session = MeasurementSession(MeasurementMode.SECONDS_30)
        assertEquals(State.Idle, session.undo(0L))
        session.tap(0L)
        session.advance(30_000L)
        assertEquals(State.Finished(1, 2), session.undo(40_000L))
    }

    @Test
    fun resetReturnsToIdle() {
        val session = MeasurementSession(MeasurementMode.SECONDS_30)
        session.tap(0L)
        session.reset()
        assertEquals(State.Idle, session.state)
    }
}
