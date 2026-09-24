package com.petbreath.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.petbreath.app.domain.DaysOfWeek
import com.petbreath.app.domain.NormalRange

enum class Species { DOG, CAT, OTHER }

@Entity(tableName = "pets")
data class PetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val species: Species,
    val breed: String = "",
    /** Age in years as entered by the owner, at [ageRecordedAt]. */
    @ColumnInfo(name = "age_years") val ageYears: Double? = null,
    @ColumnInfo(name = "age_recorded_at") val ageRecordedAt: Long? = null,
    @ColumnInfo(name = "weight_kg") val weightKg: Double? = null,
    @ColumnInfo(name = "medical_notes") val medicalNotes: String = "",
    @ColumnInfo(name = "upper_threshold_bpm") val upperThresholdBpm: Int = NormalRange.DEFAULT_UPPER_BPM,
    @ColumnInfo(name = "lower_threshold_bpm") val lowerThresholdBpm: Int? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

/** The pet's configured normal range (an extension so Room never treats it as a column). */
val PetEntity.normalRange: NormalRange get() = NormalRange(upperThresholdBpm, lowerThresholdBpm)

@Entity(
    tableName = "measurements",
    foreignKeys = [
        ForeignKey(
            entity = PetEntity::class,
            parentColumns = ["id"],
            childColumns = ["pet_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["pet_id", "taken_at"])],
)
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "pet_id") val petId: Long,
    @ColumnInfo(name = "taken_at") val takenAt: Long,
    val bpm: Int,
    /** Null for manually entered readings. */
    @ColumnInfo(name = "breath_count") val breathCount: Int? = null,
    @ColumnInfo(name = "duration_seconds") val durationSeconds: Int? = null,
    val context: MeasurementContext = MeasurementContext.SLEEPING,
    val notes: String = "",
)

enum class MeasurementContext { SLEEPING, RESTING }

@Entity(
    tableName = "medications",
    foreignKeys = [
        ForeignKey(
            entity = PetEntity::class,
            parentColumns = ["id"],
            childColumns = ["pet_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("pet_id")],
)
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "pet_id") val petId: Long,
    val name: String,
    val dosage: String = "",
    val instructions: String = "",
    val active: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(
    tableName = "medication_doses",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medication_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["medication_id", "given_at"])],
)
data class MedicationDoseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "medication_id") val medicationId: Long,
    @ColumnInfo(name = "given_at") val givenAt: Long,
)

enum class ReminderType { RESPIRATORY_RATE, MEDICATION }

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = PetEntity::class,
            parentColumns = ["id"],
            childColumns = ["pet_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medication_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("pet_id"), Index("medication_id")],
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "pet_id") val petId: Long,
    @ColumnInfo(name = "medication_id") val medicationId: Long? = null,
    val type: ReminderType,
    val hour: Int,
    val minute: Int,
    @ColumnInfo(name = "days_mask") val daysMask: Int = DaysOfWeek.EVERY_DAY,
    val enabled: Boolean = true,
)
