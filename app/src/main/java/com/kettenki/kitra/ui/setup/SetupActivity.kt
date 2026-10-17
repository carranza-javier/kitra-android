package com.kettenki.kitra.ui.setup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.kettenki.kitra.BuildConfig
import com.kettenki.kitra.core.Engine
import com.kettenki.kitra.settings.Settings
import com.kettenki.kitra.ui.KitraTheme

/** Pantallas de la configuración. */
enum class SetupScreen { WELCOME, TUTORIAL, SETTINGS, DIAGNOSTICS }

/**
 * Configuración (SPEC §5 y §6): bienvenida solo la primera vez, tutorial de la clave de Gemini,
 * ajustes (idioma, permisos, Gemini, avanzado) y diagnóstico.
 */
class SetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val settings = Settings(this)
        applyDebugExtras(intent)
        val openedForTutorial = intent.getBooleanExtra(EXTRA_OPEN_TUTORIAL, false)
        val start = when {
            openedForTutorial -> SetupScreen.TUTORIAL
            !settings.welcomeDone -> SetupScreen.WELCOME
            else -> SetupScreen.SETTINGS
        }
        setContent {
            KitraTheme {
                var screen by rememberSaveable { mutableStateOf(start) }
                fun leaveTutorial() {
                    settings.welcomeDone = true
                    // Si se llegó desde la ventana de resultado, se vuelve a lo que el usuario estaba haciendo.
                    if (openedForTutorial) finish() else screen = SetupScreen.SETTINGS
                }
                BackHandler(enabled = screen == SetupScreen.TUTORIAL || screen == SetupScreen.DIAGNOSTICS) {
                    if (screen == SetupScreen.TUTORIAL) leaveTutorial() else screen = SetupScreen.SETTINGS
                }
                when (screen) {
                    SetupScreen.WELCOME -> WelcomeScreen(
                        onSetUpKey = { screen = SetupScreen.TUTORIAL },
                        onBasicMode = {
                            settings.welcomeDone = true
                            screen = SetupScreen.SETTINGS
                        },
                    )
                    SetupScreen.TUTORIAL -> KeyTutorialScreen(settings, onDone = ::leaveTutorial)
                    SetupScreen.SETTINGS -> SettingsScreen(
                        settings,
                        onOpenTutorial = { screen = SetupScreen.TUTORIAL },
                        onOpenDiagnostics = { screen = SetupScreen.DIAGNOSTICS },
                    )
                    SetupScreen.DIAGNOSTICS -> DiagnosticsScreen(settings, onBack = { screen = SetupScreen.SETTINGS })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyDebugExtras(intent)
        recreate()
    }

    /** Solo depuración: opciones para pruebas automatizadas desde adb. */
    private fun applyDebugExtras(intent: Intent) {
        if (!BuildConfig.DEBUG) return
        val settings = Settings(this)
        if (intent.hasExtra(EXTRA_DEBUG_FAKE)) settings.useFakeProvider = intent.getBooleanExtra(EXTRA_DEBUG_FAKE, false)
        intent.getStringExtra(EXTRA_DEBUG_ENGINE)?.let { value ->
            settings.engineOverride = when (value.lowercase()) {
                "nano" -> Engine.GEMINI_NANO
                "mlkit" -> Engine.ML_KIT
                "cloud" -> Engine.GEMINI_CLOUD
                else -> null
            }
        }
        // Guarda una clave sin validarla, para probar qué pasa con una clave que ha dejado de funcionar.
        intent.getStringExtra(EXTRA_DEBUG_SET_KEY)?.let { settings.apiKey.save(it) }
        if (intent.getBooleanExtra(EXTRA_DEBUG_CLEAR_KEY, false)) settings.apiKey.clear()
    }

    companion object {
        const val EXTRA_OPEN_TUTORIAL = "open_tutorial"
        const val EXTRA_DEBUG_FAKE = "debug_fake_provider"
        const val EXTRA_DEBUG_ENGINE = "debug_engine"
        const val EXTRA_DEBUG_SET_KEY = "debug_set_key"
        const val EXTRA_DEBUG_CLEAR_KEY = "debug_clear_key"

        /** Abre directamente el tutorial de la clave (desde el aviso de modo básico). */
        fun tutorialIntent(context: Context): Intent =
            Intent(context, SetupActivity::class.java)
                .putExtra(EXTRA_OPEN_TUTORIAL, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}
