package com.kettenki.kitra.ui.setup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings as SystemSettings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.kettenki.kitra.R
import com.kettenki.kitra.core.Language
import com.kettenki.kitra.engine.PackState

@Composable
fun Section(title: String, highlighted: Boolean = false, content: @Composable () -> Unit) {
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
fun PermissionHint(text: String, onOpenSettings: () -> Unit) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
    OutlinedButton(onClick = onOpenSettings) { Text(stringResource(R.string.open_settings)) }
}

@Composable
fun LanguagePicker(selected: Language, onSelect: (Language) -> Unit) {
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

/** Estado del paquete de idioma del modo básico. */
@Composable
fun PackStatus(state: PackState?, onDownloadNow: () -> Unit) {
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

fun openUrl(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
}

fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(SystemSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
    )
}
