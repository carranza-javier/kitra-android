package app.kitra.ui.translate

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.IntentCompat
import app.kitra.R
import app.kitra.TempFiles
import app.kitra.core.Engine
import app.kitra.core.NoticeKind
import app.kitra.core.ProgressStep
import app.kitra.detect.ScreenshotNotifier
import app.kitra.ui.KitraTheme
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Vista ligera de resultado: hoja inferior sobre la app anterior. La traducción empieza sola (SPEC §7).
 * Entradas: aviso de captura (ACTION_TRANSLATE) y compartir imágenes (ACTION_SEND).
 */
class TranslateActivity : ComponentActivity() {
    private val viewModel: TranslateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handle(intent)
        setContent {
            KitraTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                ResultSheet(
                    state = state,
                    onCopy = ::copy,
                    onClose = ::close,
                    onRetry = viewModel::retry,
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
        val uri = imageUri(intent) ?: run { finish(); return }
        ScreenshotNotifier.dismiss(this)
        viewModel.start(uri)
    }

    private fun imageUri(intent: Intent): Uri? = when (intent.action) {
        ACTION_TRANSLATE -> intent.data
        Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
        else -> null
    }

    private fun copy(text: String) {
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), text))
    }

    /** Cierra, cancela la petición si sigue en curso y elimina los temporales. */
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
        const val ACTION_TRANSLATE = "app.kitra.action.TRANSLATE"

        fun intentFor(context: Context, imageUri: Uri): Intent =
            Intent(context, TranslateActivity::class.java)
                .setAction(ACTION_TRANSLATE)
                .setData(imageUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

@Composable
private fun ResultSheet(
    state: TranslateUiState,
    onCopy: (String) -> Unit,
    onClose: () -> Unit,
    onRetry: () -> Unit,
) {
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
    NoticeKind.CLOUD_FELL_BACK_TO_DEVICE -> stringResource(R.string.notice_cloud_fell_back)
}

/** Nombre del idioma en español ("alemán"), porque la interfaz de Kitra está en español. */
internal fun languageName(code: String): String =
    Locale.forLanguageTag(code).getDisplayLanguage(Locale.forLanguageTag("es"))

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
