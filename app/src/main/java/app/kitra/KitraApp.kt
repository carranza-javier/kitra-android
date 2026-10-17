package app.kitra

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import app.kitra.detect.ScreenshotDetector
import app.kitra.detect.ScreenshotNotifier
import app.kitra.ui.setup.SetupActivity

class KitraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ScreenshotNotifier.createChannel(this)
        ScreenshotDetector.sync(this)
        publishShareShortcut(this)
        TempFiles.clear(this)
    }

    /** Acceso directo de compartir para aparecer en la fila de Direct Share. */
    private fun publishShareShortcut(context: Context) {
        val shortcut = ShortcutInfoCompat.Builder(context, "translate_share")
            .setShortLabel(context.getString(R.string.shortcut_share))
            .setLongLived(true)
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setCategories(setOf("app.kitra.category.TRANSLATE_SHARE_TARGET"))
            .setIntent(Intent(context, SetupActivity::class.java).setAction(Intent.ACTION_VIEW))
            .build()
        runCatching { ShortcutManagerCompat.pushDynamicShortcut(context, shortcut) }
    }
}

/** Kitra no crea temporales; por seguridad, se vacía la caché al cerrar cada traducción. */
object TempFiles {
    fun clear(context: Context) {
        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
    }
}
