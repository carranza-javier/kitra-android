package com.kettenki.kitra.ui.setup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.kettenki.kitra.BuildConfig
import com.kettenki.kitra.KitraApp
import com.kettenki.kitra.R
import com.kettenki.kitra.core.Language
import com.kettenki.kitra.core.NanoState
import com.kettenki.kitra.core.gemini.GeminiProvider
import com.kettenki.kitra.detect.MediaAccess
import com.kettenki.kitra.detect.Permissions
import com.kettenki.kitra.detect.ScreenshotDetector
import com.kettenki.kitra.engine.LanguagePacks
import com.kettenki.kitra.engine.Nano
import com.kettenki.kitra.engine.PackState
import com.kettenki.kitra.engine.TextRecognitionEngine
import com.kettenki.kitra.settings.Settings
import com.kettenki.kitra.ui.translate.languageName
import kotlinx.coroutines.launch

/** Ajustes: idioma, aviso de capturas, Gemini, privacidad y avanzado. */
@Composable
fun SettingsScreen(settings: Settings, onOpenTutorial: () -> Unit, onOpenDiagnostics: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var language by remember { mutableStateOf(settings.targetLanguage) }
    var detection by remember { mutableStateOf(settings.detectionEnabled) }
    var hasKey by remember { mutableStateOf(settings.apiKey.hasKey()) }
    var showAdvanced by remember { mutableStateOf(false) }
    // Se incrementa para volver a leer permisos y estados (al volver de los ajustes del sistema, etc.).
    var refreshTick by remember { mutableIntStateOf(0) }
    val packStates by LanguagePacks.states.collectAsState()
    var nanoState by remember { mutableStateOf<NanoState?>(null) }
    var nanoBytes by remember { mutableStateOf<Long?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        refreshTick++
        ScreenshotDetector.sync(context)
    }
    fun requestPermissions() {
        settings.permissionsPrompted = true
        permissionLauncher.launch(Permissions.detectionPermissions())
    }

    fun requestTargetPack(lang: Language) {
        scope.launch {
            LanguagePacks.refresh(lang.code)
            if (LanguagePacks.states.value[lang.code] != PackState.READY) {
                LanguagePacks.request(context, lang.code, wifiOnly = true)
            }
            settings.targetPackRequested = lang.code
        }
    }

    fun downloadNano() {
        nanoState = NanoState.DOWNLOADING
        KitraApp.scope.launch {
            runCatching { Nano.download().collect { nanoBytes = it } }
            nanoState = Nano.state()
        }
    }

    // Primera vez tras la bienvenida: permisos, paquete de idioma y modelos se ponen en marcha solos.
    LaunchedEffect(Unit) {
        if (detection && !settings.permissionsPrompted && !Permissions.canDetect(context)) requestPermissions()
        if (settings.targetPackRequested != language.code) requestTargetPack(language) else LanguagePacks.refresh(language.code)
        KitraApp.scope.launch {
            if (!runCatching { TextRecognitionEngine.modulesReady(context) }.getOrDefault(true)) {
                runCatching { TextRecognitionEngine.installModules(context) }
            }
        }
        nanoState = Nano.state()
        if (nanoState == NanoState.DOWNLOADABLE && LanguagePacks.isUnmetered(context)) downloadNano()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        refreshTick++
        hasKey = settings.apiKey.hasKey()
        ScreenshotDetector.sync(context)
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.setup_tagline), style = MaterialTheme.typography.bodyLarge)

            GeminiSection(hasKey, onOpenTutorial, onRemoveKey = {
                settings.apiKey.clear()
                hasKey = false
            })

            Section(stringResource(R.string.setup_lang_title)) {
                LanguagePicker(language) {
                    language = it
                    settings.targetLanguage = it
                    requestTargetPack(it)
                }
                Text(stringResource(R.string.setup_pack_explainer), style = MaterialTheme.typography.bodySmall)
                PackStatus(packStates[language.code]) { LanguagePacks.request(context, language.code, wifiOnly = false) }
            }

            Section(stringResource(if (Permissions.broadPhotoAccess) R.string.setup_detect_title else R.string.setup_detect_title_picker)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(if (Permissions.broadPhotoAccess) R.string.setup_detect_desc else R.string.setup_detect_desc_picker),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.width(12.dp))
                    Switch(checked = detection, onCheckedChange = {
                        detection = it
                        settings.detectionEnabled = it
                        if (it && !Permissions.canDetect(context)) requestPermissions()
                        ScreenshotDetector.sync(context)
                    })
                }
                if (detection) {
                    // Leer refreshTick hace que este bloque se recalcule al cambiar los permisos.
                    val access = refreshTick.let { Permissions.mediaAccess(context) }
                    val notifications = refreshTick.let { Permissions.notificationsAllowed(context) }
                    when {
                        Permissions.broadPhotoAccess && access == MediaAccess.PARTIAL ->
                            PermissionHint(stringResource(R.string.setup_perm_partial)) { openAppSettings(context) }
                        Permissions.broadPhotoAccess && access == MediaAccess.DENIED -> {
                            Text(
                                stringResource(if (settings.permissionsPrompted) R.string.setup_perm_denied else R.string.setup_perm_needed),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = ::requestPermissions) { Text(stringResource(R.string.setup_perm_grant)) }
                                if (settings.permissionsPrompted) {
                                    OutlinedButton(onClick = { openAppSettings(context) }) { Text(stringResource(R.string.open_settings)) }
                                }
                            }
                        }
                        !notifications -> {
                            PermissionHint(stringResource(R.string.setup_notif_denied)) { openAppSettings(context) }
                            if (!settings.permissionsPrompted) Button(onClick = ::requestPermissions) { Text(stringResource(R.string.setup_perm_grant)) }
                        }
                        else -> Text(stringResource(R.string.setup_perm_ok), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Section(stringResource(R.string.setup_share_title)) {
                Text(stringResource(R.string.setup_share_desc), style = MaterialTheme.typography.bodyMedium)
            }

            Section(stringResource(R.string.setup_privacy_title), highlighted = true) {
                Text(stringResource(R.string.setup_privacy_body), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { openUrl(context, PRIVACY_POLICY_URL) }) { Text(stringResource(R.string.setup_privacy_policy)) }
            }

            TextButton(onClick = { showAdvanced = !showAdvanced }) { Text(stringResource(R.string.setup_advanced)) }
            if (showAdvanced) {
                AdvancedSettings(settings, nanoState, nanoBytes, onDownloadNano = ::downloadNano, onOpenDiagnostics = onOpenDiagnostics)
            }
        }
    }
}

