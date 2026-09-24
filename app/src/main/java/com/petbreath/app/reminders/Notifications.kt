package com.petbreath.app.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.petbreath.app.MainActivity
import com.petbreath.app.R

object Notifications {
    const val CHANNEL_RRR = "rrr_reminders"
    const val CHANNEL_MEDICATION = "medication_reminders"

    fun createChannels(context: Context) {
        val manager = checkNotNull(context.getSystemService(NotificationManager::class.java))
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    CHANNEL_RRR,
                    context.getString(R.string.channel_rrr_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = context.getString(R.string.channel_rrr_description) },
                NotificationChannel(
                    CHANNEL_MEDICATION,
                    context.getString(R.string.channel_medication_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply { description = context.getString(R.string.channel_medication_description) },
            ),
        )
    }

    fun canPost(context: Context): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun showRrrReminder(context: Context, reminderId: Long, petId: Long, petName: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_RRR)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_rrr_title, petName))
            .setContentText(context.getString(R.string.notification_rrr_text, petName))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.notification_rrr_text, petName)),
            )
            .setContentIntent(openAppIntent(context, reminderId, petId, MainActivity.DESTINATION_MEASURE))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        notify(context, reminderId, notification)
    }

    fun showMedicationReminder(
        context: Context,
        reminderId: Long,
        petId: Long,
        petName: String,
        medicationId: Long,
        medicationName: String,
        dosage: String,
    ) {
        val text = if (dosage.isBlank()) {
            context.getString(R.string.notification_med_text_no_dose, medicationName, petName)
        } else {
            context.getString(R.string.notification_med_text, medicationName, dosage, petName)
        }
        val markGiven = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            Intent(context, DoseActionReceiver::class.java)
                .setAction(DoseActionReceiver.ACTION_MARK_GIVEN)
                .putExtra(DoseActionReceiver.EXTRA_MEDICATION_ID, medicationId)
                .putExtra(DoseActionReceiver.EXTRA_NOTIFICATION_ID, reminderId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_MEDICATION)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_med_title, petName))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openAppIntent(context, reminderId, petId, MainActivity.DESTINATION_MEDICATIONS))
            .addAction(0, context.getString(R.string.notification_action_mark_given), markGiven)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        notify(context, reminderId, notification)
    }

    fun cancel(context: Context, notificationId: Long) {
        NotificationManagerCompat.from(context).cancel(notificationId.toInt())
    }

    private fun notify(context: Context, id: Long, notification: android.app.Notification) {
        if (!canPost(context)) return
        try {
            NotificationManagerCompat.from(context).notify(id.toInt(), notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call; nothing else to do.
        }
    }

    private fun openAppIntent(context: Context, requestCode: Long, petId: Long, destination: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(MainActivity.EXTRA_PET_ID, petId)
            .putExtra(MainActivity.EXTRA_DESTINATION, destination)
        return PendingIntent.getActivity(
            context,
            requestCode.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
