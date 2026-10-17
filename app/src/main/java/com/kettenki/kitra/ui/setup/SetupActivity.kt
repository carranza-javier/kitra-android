package com.kettenki.kitra.ui.setup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings as SystemSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.kettenki.kitra.BuildConfig
import com.kettenki.kitra.KitraApp
import com.kettenki.kitra.R
import com.kettenki.kitra.core.Engine
import com.kettenki.kitra.core.Language
import com.kettenki.kitra.core.NanoState
import com.kettenki.kitra.core.TranslationException
import com.kettenki.kitra.core.gemini.GeminiProvider
import com.kettenki.kitra.detect.MediaAccess
import com.kettenki.kitra.detect.Permissions
import com.kettenki.kitra.detect.ScreenshotDetector
import com.kettenki.kitra.engine.Engines
import com.kettenki.kitra.engine.LanguagePacks
import com.kettenki.kitra.engine.Nano
import com.kettenki.kitra.engine.PackState
import com.kettenki.kitra.engine.TextRecognitionEngine
import com.kettenki.kitra.settings.Settings
import com.kettenki.kitra.ui.KitraTheme
import com.kettenki.kitra.ui.translate.languageName
import kotlinx.coroutines.launch

/**
 * Configuración inicial y ajustes (SPEC §6). Sin clave: solo idioma de destino y permisos.
 * Lo demás (motor, Gemini en la nube, idiomas descargados) está en «Avanzado».
 */
class SetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val settings = Settings(this)
        applyDebugExtras(intent)
        setContent { KitraTheme { SetupScreen(settings) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        applyDebugExtras(intent)
        recreate()
    }

    /** Solo depuración: permite elegir proveedor desde adb para pruebas automatizadas y comparativas. */
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
    }

    companion object {
        const val EXTRA_DEBUG_FAKE = "debug_fake_provider"
        const val EXTRA_DEBUG_ENGINE = "debug_engine"
    }
}

private enum class KeyStatus { IDLE, TESTING, OK }

@Composable
private fun SetupScreen(settings: Settings) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var language by remember { mutableStateOf(settings.targetLanguage) }
    var detection by remember { mutableStateOf(settings.detectionEnabled) }
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

    // Primera apertura: todo lo necesario se pone en marcha solo, sin que el usuario tenga que buscarlo.
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

            Section(stringResource(R.string.setup_lang_title)) {
                LanguagePicker(language) {
                    language = it
                    settings.targetLanguage = it
                    requestTargetPack(it)
                }
                PackStatus(packStates[language.code]) { LanguagePacks.request(context, language.code, wifiOnly = false) }
            }

            Section(stringResource(R.string.setup_detect_title)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.setup_detect_desc), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
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
                        access == MediaAccess.PARTIAL -> PermissionHint(stringResource(R.string.setup_perm_partial)) { openAppSettings(context) }
                        access == MediaAccess.DENIED && settings.permissionsPrompted -> {
                            Text(stringResource(R.string.setup_perm_denied), style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = ::requestPermissions) { Text(stringResource(R.string.setup_perm_grant)) }
                                OutlinedButton(onClick = { openAppSettings(context) }) { Text(stringResource(R.string.open_settings)) }
                            }
                        }
                        access == MediaAccess.DENIED -> {
                            Text(stringResource(R.string.setup_perm_needed), style = MaterialTheme.typography.bodyMedium)
                            Button(onClick = ::requestPermissions) { Text(stringResource(R.string.setup_perm_grant)) }
                        }
                        !notifications -> PermissionHint(stringResource(R.string.setup_notif_denied)) { openAppSettings(context) }
                        else -> Text(stringResource(R.string.setup_perm_ok), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Section(stringResource(R.string.setup_share_title)) {
                Text(stringResource(R.string.setup_share_desc), style = MaterialTheme.typography.bodyMedium)
            }

            Section(stringResource(R.string.setup_privacy_title), highlighted = true) {
                Text(stringResource(R.string.setup_privacy_body), style = MaterialTheme.typography.bodyMedium)
            }

            TextButton(onClick = { showAdvanced = !showAdvanced }) { Text(stringResource(R.string.setup_advanced)) }
            if (showAdvanced) {
                AdvancedSettings(settings, nanoState, nanoBytes, onDownloadNano = ::downloadNano)
            }
        }
    }
}

@Composable
private fun PackStatus(state: PackState?, onDownloadNow: () -> Unit) {
    val style = MaterialTheme.typography.bodyMedium
    when (state) {
        PackState.READY -> Text(stringResource(R.string.setup_pack_ready), color = MaterialTheme.colorScheme.primary, style = style)
        PackState.DOWNLOADING -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.setup_pack_downloading), style = style)
        }
        PackState.WAITING_FOR_WIFI -> {
            Text(stringResource(R.string.setup_pack_waiting_wifi), style = style)
            TextButton(onClick = onDownloadNow) { Text(stringResource(R.string.setup_pack_download_now)) }
        }
        PackState.FAILED -> {
            Text(stringResource(R.string.setup_pack_failed), color = MaterialTheme.colorScheme.error, style = style)
            TextButton(onClick = onDownloadNow) { Text(stringResource(R.string.retry)) }
        }
        PackState.NOT_DOWNLOADED -> {
            Text(stringResource(R.string.setup_pack_missing), style = style)
            TextButton(onClick = onDownloadNow) { Text(stringResource(R.string.setup_pack_download_now)) }
        }
        PackState.UNKNOWN, null -> Unit
    }
}

