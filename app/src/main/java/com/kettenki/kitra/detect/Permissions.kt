package com.kettenki.kitra.detect

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kettenki.kitra.BuildConfig

enum class MediaAccess { FULL, PARTIAL, DENIED }

object Permissions {

    /** Variante «full»: permiso amplio de fotos para leer cada captura nueva. «play»: sin permiso de fotos. */
    val broadPhotoAccess: Boolean get() = BuildConfig.BROAD_PHOTO_ACCESS

    /** Permisos que se piden al activar el aviso de capturas. */
    fun detectionPermissions(): Array<String> = buildList {
        if (broadPhotoAccess) {
            when {
                Build.VERSION.SDK_INT >= 34 -> {
                    add("android.permission.READ_MEDIA_IMAGES")
                    add("android.permission.READ_MEDIA_VISUAL_USER_SELECTED")
                }
                Build.VERSION.SDK_INT >= 33 -> add("android.permission.READ_MEDIA_IMAGES")
                else -> add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()

    fun mediaAccess(context: Context): MediaAccess {
        fun granted(p: String) = ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
        return when {
            Build.VERSION.SDK_INT >= 33 && granted("android.permission.READ_MEDIA_IMAGES") -> MediaAccess.FULL
            Build.VERSION.SDK_INT < 33 && granted(Manifest.permission.READ_EXTERNAL_STORAGE) -> MediaAccess.FULL
            // Android 14+: acceso solo a "fotos seleccionadas"; las capturas nuevas no serán visibles.
            Build.VERSION.SDK_INT >= 34 && granted("android.permission.READ_MEDIA_VISUAL_USER_SELECTED") -> MediaAccess.PARTIAL
            else -> MediaAccess.DENIED
        }
    }

    fun notificationsAllowed(context: Context): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Si el aviso de capturas puede funcionar con los permisos actuales. */
    fun canDetect(context: Context): Boolean =
        notificationsAllowed(context) && (!broadPhotoAccess || mediaAccess(context) == MediaAccess.FULL)
}
