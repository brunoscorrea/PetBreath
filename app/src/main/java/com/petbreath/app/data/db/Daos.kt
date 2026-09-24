package com.petbreath.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {
    @Query("SELECT * FROM pets ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<PetEntity>>

    @Query("SELECT * FROM pets WHERE id = :id")
    fun observe(id: Long): Flow<PetEntity?>

    @Query("SELECT * FROM pets WHERE id = :id")
    suspend fun get(id: Long): PetEntity?

    @Insert
    suspend fun insert(pet: PetEntity): Long

    @Update
    suspend fun update(pet: PetEntity)

    @Query("DELETE FROM pets WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurements WHERE pet_id = :petId ORDER BY taken_at DESC")
    fun observeForPet(petId: Long): Flow<List<MeasurementEntity>>

    @Query(
        "SELECT * FROM measurements WHERE pet_id = :petId AND taken_at >= :fromMillis " +
            "ORDER BY taken_at ASC",
    )
    suspend fun listForPetSince(petId: Long, fromMillis: Long): List<MeasurementEntity>

    /** The most recent measurement of every pet (at most one row per pet). */
    @Query(
        "SELECT * FROM measurements m WHERE m.id = (" +
            "SELECT id FROM measurements WHERE pet_id = m.pet_id " +
            "ORDER BY taken_at DESC, id DESC LIMIT 1)",
    )
    fun observeLatestPerPet(): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE id = :id")
    suspend fun get(id: Long): MeasurementEntity?

    @Insert
    suspend fun insert(measurement: MeasurementEntity): Long

    @Update
    suspend fun update(measurement: MeasurementEntity)

    @Delete
    suspend fun delete(measurement: MeasurementEntity)
}

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications WHERE pet_id = :petId ORDER BY active DESC, name COLLATE NOCASE")
    fun observeForPet(petId: Long): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE pet_id = :petId ORDER BY active DESC, name COLLATE NOCASE")
    suspend fun listForPet(petId: Long): List<MedicationEntity>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun get(id: Long): MedicationEntity?

    @Insert
    suspend fun insert(medication: MedicationEntity): Long

    @Update
    suspend fun update(medication: MedicationEntity)

    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun delete(id: Long)

    @Insert
    suspend fun insertDose(dose: MedicationDoseEntity): Long

    @Query("DELETE FROM medication_doses WHERE id = :doseId")
    suspend fun deleteDose(doseId: Long)

    @Query(
        "SELECT d.* FROM medication_doses d JOIN medications m ON m.id = d.medication_id " +
            "WHERE m.pet_id = :petId AND d.given_at >= :fromMillis ORDER BY d.given_at DESC",
    )
    fun observeDosesForPetSince(petId: Long, fromMillis: Long): Flow<List<MedicationDoseEntity>>
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY hour, minute")
    suspend fun listAll(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE pet_id = :petId ORDER BY hour, minute")
    fun observeForPet(petId: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE pet_id = :petId")
    suspend fun listForPet(petId: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE medication_id = :medicationId")
    suspend fun listForMedication(medicationId: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun get(id: Long): ReminderEntity?

    @Upsert
    suspend fun upsert(reminder: ReminderEntity): Long

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: Long)
}