const val PRIVACY_POLICY_URL = "https://kettenki.com/kitra/privacidad"

@Composable
private fun GeminiSection(hasKey: Boolean, onOpenTutorial: () -> Unit, onRemoveKey: () -> Unit) {
    Section(stringResource(R.string.setup_gemini_title), highlighted = !hasKey) {
        if (hasKey) {
            Text(stringResource(R.string.setup_gemini_on), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenTutorial) { Text(stringResource(R.string.setup_gemini_change)) }
                TextButton(onClick = onRemoveKey) { Text(stringResource(R.string.setup_gemini_remove)) }
            }
        } else {
            Text(stringResource(R.string.setup_gemini_off), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onOpenTutorial, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(stringResource(R.string.welcome_set_up_key))
            }
        }
    }
}

@Composable
private fun AdvancedSettings(
    settings: Settings,
    nanoState: NanoState?,
    nanoBytes: Long?,
    onDownloadNano: () -> Unit,
    onOpenDiagnostics: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var downloaded by remember { mutableStateOf<List<String>?>(null) }
    var model by remember { mutableStateOf(settings.model) }
    var fake by remember { mutableStateOf(settings.useFakeProvider) }
    LaunchedEffect(Unit) { downloaded = runCatching { LanguagePacks.downloaded() }.getOrDefault(emptyList()) }

    Section(stringResource(R.string.setup_diagnostics_title)) {
        Text(stringResource(R.string.setup_diagnostics_desc), style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = onOpenDiagnostics) { Text(stringResource(R.string.setup_diagnostics_open)) }
    }

    Section(stringResource(R.string.setup_basic_engine_title)) {
        Text(stringResource(R.string.setup_basic_engine_desc), style = MaterialTheme.typography.bodyMedium)
        when (nanoState) {
            NanoState.AVAILABLE -> Text(stringResource(R.string.setup_nano_available), style = MaterialTheme.typography.bodySmall)
            NanoState.DOWNLOADABLE -> {
                Text(stringResource(R.string.setup_nano_downloadable), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onDownloadNano) { Text(stringResource(R.string.setup_nano_download)) }
            }
            NanoState.DOWNLOADING -> Text(
                stringResource(R.string.setup_nano_downloading) + (nanoBytes?.let { " ${it / (1024 * 1024)} MB" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
            )
            NanoState.UNAVAILABLE -> Text(stringResource(R.string.setup_nano_unavailable), style = MaterialTheme.typography.bodySmall)
            null -> Unit
        }
    }

    Section(stringResource(R.string.setup_packs_title)) {
        val list = downloaded
        if (list != null && list.isEmpty()) Text(stringResource(R.string.setup_packs_empty), style = MaterialTheme.typography.bodyMedium)
        list?.forEach { code ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(languageName(code).replaceFirstChar { it.uppercase() }, Modifier.weight(1f))
                TextButton(onClick = {
                    scope.launch {
                        runCatching { LanguagePacks.delete(code) }
                        downloaded = runCatching { LanguagePacks.downloaded() }.getOrDefault(emptyList())
                    }
                }) { Text(stringResource(R.string.setup_pack_delete)) }
            }
        }
    }

    Section(stringResource(R.string.setup_model)) {
        OutlinedTextField(
            value = model,
            onValueChange = { model = it; settings.model = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = { Text(stringResource(R.string.setup_model_desc, GeminiProvider.DEFAULT_MODEL)) },
        )
    }

    Section(stringResource(R.string.setup_legal_title)) {
        Text(stringResource(R.string.setup_legal_body), style = MaterialTheme.typography.bodySmall)
    }

    if (BuildConfig.DEBUG) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.setup_debug_fake), Modifier.weight(1f))
            Switch(checked = fake, onCheckedChange = { fake = it; settings.useFakeProvider = it })
        }
    }
}
