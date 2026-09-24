package com.petbreath.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PetEntity::class,
        MeasurementEntity::class,
        MedicationEntity::class,
        MedicationDoseEntity::class,
        ReminderEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class PetBreathDatabase : RoomDatabase() {
    abstract fun petDao(): PetDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun medicationDao(): MedicationDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        const val NAME = "petbreath.db"

        fun build(context: Context): PetBreathDatabase =
            Room.databaseBuilder(context, PetBreathDatabase::class.java, NAME).build()
    }
}
