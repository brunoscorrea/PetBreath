package com.petbreath.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.petbreath.app.data.db.MeasurementEntity
import com.petbreath.app.data.db.MedicationEntity
import com.petbreath.app.data.db.PetBreathDatabase
import com.petbreath.app.data.db.PetEntity
import com.petbreath.app.data.db.ReminderEntity
import com.petbreath.app.data.db.ReminderType
import com.petbreath.app.data.db.Species
import com.petbreath.app.data.repository.MedicationRepository
import com.petbreath.app.data.repository.PetRepository
import com.petbreath.app.data.repository.ReminderRepository
import com.petbreath.app.data.repository.ReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

class FakeScheduler : ReminderScheduler {
    val scheduled = mutableMapOf<Long, ReminderEntity>()
    val cancelled = mutableListOf<Long>()
    override fun schedule(reminder: ReminderEntity) {
        scheduled[reminder.id] = reminder
    }
    override fun cancel(reminderId: Long) {
        scheduled.remove(reminderId)
        cancelled += reminderId
    }
}

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private lateinit var db: PetBreathDatabase
    private lateinit var scheduler: FakeScheduler

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PetBreathDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        scheduler = FakeScheduler()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun insertPet(name: String = "Rex", species: Species = Species.DOG) =
        db.petDao().insert(PetEntity(name = name, species = species, createdAt = 0))

    @Test
    fun petsAreSortedByNameCaseInsensitive() = runTest {
        insertPet("milo", Species.CAT)
        insertPet("Bella")
        insertPet("Zed")
        assertEquals(listOf("Bella", "milo", "Zed"), db.petDao().observeAll().first().map { it.name })
    }

    @Test
    fun newPetUsesDefaultThresholdOf30() = runTest {
        val id = insertPet()
        val pet = db.petDao().get(id)!!
        assertEquals(30, pet.upperThresholdBpm)
        assertNull(pet.lowerThresholdBpm)
    }

    @Test
    fun measurementsAreNewestFirstAndEditable() = runTest {
        val petId = insertPet()
        val dao = db.measurementDao()
        dao.insert(MeasurementEntity(petId = petId, takenAt = 1_000, bpm = 20))
        val id = dao.insert(MeasurementEntity(petId = petId, takenAt = 2_000, bpm = 24))
        assertEquals(listOf(24, 20), dao.observeForPet(petId).first().map { it.bpm })

        dao.update(dao.get(id)!!.copy(bpm = 26, notes = "after walk"))
        assertEquals(26, dao.get(id)!!.bpm)
        assertEquals("after walk", dao.get(id)!!.notes)

        dao.delete(dao.get(id)!!)
        assertEquals(listOf(20), dao.observeForPet(petId).first().map { it.bpm })
    }

    @Test
    fun latestPerPetReturnsOneRowPerPet() = runTest {
        val rex = insertPet("Rex")
        val milo = insertPet("Milo", Species.CAT)
        val dao = db.measurementDao()
        dao.insert(MeasurementEntity(petId = rex, takenAt = 1_000, bpm = 20))
        dao.insert(MeasurementEntity(petId = rex, takenAt = 3_000, bpm = 28))
        dao.insert(MeasurementEntity(petId = milo, takenAt = 2_000, bpm = 22))
        val latest = dao.observeLatestPerPet().first().associateBy { it.petId }
        assertEquals(2, latest.size)
        assertEquals(28, latest[rex]!!.bpm)
        assertEquals(22, latest[milo]!!.bpm)
    }

    @Test
    fun listSinceFiltersByTimeAscending() = runTest {
        val petId = insertPet()
        val dao = db.measurementDao()
        listOf(5_000L, 1_000L, 3_000L).forEach { dao.insert(MeasurementEntity(petId = petId, takenAt = it, bpm = 20)) }
        assertEquals(listOf(3_000L, 5_000L), dao.listForPetSince(petId, 2_000).map { it.takenAt })
    }

    @Test
    fun deletingPetCascadesAndCancelsReminders() = runTest {
        val petId = insertPet()
        db.measurementDao().insert(MeasurementEntity(petId = petId, takenAt = 1, bpm = 20))
        val reminders = ReminderRepository(db, scheduler)
        val reminderId = reminders.save(ReminderEntity(petId = petId, type = ReminderType.RESPIRATORY_RATE, hour = 21, minute = 0))
        assertTrue(scheduler.scheduled.containsKey(reminderId))

        PetRepository(db, scheduler).delete(petId)

        assertNull(db.petDao().get(petId))
        assertTrue(db.measurementDao().observeForPet(petId).first().isEmpty())
        assertTrue(db.reminderDao().listAll().isEmpty())
        assertTrue(reminderId in scheduler.cancelled)
    }

    @Test
    fun savingMedicationReplacesReminderTimes() = runTest {
        val petId = insertPet()
        val repo = MedicationRepository(db, scheduler)
        val medId = repo.save(
            MedicationEntity(petId = petId, name = "Pimobendan", dosage = "2.5 mg", createdAt = 0),
            listOf(8 to 0, 20 to 0),
        )
        assertEquals(listOf(8 to 0, 20 to 0), repo.reminderTimes(medId))
        assertEquals(2, scheduler.scheduled.size)

        repo.save(repo.get(medId)!!, listOf(9 to 30))
        assertEquals(listOf(9 to 30), repo.reminderTimes(medId))
        assertEquals(1, scheduler.scheduled.size)
        assertEquals(9, scheduler.scheduled.values.single().hour)
    }

    @Test
    fun inactiveMedicationHasNoReminders() = runTest {
        val petId = insertPet()
        val repo = MedicationRepository(db, scheduler)
        val medId = repo.save(MedicationEntity(petId = petId, name = "Furosemide", createdAt = 0), listOf(8 to 0))
        repo.save(repo.get(medId)!!.copy(active = false), listOf(8 to 0))
        assertTrue(repo.reminderTimes(medId).isEmpty())
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun dosesAreLoggedAndRemovedWithMedication() = runTest {
        val petId = insertPet()
        val repo = MedicationRepository(db, scheduler)
        val medId = repo.save(MedicationEntity(petId = petId, name = "Benazepril", createdAt = 0), emptyList())
        repo.logDose(medId, 5_000)
        repo.logDose(medId, 9_000)
        assertEquals(listOf(9_000L, 5_000L), repo.observeDosesSince(petId, 0).first().map { it.givenAt })
        repo.delete(medId)
        assertTrue(repo.observeDosesSince(petId, 0).first().isEmpty())
    }

    @Test
    fun disablingReminderCancelsAlarm() = runTest {
        val petId = insertPet()
        val repo = ReminderRepository(db, scheduler)
        val id = repo.save(ReminderEntity(petId = petId, type = ReminderType.RESPIRATORY_RATE, hour = 7, minute = 15))
        repo.save(repo.get(id)!!.copy(enabled = false))
        assertTrue(scheduler.scheduled.isEmpty())
        assertEquals(false, repo.get(id)!!.enabled)

        repo.save(repo.get(id)!!.copy(enabled = true))
        scheduler.scheduled.clear()
        repo.rescheduleAll()
        assertEquals(setOf(id), scheduler.scheduled.keys)
    }
}
