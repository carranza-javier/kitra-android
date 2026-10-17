package com.kettenki.kitra.ui.translate

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kettenki.kitra.R
import com.kettenki.kitra.TempFiles
import com.kettenki.kitra.core.BasicModeReason
import com.kettenki.kitra.core.Engine
import com.kettenki.kitra.core.NoticeKind
import com.kettenki.kitra.core.ProgressStep
import com.kettenki.kitra.detect.ScreenshotNotifier
import com.kettenki.kitra.ui.KitraTheme
import com.kettenki.kitra.ui.setup.SetupActivity
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Vista ligera de resultado: hoja inferior sobre la app anterior. La traducción empieza sola (SPEC §7).
 * Entradas: aviso de captura (ACTION_TRANSLATE), compartir imágenes (ACTION_SEND) y, en la variante
 * «play», el aviso que abre el selector de fotos del sistema (ACTION_PICK_AND_TRANSLATE).
 */
class TranslateActivity : ComponentActivity() {
    private val viewModel: TranslateViewModel by viewModels()

    /** Selector de fotos del sistema: no necesita permiso de fotos (variante «play»). */
    private val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.start(uri) else finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handle(intent)
        setContent {
            KitraTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                ResultSheet(
                    state = state,
                    onCopy = ::copy,
                    onClose = ::close,
                    onRetry = viewModel::retry,
                    onSetUpKey = ::openKeyTutorial,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent) {
        ScreenshotNotifier.dismiss(this)
        when (intent.action) {
            ACTION_PICK_AND_TRANSLATE -> {
                viewModel.awaitPick()
                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            ACTION_TRANSLATE -> intent.data?.let(viewModel::start) ?: finish()
            Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                ?.let(viewModel::start) ?: finish()
            else -> finish()
        }
    }

    private fun copy(text: String) {
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), text))
    }

    /** Lleva directamente al tutorial de la clave de Gemini. */
    private fun openKeyTutorial() {
        startActivity(SetupActivity.tutorialIntent(this))
        close()
    }

    /** Cierra, cancela el trabajo si sigue en curso y elimina los temporales. */
    private fun close() {
        viewModel.cancel()
        TempFiles.clear(this)
        finishAndRemoveTask()
    }

    override fun onDestroy() {
        if (isFinishing) {
            viewModel.cancel()
            TempFiles.clear(this)
        }
        super.onDestroy()
    }

    companion object {
        const val ACTION_TRANSLATE = "com.kettenki.kitra.action.TRANSLATE"
        const val ACTION_PICK_AND_TRANSLATE = "com.kettenki.kitra.action.PICK_AND_TRANSLATE"

        fun intentFor(context: Context, imageUri: Uri): Intent =
            Intent(context, TranslateActivity::class.java)
                .setAction(ACTION_TRANSLATE)
                .setData(imageUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        fun pickIntent(context: Context): Intent =
            Intent(context, TranslateActivity::class.java)
                .setAction(ACTION_PICK_AND_TRANSLATE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

@Composable
private fun ResultSheet(
    state: TranslateUiState,
    onCopy: (String) -> Unit,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onSetUpKey: () -> Unit,
) {
    if (state.awaitingPick) return
    // Tocar fuera de la hoja cierra, igual que el botón Cerrar o el gesto Atrás.
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.32f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose)
            .statusBarsPadding(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            tonalElevation = 2.dp,
        ) {
            Column(
                Modifier
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
            ) {
                // Aviso visible del modo básico, antes del texto: el motor local es claramente secundario.
                state.basicReason?.let { BasicModeBanner(it, onSetUpKey) }
                Column(
                    Modifier
                        .weight(1f, fill = false)
                        .heightIn(min = 96.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    state.notice?.let { Message(noticeText(it, state.targetLanguageCode)) }
                    when {
                        state.error != null -> {
                            if (state.text.isNotEmpty()) TranslatedText(state.text)
                            Message(stringResource(state.error), isError = true)
                        }
                        state.text.isEmpty() -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(progressText(state.progress), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        else -> TranslatedText(state.text)
                    }
                }
                // Atribución obligatoria de la traducción local de ML Kit, junto al resultado.
                if (state.engine == Engine.ML_KIT && state.text.isNotEmpty()) {
                    Image(
                        painter = painterResource(R.drawable.gt_attribution),
                        contentDescription = stringResource(R.string.attribution_google_translate),
                        modifier = Modifier.padding(top = 12.dp).height(16.dp),
                    )
                }
                Spacer(Modifier.padding(top = 12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state.inProgress && state.text.isNotEmpty()) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.weight(1f))
                    }
                    when {
                        state.error != null -> OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                        else -> CopyButton(enabled = state.text.isNotEmpty(), onCopy = { onCopy(state.text) })
                    }
                    Button(onClick = onClose) { Text(stringResource(R.string.close)) }
                }
            }
        }
    }
}

/**
 * «Traducción básica: puede ser imprecisa…». Sin clave o con la clave rota, invita a configurarla con un
 * botón al tutorial. Sin conexión, sin cuota o con Gemini caído, explica el motivo sin pedir nada.
 */
@Composable
private fun BasicModeBanner(reason: BasicModeReason, onSetUpKey: () -> Unit) {
    val (message, action) = when (reason) {
        BasicModeReason.NO_KEY -> R.string.basic_no_key to R.string.basic_action_set_up
        BasicModeReason.KEY_INVALID -> R.string.basic_key_invalid to R.string.basic_action_fix_key
        BasicModeReason.OFFLINE -> R.string.basic_offline to null
        BasicModeReason.QUOTA_EXCEEDED -> R.string.basic_quota to null
        BasicModeReason.CLOUD_FAILED -> R.string.basic_cloud_failed to null
    }
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.basic_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(stringResource(message), style = MaterialTheme.typography.bodyMedium)
            if (action != null) {
                FilledTonalButton(onClick = onSetUpKey) { Text(stringResource(action)) }
            }
        }
    }
}

@Composable
private fun progressText(step: ProgressStep?): String = when (step) {
    ProgressStep.PreparingTextRecognition -> stringResource(R.string.progress_preparing_ocr)
    ProgressStep.RecognizingText -> stringResource(R.string.progress_recognizing)
    is ProgressStep.DownloadingLanguage -> stringResource(R.string.progress_downloading_language, languageName(step.languageCode))
    ProgressStep.Translating, null -> stringResource(R.string.translating)
}

@Composable
private fun noticeText(kind: NoticeKind, targetCode: String): String = when (kind) {
    NoticeKind.ALREADY_IN_TARGET -> stringResource(R.string.notice_already_in_target, languageName(targetCode))
}

/** Nombre de un idioma en el idioma de la interfaz ("alemán", "German", "Deutsch"). */
internal fun languageName(code: String): String =
    Locale.forLanguageTag(code).getDisplayLanguage(Locale.getDefault())

@Composable
private fun CopyButton(enabled: Boolean, onCopy: () -> Unit) {
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }
    OutlinedButton(enabled = enabled, onClick = { onCopy(); copied = true }) {
        Text(stringResource(if (copied) R.string.copied else R.string.copy))
    }
}

@Composable
private fun TranslatedText(text: String) {
    SelectionContainer {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun Message(text: String, isError: Boolean = false) {
    Text(
        text,
        modifier = Modifier.padding(top = 8.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
