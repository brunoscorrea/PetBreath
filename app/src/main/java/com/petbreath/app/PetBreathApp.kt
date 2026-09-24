package com.petbreath.app

import android.app.Application
import android.content.Context
import com.petbreath.app.data.db.PetBreathDatabase
import com.petbreath.app.data.repository.MeasurementRepository
import com.petbreath.app.data.repository.MedicationRepository
import com.petbreath.app.data.repository.PetRepository
import com.petbreath.app.data.repository.ReminderRepository
import com.petbreath.app.data.settings.SettingsRepository
import com.petbreath.app.export.ReportExporter
import com.petbreath.app.reminders.AlarmReminderScheduler
import com.petbreath.app.reminders.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/** Minimal manual dependency container; everything lives on the device. */
class AppContainer(context: Context) {
    val applicationScope = CoroutineScope(SupervisorJob())
    val database: PetBreathDatabase = PetBreathDatabase.build(context)
    val reminderScheduler = AlarmReminderScheduler(context)
    val settingsRepository = SettingsRepository(context)
    val petRepository = PetRepository(database, reminderScheduler)
    val measurementRepository = MeasurementRepository(database)
    val medicationRepository = MedicationRepository(database, reminderScheduler)
    val reminderRepository = ReminderRepository(database, reminderScheduler)
    val reportExporter = ReportExporter(context.applicationContext, measurementRepository, medicationRepository)
}

class PetBreathApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as PetBreathApp).container
