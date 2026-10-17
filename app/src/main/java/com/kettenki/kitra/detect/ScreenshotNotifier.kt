package com.kettenki.kitra.detect

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.kettenki.kitra.R
import com.kettenki.kitra.ui.translate.TranslateActivity

/** Aviso heads-up con la acción "Traducir" (SPEC §3.1). */
object ScreenshotNotifier {
    private const val CHANNEL_ID = "screenshot_translate"
    /** Id fijo: un aviso nuevo sustituye al anterior y nunca se acumulan en la bandeja. */
    const val NOTIFICATION_ID = 1

    /** Tiempo tras el que el aviso desaparece solo si no se usa. */
    const val TIMEOUT_MS = 8_000L

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_screenshots),
            NotificationManager.IMPORTANCE_HIGH, // necesario para que aparezca como heads-up
        ).apply {
            description = context.getString(R.string.channel_screenshots_desc)
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /**
     * Con [imageUri] (variante «full») el aviso traduce esa captura directamente.
     * Sin ella (variante «play», sin permiso de fotos) abre el selector de fotos del sistema.
     */
    fun show(context: Context, imageUri: Uri?) {
        val intent = if (imageUri != null) TranslateActivity.intentFor(context, imageUri) else TranslateActivity.pickIntent(context)
        val pending = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val translate = Notification.Action.Builder(null, context.getString(R.string.action_translate), pending).build()
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(if (imageUri != null) R.string.notif_title else R.string.notif_title_picker))
            .setContentText(context.getString(if (imageUri != null) R.string.notif_text else R.string.notif_text_picker))
            .setContentIntent(pending) // pulsar el aviso entero también traduce
            .addAction(translate)
            .setAutoCancel(true)
            .setTimeoutAfter(TIMEOUT_MS)
            .setCategory(Notification.CATEGORY_RECOMMENDATION)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setShowWhen(false)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    fun dismiss(context: Context) = context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
}
