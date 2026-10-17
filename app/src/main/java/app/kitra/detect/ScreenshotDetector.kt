package app.kitra.detect

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import app.kitra.core.ScreenshotFilter
import app.kitra.settings.Settings

/**
 * Detección de capturas nuevas sin servicio permanente (SPEC §3.1):
 * un job de JobScheduler que se dispara cuando cambia MediaStore. Los jobs con content URI son de un solo uso,
 * así que se reprograman tras cada ejecución, al arrancar el teléfono y al actualizar la app.
 */
object ScreenshotDetector {
    private const val JOB_ID = 1001

    /** Programa o cancela el job según la preferencia del usuario y los permisos. */
    fun sync(context: Context) {
        val settings = Settings(context)
        if (settings.detectionEnabled && Permissions.mediaAccess(context) == MediaAccess.FULL) schedule(context)
        else cancel(context)
    }

    fun schedule(context: Context) {
        val job = JobInfo.Builder(JOB_ID, ComponentName(context, ScreenshotJobService::class.java))
            .addTriggerContentUri(
                JobInfo.TriggerContentUri(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS,
                ),
            )
            // Agrupa la inserción y la actualización que hace el sistema al guardar una captura.
            .setTriggerContentUpdateDelay(UPDATE_DELAY_MS)
            .setTriggerContentMaxDelay(MAX_DELAY_MS)
            .build()
        context.getSystemService(JobScheduler::class.java).schedule(job)
    }

    fun cancel(context: Context) = context.getSystemService(JobScheduler::class.java).cancel(JOB_ID)

    const val UPDATE_DELAY_MS = 200L
    const val MAX_DELAY_MS = 1_000L

    /** Busca la captura más reciente aún no notificada. Devuelve su URI o null. */
    fun findNewScreenshot(context: Context, nowSeconds: Long = System.currentTimeMillis() / 1000): Pair<Long, Uri>? {
        val settings = Settings(context)
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
        )
        val selection = "${MediaStore.Images.Media.DATE_ADDED} >= ?"
        val args = arrayOf((nowSeconds - ScreenshotFilter.MAX_AGE_SECONDS).toString())
        val sort = "${MediaStore.Images.Media.DATE_ADDED} DESC, ${MediaStore.Images.Media._ID} DESC"
        // Por defecto MediaStore excluye las imágenes pendientes (aún escribiéndose).
        context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, args, sort)?.use { c ->
            var checked = 0
            while (c.moveToNext() && checked++ < 10) {
                val id = c.getLong(0)
                if (id <= settings.lastNotifiedMediaId) continue
                if (ScreenshotFilter.isScreenshot(c.getString(1), c.getString(2), c.getLong(3), nowSeconds)) {
                    return id to ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                }
            }
        }
        return null
    }
}
