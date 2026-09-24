package com.petbreath.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.petbreath.app.appContainer
import com.petbreath.app.data.db.ReminderType
import kotlinx.coroutines.launch

/** Runs [block] off the main thread while keeping the broadcast alive. */
private fun BroadcastReceiver.runAsync(context: Context, block: suspend () -> Unit) {
    val pending = goAsync()
    context.appContainer.applicationScope.launch {
        try {
            block()
        } finally {
            pending.finish()
        }
    }
}

/** Fires a single reminder, then schedules its next occurrence. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId < 0) return
        runAsync(context) {
            val container = context.appContainer
            val reminder = container.reminderRepository.get(reminderId) ?: return@runAsync
            if (!reminder.enabled) return@runAsync
            val pet = container.petRepository.getPet(reminder.petId)
            if (pet != null) {
                when (reminder.type) {
                    ReminderType.RESPIRATORY_RATE ->
                        Notifications.showRrrReminder(context, reminder.id, pet.id, pet.name)
                    ReminderType.MEDICATION -> {
                        val medication = reminder.medicationId?.let { container.medicationRepository.get(it) }
                        if (medication != null && medication.active) {
                            Notifications.showMedicationReminder(
                                context, reminder.id, pet.id, pet.name,
                                medication.id, medication.name, medication.dosage,
                            )
                        }
                    }
                }
            }
            container.reminderScheduler.schedule(reminder)
        }
    }

    companion object {
        const val ACTION_FIRE = "com.petbreath.app.action.FIRE_REMINDER"
        const val EXTRA_REMINDER_ID = "reminder_id"
    }
}

/** Alarms are cleared on reboot and must be re-registered after clock changes. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> runAsync(context) { context.appContainer.reminderRepository.rescheduleAll() }
        }
    }
}

/** Handles the "Mark as given" notification action. */
class DoseActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MARK_GIVEN) return
        val medicationId = intent.getLongExtra(EXTRA_MEDICATION_ID, -1L)
        val notificationId = intent.getLongExtra(EXTRA_NOTIFICATION_ID, -1L)
        if (medicationId < 0) return
        runAsync(context) {
            context.appContainer.medicationRepository.logDose(medicationId, System.currentTimeMillis())
            if (notificationId >= 0) Notifications.cancel(context, notificationId)
        }
    }

    companion object {
        const val ACTION_MARK_GIVEN = "com.petbreath.app.action.MARK_DOSE_GIVEN"
        const val EXTRA_MEDICATION_ID = "medication_id"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
}