@Composable
private fun AdvancedSettings(settings: Settings, nanoState: NanoState?, nanoBytes: Long?, onDownloadNano: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cloud by remember { mutableStateOf(settings.cloudEnabled) }
    var hasKey by remember { mutableStateOf(settings.apiKey.hasKey()) }
    var downloaded by remember { mutableStateOf<List<String>?>(null) }
    var fake by remember { mutableStateOf(settings.useFakeProvider) }
    LaunchedEffect(Unit) { downloaded = runCatching { LanguagePacks.downloaded() }.getOrDefault(emptyList()) }

    val engineName = stringResource(
        when {
            cloud && hasKey -> R.string.setup_engine_cloud
            nanoState == NanoState.AVAILABLE -> R.string.setup_engine_nano
            else -> R.string.setup_engine_mlkit
        },
    )
    Section(stringResource(R.string.setup_engine_title)) {
        Text(stringResource(R.string.setup_engine_auto, engineName), style = MaterialTheme.typography.bodyMedium)
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

    Section(stringResource(R.string.setup_cloud_title)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.setup_cloud_desc), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.width(12.dp))
            Switch(checked = cloud, onCheckedChange = { cloud = it; settings.cloudEnabled = it })
        }
        if (cloud) CloudKeySection(settings) { hasKey = it }
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

    Section(stringResource(R.string.setup_legal_title)) {
        Text(stringResource(R.string.setup_legal_body), style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { openUrl(context, "https://ai.google.dev/gemini-api/terms") }) {
            Text(stringResource(R.string.setup_privacy_terms))
        }
    }

    if (BuildConfig.DEBUG) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.setup_debug_fake), Modifier.weight(1f))
            Switch(checked = fake, onCheckedChange = { fake = it; settings.useFakeProvider = it })
        }
    }
}

/** Clave de Gemini API para el modo opcional en la nube. Solo se guarda si la prueba de conexión pasa. */
@Composable
private fun CloudKeySection(settings: Settings, onHasKeyChanged: (Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasKey by remember { mutableStateOf(settings.apiKey.hasKey()) }
    var editingKey by remember { mutableStateOf(!hasKey) }
    var keyInput by remember { mutableStateOf("") }
    var keyStatus by remember { mutableStateOf(KeyStatus.IDLE) }
    var keyError by remember { mutableStateOf<Int?>(null) }
    var model by remember { mutableStateOf(settings.model) }

    fun testKey(newKey: String?) {
        keyStatus = KeyStatus.TESTING
        keyError = null
        scope.launch {
            val key = newKey ?: settings.apiKey.load()
            try {
                if (key == null) throw TranslationException.InvalidKey()
                Engines.gemini(key, settings.model).verify()
                if (newKey != null) settings.apiKey.save(newKey)
                hasKey = true
                onHasKeyChanged(true)
                editingKey = false
                keyInput = ""
                keyStatus = KeyStatus.OK
            } catch (e: Exception) {
                keyStatus = KeyStatus.IDLE
                keyError = when (e) {
                    is TranslationException.InvalidKey -> R.string.error_invalid_key
                    is TranslationException.NoConnection -> R.string.error_no_connection
                    is TranslationException.QuotaExceeded -> R.string.error_quota
                    is TranslationException.ModelNotFound -> R.string.error_model
                    is TranslationException.Unavailable -> R.string.error_unavailable
                    else -> R.string.error_unknown
                }
            }
        }
    }

    Text(stringResource(R.string.setup_key_desc), style = MaterialTheme.typography.bodyMedium)
    if (editingKey) {
        TextButton(onClick = { openUrl(context, "https://aistudio.google.com/apikey") }) {
            Text(stringResource(R.string.setup_key_get))
        }
        OutlinedTextField(
            value = keyInput,
            onValueChange = { keyInput = it.trim() },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.setup_key_hint)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                enabled = keyInput.isNotBlank() && keyStatus != KeyStatus.TESTING,
                onClick = { testKey(keyInput) },
            ) { Text(stringResource(R.string.setup_key_save)) }
            if (hasKey) TextButton(onClick = { editingKey = false; keyInput = "" }) { Text(stringResource(android.R.string.cancel)) }
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(if (keyStatus == KeyStatus.OK) R.string.setup_key_ok else R.string.setup_key_saved),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.primary,
            )
            TextButton(enabled = keyStatus != KeyStatus.TESTING, onClick = { testKey(null) }) {
                Text(stringResource(R.string.setup_key_test))
            }
            TextButton(onClick = { editingKey = true; keyStatus = KeyStatus.IDLE }) { Text(stringResource(R.string.setup_key_change)) }
        }
    }
    if (keyStatus == KeyStatus.TESTING) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.setup_key_testing), style = MaterialTheme.typography.bodyMedium)
        }
    }
    keyError?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
    OutlinedTextField(
        value = model,
        onValueChange = { model = it; settings.model = it },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(stringResource(R.string.setup_model)) },
        supportingText = { Text(stringResource(R.string.setup_model_desc, GeminiProvider.DEFAULT_MODEL)) },
    )
}

@Composable
private fun Section(title: String, highlighted: Boolean = false, content: @Composable () -> Unit) {
    Card(
        colors = if (highlighted) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        else CardDefaults.cardColors(),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun PermissionHint(text: String, onOpenSettings: () -> Unit) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
    OutlinedButton(onClick = onOpenSettings) { Text(stringResource(R.string.open_settings)) }
}

@Composable
private fun LanguagePicker(selected: Language, onSelect: (Language) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected.nativeName, Modifier.weight(1f))
            Text("▾")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Language.entries.forEach { lang ->
                DropdownMenuItem(text = { Text(lang.nativeName) }, onClick = { onSelect(lang); expanded = false })
            }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
}

private fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(SystemSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
    )
}
