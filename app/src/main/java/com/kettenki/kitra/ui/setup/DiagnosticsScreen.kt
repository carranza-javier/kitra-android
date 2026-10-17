package com.kettenki.kitra.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kettenki.kitra.BuildConfig
import com.kettenki.kitra.R
import com.kettenki.kitra.core.BasicModeReason
import com.kettenki.kitra.core.Engine
import com.kettenki.kitra.core.NanoState
import com.kettenki.kitra.engine.LanguagePacks
import com.kettenki.kitra.engine.Nano
import com.kettenki.kitra.engine.TextRecognitionEngine
import com.kettenki.kitra.net.Connectivity
import com.kettenki.kitra.settings.Settings
import com.kettenki.kitra.settings.TranslationLogEntry
import com.kettenki.kitra.ui.translate.languageName
import java.text.DateFormat
import java.util.Date

/** Qué motores hay disponibles y cuál se ha usado en cada traducción. Nunca muestra texto traducido. */
@Composable
fun DiagnosticsScreen(settings: Settings, onBack: () -> Unit) {
    val context = LocalContext.current
    var nano by remember { mutableStateOf<NanoState?>(null) }
    var ocrReady by remember { mutableStateOf<Boolean?>(null) }
    var packs by remember { mutableStateOf<List<String>>(emptyList()) }
    var log by remember { mutableStateOf(settings.translationLog.entries()) }
    LaunchedEffect(Unit) {
        nano = Nano.state()
        ocrReady = runCatching { TextRecognitionEngine.modulesReady(context) }.getOrDefault(false)
        packs = runCatching { LanguagePacks.downloaded() }.getOrDefault(emptyList())
    }
    val yes = stringResource(R.string.diag_yes)
    val no = stringResource(R.string.diag_no)

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.diag_back)) }
            Text(stringResource(R.string.setup_diagnostics_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)

            Section(stringResource(R.string.diag_engines_title)) {
                DiagRow(stringResource(R.string.diag_key), if (settings.apiKey.hasKey()) yes else no)
                DiagRow(stringResource(R.string.diag_online), if (Connectivity.isOnline(context)) yes else no)
                DiagRow(
                    stringResource(R.string.diag_nano),
                    when (nano) {
                        NanoState.AVAILABLE -> yes
                        NanoState.DOWNLOADABLE -> stringResource(R.string.diag_nano_downloadable)
                        NanoState.DOWNLOADING -> stringResource(R.string.diag_nano_downloading)
                        NanoState.UNAVAILABLE -> no
                        null -> "…"
                    },
                )
                DiagRow(stringResource(R.string.diag_ocr), when (ocrReady) { true -> yes; false -> no; null -> "…" })
                DiagRow(
                    stringResource(R.string.diag_packs),
                    packs.joinToString { languageName(it) }.ifEmpty { stringResource(R.string.setup_packs_empty) },
                )
                DiagRow(stringResource(R.string.diag_variant), "${BuildConfig.FLAVOR} · ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            }

            Section(stringResource(R.string.diag_log_title)) {
                if (log.isEmpty()) Text(stringResource(R.string.diag_log_empty), style = MaterialTheme.typography.bodyMedium)
                log.forEachIndexed { i, entry ->
                    if (i > 0) HorizontalDivider()
                    LogRow(entry)
                }
                if (log.isNotEmpty()) {
                    OutlinedButton(onClick = { settings.translationLog.clear(); log = emptyList() }) {
                        Text(stringResource(R.string.diag_log_clear))
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagRow(label: String, value: String) {
    Row {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun LogRow(entry: TranslationLogEntry) {
    val time = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date(entry.timeMillis))
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("$time · ${engineName(entry.engine)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        entry.basicReason?.let { Text(stringResource(R.string.diag_reason, reasonName(it)), style = MaterialTheme.typography.bodySmall) }
        val timing = listOfNotNull(
            entry.firstTextMs?.let { stringResource(R.string.diag_first_text, it) },
            entry.totalMs?.let { stringResource(R.string.diag_total, it) },
        ).joinToString(" · ")
        if (timing.isNotEmpty()) Text(timing, style = MaterialTheme.typography.bodySmall)
        entry.error?.let { Text(stringResource(R.string.diag_error, it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun engineName(engine: Engine?): String = stringResource(
    when (engine) {
        Engine.GEMINI_CLOUD -> R.string.engine_cloud
        Engine.GEMINI_NANO -> R.string.engine_nano
        Engine.ML_KIT -> R.string.engine_mlkit
        Engine.FAKE -> R.string.engine_fake
        null -> R.string.engine_none
    },
)

@Composable
private fun reasonName(reason: BasicModeReason): String = stringResource(
    when (reason) {
        BasicModeReason.NO_KEY -> R.string.reason_no_key
        BasicModeReason.OFFLINE -> R.string.reason_offline
        BasicModeReason.QUOTA_EXCEEDED -> R.string.reason_quota
        BasicModeReason.KEY_INVALID -> R.string.reason_key_invalid
        BasicModeReason.CLOUD_FAILED -> R.string.reason_cloud_failed
    },
)
