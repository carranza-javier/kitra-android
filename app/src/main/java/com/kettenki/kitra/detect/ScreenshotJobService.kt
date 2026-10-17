package com.kettenki.kitra.detect

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.SystemClock
import android.util.Log
import com.kettenki.kitra.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ScreenshotJobService : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartJob(params: JobParameters): Boolean {
        val started = SystemClock.elapsedRealtime()
        scope.launch {
            try {
                val context = this@ScreenshotJobService
                if (Permissions.canDetect(context)) {
                    if (Permissions.broadPhotoAccess) {
                        ScreenshotDetector.findNewScreenshot(context)?.let { (id, uri) ->
                            Settings(context).lastNotifiedMediaId = id
                            ScreenshotNotifier.show(context, uri)
                            // Solo métricas de tiempo; nunca contenido ni rutas de la captura.
                            Log.i(TAG, "Screenshot notified in ${SystemClock.elapsedRealtime() - started} ms after job start")
                        }
                    } else {
                        ScreenshotDetector.newImageIdFromTrigger(context, params.triggeredContentUris)?.let { id ->
                            Settings(context).lastNotifiedMediaId = id
                            ScreenshotNotifier.show(context, imageUri = null)
                            Log.i(TAG, "New image notified (picker mode) in ${SystemClock.elapsedRealtime() - started} ms after job start")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Screenshot check failed: ${e.javaClass.simpleName}")
            } finally {
                jobFinished(params, false)
                // Los jobs con content URI se disparan una sola vez: volver a escuchar.
                ScreenshotDetector.sync(this@ScreenshotJobService)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        scope.cancel()
        ScreenshotDetector.sync(this)
        return false
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "Kitra"
    }
}
