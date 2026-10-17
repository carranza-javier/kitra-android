package app.kitra.detect

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Los jobs con content URI no sobreviven a un reinicio ni a una actualización: se vuelven a programar. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            ScreenshotDetector.sync(context)
        }
    }
}
