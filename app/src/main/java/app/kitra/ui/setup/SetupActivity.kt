package app.kitra.ui.setup

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings as SystemSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import app.kitra.BuildConfig
import app.kitra.ProviderFactory
import app.kitra.R
import app.kitra.core.Language
import app.kitra.core.TranslationException
import app.kitra.core.gemini.GeminiProvider
import app.kitra.detect.MediaAccess
import app.kitra.detect.Permissions
import app.kitra.detect.ScreenshotDetector
import app.kitra.settings.Settings
import app.kitra.ui.KitraTheme
import kotlinx.coroutines.launch

/** Configuración inicial y ajustes (SPEC §5 y §6): una sola pantalla que solo se abre desde el lanzador. */
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

    /** Solo depuración: permite activar el proveedor falso desde adb para pruebas automatizadas. */
    private fun applyDebugExtras(intent: Intent) {
        if (BuildConfig.DEBUG && intent.hasExtra(EXTRA_DEBUG_FAKE)) {
            Settings(this).useFakeProvider = intent.getBooleanExtra(EXTRA_DEBUG_FAKE, false)
        }
    }

    companion object {
        const val EXTRA_DEBUG_FAKE = "debug_fake_provider"
    }
}

private enum class KeyStatus { IDLE, TESTING, OK }

@Composable
private fun SetupScreen(settings: Settings) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasKey by remember { mutableStateOf(settings.apiKey.hasKey()) }
    var editingKey by remember { mutableStateOf(!hasKey) }
    var keyInput by remember { mutableStateOf("") }
    var keyStatus by remember { mutableStateOf(KeyStatus.IDLE) }
    var keyError by remember { mutableStateOf<Int?>(null) }
    var language by remember { mutableStateOf(settings.targetLanguage) }
    var detection by remember { mutableStateOf(settings.detectionEnabled) }
    var fake by remember { mutableStateOf(settings.useFakeProvider) }
    var model by remember { mutableStateOf(settings.model) }
    var showAdvanced by remember { mutableStateOf(false) }
    // Se incrementa para volver a leer los permisos (al volver de los ajustes del sistema, etc.).
    var permissionsTick by remember { mutableIntStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permissionsTick++
        ScreenshotDetector.sync(context)
    }
    fun requestPermissions() {
        settings.permissionsPrompted = true
        permissionLauncher.launch(Permissions.detectionPermissions())
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissionsTick++
        ScreenshotDetector.sync(context)
    }

    fun testKey(newKey: String?) {
        keyStatus = KeyStatus.TESTING
        keyError = null
        scope.launch {
            val key = newKey ?: settings.apiKey.load()
            try {
                if (key == null) throw TranslationException.InvalidKey()
                ProviderFactory.gemini(key, settings.model).verify()
                if (newKey != null) settings.apiKey.save(newKey)
                hasKey = true
                editingKey = false
                keyInput = ""
                keyStatus = KeyStatus.OK
                // Siguiente paso natural del alta: pedir permisos sin que el usuario tenga que buscarlos.
                if (settings.detectionEnabled && !settings.permissionsPrompted && !Permissions.canDetect(context)) {
                    requestPermissions()
                }
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

            Section(stringResource(R.string.setup_key_title)) {
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
            }

            Section(stringResource(R.string.setup_lang_title)) {
                LanguagePicker(language) {
                    language = it
                    settings.targetLanguage = it
                }
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
                    // Leer permissionsTick hace que este bloque se recalcule al cambiar los permisos.
                    val access = permissionsTick.let { Permissions.mediaAccess(context) }
                    val notifications = permissionsTick.let { Permissions.notificationsAllowed(context) }
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
                TextButton(onClick = { openUrl(context, "https://ai.google.dev/gemini-api/terms") }) {
                    Text(stringResource(R.string.setup_privacy_terms))
                }
            }

            TextButton(onClick = { showAdvanced = !showAdvanced }) { Text(stringResource(R.string.setup_advanced)) }
            if (showAdvanced) {
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it; settings.model = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.setup_model)) },
                    supportingText = { Text(stringResource(R.string.setup_model_desc, GeminiProvider.DEFAULT_MODEL)) },
                )
                if (BuildConfig.DEBUG) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.setup_debug_fake), Modifier.weight(1f))
                        Switch(checked = fake, onCheckedChange = { fake = it; settings.useFakeProvider = it })
                    }
                }
            }
        }
    }
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

private fun openUrl(context: android.content.Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
}

private fun openAppSettings(context: android.content.Context) {
    context.startActivity(
        Intent(SystemSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
    )
}
