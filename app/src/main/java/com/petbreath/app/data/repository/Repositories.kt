package com.petbreath.app.data.repository

import androidx.room.withTransaction
import com.petbreath.app.data.db.MeasurementEntity
import com.petbreath.app.data.db.MedicationDoseEntity
import com.petbreath.app.data.db.MedicationEntity
import com.petbreath.app.data.db.PetBreathDatabase
import com.petbreath.app.data.db.PetEntity
import com.petbreath.app.data.db.ReminderEntity
import com.petbreath.app.data.db.ReminderType
import kotlinx.coroutines.flow.Flow

/** Schedules the system alarms that back reminders. Abstracted for testing. */
interface ReminderScheduler {
    fun schedule(reminder: ReminderEntity)
    fun cancel(reminderId: Long)
}

class PetRepository(
    private val db: PetBreathDatabase,
    private val scheduler: ReminderScheduler,
) {
    private val dao = db.petDao()

    fun observePets(): Flow<List<PetEntity>> = dao.observeAll()
    fun observePet(id: Long): Flow<PetEntity?> = dao.observe(id)
    suspend fun getPet(id: Long): PetEntity? = dao.get(id)

    /** Inserts or updates [pet] and returns its id. */
    suspend fun save(pet: PetEntity): Long =
        if (pet.id == 0L) dao.insert(pet) else pet.id.also { dao.update(pet) }

    /** Deletes the pet and, via cascading foreign keys, all of its data. */
    suspend fun delete(id: Long) {
        db.reminderDao().listForPet(id).forEach { scheduler.cancel(it.id) }
        dao.delete(id)
    }
}

class MeasurementRepository(db: PetBreathDatabase) {
    private val dao = db.measurementDao()

    fun observeForPet(petId: Long): Flow<List<MeasurementEntity>> = dao.observeForPet(petId)
    fun observeLatestPerPet(): Flow<List<MeasurementEntity>> = dao.observeLatestPerPet()
    suspend fun listForPetSince(petId: Long, fromMillis: Long) = dao.listForPetSince(petId, fromMillis)
    suspend fun get(id: Long): MeasurementEntity? = dao.get(id)
    suspend fun add(measurement: MeasurementEntity): Long = dao.insert(measurement)
    suspend fun update(measurement: MeasurementEntity) = dao.update(measurement)
    suspend fun delete(measurement: MeasurementEntity) = dao.delete(measurement)
}

class MedicationRepository(
    private val db: PetBreathDatabase,
    private val scheduler: ReminderScheduler,
) {
    private val dao = db.medicationDao()

    fun observeForPet(petId: Long): Flow<List<MedicationEntity>> = dao.observeForPet(petId)
    suspend fun listForPet(petId: Long): List<MedicationEntity> = dao.listForPet(petId)
    suspend fun get(id: Long): MedicationEntity? = dao.get(id)

    fun observeDosesSince(petId: Long, fromMillis: Long): Flow<List<MedicationDoseEntity>> =
        dao.observeDosesForPetSince(petId, fromMillis)

    /**
     * Saves the medication and replaces its reminder times with [reminderTimes]
     * (hour to minute pairs, every day). Returns the medication id.
     */
    suspend fun save(medication: MedicationEntity, reminderTimes: List<Pair<Int, Int>>): Long {
        val reminderDao = db.reminderDao()
        val (id, old, new) = db.withTransaction {
            val id = if (medication.id == 0L) dao.insert(medication) else medication.id.also { dao.update(medication) }
            val old = reminderDao.listForMedication(id)
            old.forEach { reminderDao.delete(it.id) }
            val new = if (medication.active) {
                reminderTimes.distinct().map { (h, m) ->
                    val reminder = ReminderEntity(
                        petId = medication.petId,
                        medicationId = id,
                        type = ReminderType.MEDICATION,
                        hour = h,
                        minute = m,
                    )
                    reminder.copy(id = reminderDao.upsert(reminder))
                }
            } else {
                emptyList()
            }
            Triple(id, old, new)
        }
        old.forEach { scheduler.cancel(it.id) }
        new.forEach { scheduler.schedule(it) }
        return id
    }

    suspend fun reminderTimes(medicationId: Long): List<Pair<Int, Int>> =
        db.reminderDao().listForMedication(medicationId).map { it.hour to it.minute }.sortedWith(
            compareBy({ it.first }, { it.second }),
        )

    suspend fun delete(id: Long) {
        db.reminderDao().listForMedication(id).forEach { scheduler.cancel(it.id) }
        dao.delete(id)
    }

    suspend fun logDose(medicationId: Long, givenAt: Long): Long =
        dao.insertDose(MedicationDoseEntity(medicationId = medicationId, givenAt = givenAt))

    suspend fun deleteDose(doseId: Long) = dao.deleteDose(doseId)
}

class ReminderRepository(
    db: PetBreathDatabase,
    private val scheduler: ReminderScheduler,
) {
    private val dao = db.reminderDao()

    fun observeForPet(petId: Long): Flow<List<ReminderEntity>> = dao.observeForPet(petId)
    suspend fun get(id: Long): ReminderEntity? = dao.get(id)

    suspend fun save(reminder: ReminderEntity): Long {
        val result = dao.upsert(reminder)
        val id = if (reminder.id != 0L) reminder.id else result
        val saved = reminder.copy(id = id)
        if (saved.enabled) scheduler.schedule(saved) else scheduler.cancel(id)
        return id
    }

    suspend fun delete(id: Long) {
        scheduler.cancel(id)
        dao.delete(id)
    }

    /** Re-registers every enabled reminder, e.g. after a reboot or time-zone change. */
    suspend fun rescheduleAll() {
        dao.listAll().forEach { if (it.enabled) scheduler.schedule(it) else scheduler.cancel(it.id) }
    }
}
