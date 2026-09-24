package com.petbreath.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.petbreath.app.data.db.MeasurementEntity
import com.petbreath.app.data.db.PetBreathDatabase
import com.petbreath.app.data.db.PetEntity
import com.petbreath.app.data.db.Species
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs the Room schema against the device's real SQLite implementation. */
@RunWith(AndroidJUnit4::class)
class PetDaoInstrumentedTest {
    private lateinit var db: PetBreathDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PetBreathDatabase::class.java).build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun insertAndReadBack() = runTest {
        val petId = db.petDao().insert(PetEntity(name = "Luna", species = Species.CAT, createdAt = 0))
        db.measurementDao().insert(MeasurementEntity(petId = petId, takenAt = 1_000, bpm = 22))
        assertEquals("Luna", db.petDao().get(petId)?.name)
        assertEquals(22, db.measurementDao().observeLatestPerPet().first().single().bpm)
    }
}
