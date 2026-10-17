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
                if (Permissions.canDetect(this@ScreenshotJobService)) {
                    ScreenshotDetector.findNewScreenshot(this@ScreenshotJobService)?.let { (id, uri) ->
                        Settings(this@ScreenshotJobService).lastNotifiedMediaId = id
                        ScreenshotNotifier.show(this@ScreenshotJobService, uri)
                        // Solo métricas de tiempo; nunca contenido ni rutas de la captura.
                        Log.i(TAG, "Screenshot notified in ${SystemClock.elapsedRealtime() - started} ms after job start")
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
