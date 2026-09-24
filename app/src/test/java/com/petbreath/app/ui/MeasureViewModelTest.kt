package com.petbreath.app.ui

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.petbreath.app.data.FakeScheduler
import com.petbreath.app.data.db.MeasurementContext
import com.petbreath.app.data.db.PetBreathDatabase
import com.petbreath.app.data.db.PetEntity
import com.petbreath.app.data.db.Species
import com.petbreath.app.data.repository.MeasurementRepository
import com.petbreath.app.data.repository.PetRepository
import com.petbreath.app.data.settings.SettingsRepository
import com.petbreath.app.domain.MeasurementMode
import com.petbreath.app.domain.RangeStatus
import com.petbreath.app.ui.measure.MeasurePhase
import com.petbreath.app.ui.measure.MeasureViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class MeasureViewModelTest {
    @get:Rule val tmp = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private lateinit var db: PetBreathDatabase
    private lateinit var settings: SettingsRepository
    private var petId = 0L
    private var now = 0L

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PetBreathDatabase::class.java).allowMainThreadQueries().build()
        settings = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = TestScope(dispatcher)) { java.io.File(tmp.root, "test.preferences_pb") },
        )
        petId = runBlocking { db.petDao().insert(PetEntity(name = "Rex", species = Species.DOG, createdAt = 0)) }
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun viewModel() = MeasureViewModel(
        petId = petId,
        pets = PetRepository(db, FakeScheduler()),
        measurements = MeasurementRepository(db),
        settings = settings,
        clock = { now },
        wallClock = { 1_700_000_000_000L },
    )

    @Test
    fun countsTapsAndFinishesAboveThreshold() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        // Wait for Room's flow to deliver the pet on its own executor.
        vm.state.first { it.pet != null }
        assertEquals(MeasurementMode.SECONDS_30, vm.state.value.mode)

        // 18 breaths in 30 seconds -> 36 bpm, above the default threshold of 30.
        repeat(18) { i ->
            now = i * 1_500L
            vm.tap()
        }
        assertEquals(MeasurePhase.RUNNING, vm.state.value.phase)
        assertEquals(18, vm.state.value.breaths)

        now = 30_000L
        advanceTimeBy(200)
        val state = vm.state.value
        assertEquals(MeasurePhase.FINISHED, state.phase)
        assertEquals(36, state.bpm)
        assertEquals(RangeStatus.ABOVE, state.status)
    }

    @Test
    fun undoAndResetWork() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.tap()
        vm.tap()
        vm.undo()
        assertEquals(1, vm.state.value.breaths)
        vm.reset()
        assertEquals(MeasurePhase.IDLE, vm.state.value.phase)
        assertEquals(0, vm.state.value.breaths)
    }

    @Test
    fun switchingModeChangesDuration() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.setMode(MeasurementMode.SECONDS_60)
        assertEquals(60_000L, vm.state.value.remainingMillis)
        vm.tap()
        now = 30_000L
        advanceTimeBy(200)
        assertEquals(MeasurePhase.RUNNING, vm.state.value.phase)
        now = 60_000L
        advanceTimeBy(200)
        assertEquals(MeasurePhase.FINISHED, vm.state.value.phase)
        assertEquals(1, vm.state.value.bpm)
    }

    @Test
    fun savingStoresMeasurement() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.state.first { it.pet != null }
        repeat(10) { i -> now = i * 2_000L; vm.tap() }
        now = 30_000L
        advanceTimeBy(200)
        vm.setContext(MeasurementContext.RESTING)
        vm.setNotes("  after dinner  ")
        vm.save()
        advanceUntilIdle()
        // Room writes on its own executor; wait for the row to appear.
        val saved = db.measurementDao().observeForPet(petId).first { it.isNotEmpty() }.single()
        assertEquals(20, saved.bpm)
        assertEquals(10, saved.breathCount)
        assertEquals(30, saved.durationSeconds)
        assertEquals(MeasurementContext.RESTING, saved.context)
        assertEquals("after dinner", saved.notes)
        assertEquals(1_700_000_000_000L, saved.takenAt)
    }
}
