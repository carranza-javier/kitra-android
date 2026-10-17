package com.kettenki.kitra.ui.setup

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kettenki.kitra.R
import com.kettenki.kitra.settings.Settings
import kotlinx.coroutines.launch

private const val AI_STUDIO_KEYS_URL = "https://aistudio.google.com/apikey"

private sealed interface TutorialStatus {
    data object Idle : TutorialStatus
    data object Checking : TutorialStatus
    data class Done(val noQuota: Boolean) : TutorialStatus
    data class Failed(val message: Int) : TutorialStatus
}

/**
 * Tutorial de la clave en tres pasos. Al volver de AI Studio, Kitra mira el portapapeles y, si hay algo
 * con forma de clave, ofrece usarlo con un toque; la valida con Google y confirma con un mensaje claro.
 */
@Composable
fun KeyTutorialScreen(settings: Settings, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val windowInfo = LocalWindowInfo.current
    var clipboardKey by remember { mutableStateOf<String?>(null) }
    var manualKey by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<TutorialStatus>(TutorialStatus.Idle) }

    // El portapapeles solo se puede leer con la ventana enfocada: se mira cada vez que el usuario vuelve.
    LaunchedEffect(windowInfo) {
        snapshotFlow { windowInfo.isWindowFocused }.collect { focused ->
            if (focused && status !is TutorialStatus.Done) {
                val text = readClipboard(context)
                clipboardKey = text?.trim()?.takeIf { KeyValidation.looksLikeKey(it) && it != settings.apiKey.load() }
            }
        }
    }

    fun check(key: String) {
        status = TutorialStatus.Checking
        scope.launch {
            status = when (val result = KeyValidation.checkAndSave(settings, key)) {
                KeyCheck.Valid, KeyCheck.ValidNoQuota -> {
                    // La clave ya está guardada cifrada: no se deja en el portapapeles.
                    clearClipboardIfContains(context, key.trim())
                    clipboardKey = null
                    manualKey = ""
                    TutorialStatus.Done(noQuota = result == KeyCheck.ValidNoQuota)
                }
                is KeyCheck.Invalid -> TutorialStatus.Failed(result.message)
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
            Text(stringResource(R.string.tutorial_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.tutorial_subtitle), style = MaterialTheme.typography.bodyLarge)

            val done = status as? TutorialStatus.Done
            if (done != null) {
                SuccessCard(noQuota = done.noQuota, onContinue = onDone)
            }

            StepCard(1, stringResource(R.string.tutorial_step1_title), stringResource(R.string.tutorial_step1_text), illustration = { BrowserIllustration() }) {
                Button(onClick = { openUrl(context, AI_STUDIO_KEYS_URL) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.tutorial_step1_button))
                }
            }
            StepCard(2, stringResource(R.string.tutorial_step2_title), stringResource(R.string.tutorial_step2_text), illustration = { CreateKeyIllustration() })
            StepCard(3, stringResource(R.string.tutorial_step3_title), stringResource(R.string.tutorial_step3_text), illustration = { DetectedIllustration() }) {
                val candidate = clipboardKey
                if (done == null && candidate != null) {
                    Text(stringResource(R.string.tutorial_detected, KeyValidation.mask(candidate)), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Button(
                        onClick = { check(candidate) },
                        enabled = status != TutorialStatus.Checking,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) { Text(stringResource(R.string.tutorial_use_key)) }
                }
                if (done == null) {
                    OutlinedTextField(
                        value = manualKey,
                        onValueChange = { manualKey = it.trim() },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.tutorial_manual_hint)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    )
                    OutlinedButton(
                        onClick = { check(manualKey) },
                        enabled = manualKey.isNotBlank() && status != TutorialStatus.Checking,
                    ) { Text(stringResource(R.string.tutorial_manual_button)) }
                }
                when (val s = status) {
                    TutorialStatus.Checking -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.tutorial_checking))
                    }
                    is TutorialStatus.Failed -> Text(stringResource(s.message), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    else -> Unit
                }
            }

            // Aviso de privacidad del nivel gratuito, visible en el propio tutorial.
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.tutorial_privacy_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.tutorial_privacy_body), style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (done == null) {
                TextButton(onClick = onDone, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.tutorial_later))
                }
            }
        }
    }
}

@Composable
private fun SuccessCard(noQuota: Boolean, onContinue: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.tutorial_success_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(stringResource(if (noQuota) R.string.tutorial_success_no_quota else R.string.tutorial_success_body), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onContinue) { Text(stringResource(R.string.tutorial_continue)) }
        }
    }
}

@Composable
private fun StepCard(
    number: Int,
    title: String,
    text: String,
    illustration: @Composable () -> Unit,
    actions: @Composable () -> Unit = {},
) {
    ElevatedCard(colors = CardDefaults.elevatedCardColors()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                    Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) { Text("$number", fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            illustration()
            Text(text, style = MaterialTheme.typography.bodyMedium)
            actions()
        }
    }
}

// Ilustraciones esquemáticas de AI Studio (no son capturas: la web puede cambiar de aspecto).

@Composable
private fun IllustrationFrame(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
private fun FakePill(text: String, filled: Boolean) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (filled) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
        border = if (filled) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Text(text, Modifier.padding(horizontal = 14.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun BrowserIllustration() = IllustrationFrame {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface) {
        Text("aistudio.google.com/apikey", Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium)
    }
    Text("Google AI Studio", style = MaterialTheme.typography.titleSmall)
    FakePill(stringResource(R.string.tutorial_illustration_sign_in), filled = true)
}

@Composable
private fun CreateKeyIllustration() = IllustrationFrame {
    Text("API keys", style = MaterialTheme.typography.titleSmall)
    FakePill("Create API key", filled = true)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.weight(1f)) {
            Text("••••••••••••••••", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.width(8.dp))
        FakePill(stringResource(R.string.tutorial_illustration_copy), filled = false)
    }
}

@Composable
private fun DetectedIllustration() = IllustrationFrame {
    Text("Kitra", style = MaterialTheme.typography.titleSmall)
    Text(stringResource(R.string.tutorial_illustration_detected), style = MaterialTheme.typography.bodyMedium)
    FakePill(stringResource(R.string.tutorial_use_key), filled = true)
}

private fun readClipboard(context: Context): String? = runCatching {
    val clip = context.getSystemService(ClipboardManager::class.java).primaryClip ?: return null
    if (clip.itemCount == 0) null else clip.getItemAt(0).coerceToText(context)?.toString()
}.getOrNull()

private fun clearClipboardIfContains(context: Context, key: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    if (readClipboard(context)?.trim() == key) runCatching { clipboard.clearPrimaryClip() }
}
