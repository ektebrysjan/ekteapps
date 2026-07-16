package com.ektebrysjan.notes.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ektebrysjan.notes.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: NotesViewModel) {
    val hasPin by viewModel.hasPin.collectAsStateWithLifecycle()
    val unlocked by viewModel.privateUnlocked.collectAsStateWithLifecycle()

    var showSetPin by remember { mutableStateOf(false) }
    var showChangeVerify by remember { mutableStateOf(false) }
    var showRemoveVerify by remember { mutableStateOf(false) }
    var showUnlockForExport by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> uri?.let { viewModel.export(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.import(it) } }

    val exportName = stringResource(R.string.export_filename)
    fun launchExport() = exportLauncher.launch(exportName)

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_settings)) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // --- Security ---
            SettingsSection(title = stringResource(R.string.settings_security)) {
                Text(
                    text = stringResource(if (hasPin) R.string.pin_set else R.string.pin_not_set),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!hasPin) {
                    ActionButton(stringResource(R.string.set_pin)) { showSetPin = true }
                } else {
                    Text(
                        text = stringResource(
                            if (unlocked) R.string.unlocked_status else R.string.locked_status
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    ActionButton(
                        text = stringResource(R.string.change_pin),
                        icon = { Icon(Icons.Default.Lock, null) }
                    ) { showChangeVerify = true }
                    if (unlocked) {
                        ActionButton(
                            text = stringResource(R.string.lock_now),
                            icon = { Icon(Icons.Default.Lock, null) }
                        ) { viewModel.lock() }
                    }
                    ActionButton(
                        text = stringResource(R.string.remove_pin),
                        icon = { Icon(Icons.Default.LockOpen, null) }
                    ) { showRemoveVerify = true }
                }
            }

            // --- Backup / export ---
            SettingsSection(title = stringResource(R.string.settings_backup)) {
                Text(
                    text = stringResource(R.string.export_caveat),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ActionButton(
                    text = stringResource(R.string.export_notes),
                    icon = { Icon(Icons.Default.FileDownload, null) }
                ) {
                    if (viewModel.canExport()) launchExport() else showUnlockForExport = true
                }
                ActionButton(
                    text = stringResource(R.string.import_notes),
                    icon = { Icon(Icons.Default.FileUpload, null) }
                ) {
                    // application/zip plus a wildcard so pickers that report a generic type still work.
                    importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                }
            }

            // --- About ---
            SettingsSection(title = stringResource(R.string.settings_about)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.author_credit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.about_version, "1.0"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                val uriHandler = LocalUriHandler.current
                val githubUrl = stringResource(R.string.github_url)
                Text(
                    text = stringResource(R.string.github_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable { uriHandler.openUri(githubUrl) }
                )
            }
        }
    }

    if (showSetPin) {
        SetPinDialog(
            onConfirm = { viewModel.setPin(it); showSetPin = false },
            onDismiss = { showSetPin = false }
        )
    }
    if (showChangeVerify) {
        EnterPinDialog(
            title = stringResource(R.string.change_pin),
            onVerify = {
                val ok = viewModel.unlock(it)
                if (ok) { showChangeVerify = false; showSetPin = true } // verified → pick new PIN
                ok
            },
            onDismiss = { showChangeVerify = false }
        )
    }
    if (showRemoveVerify) {
        EnterPinDialog(
            title = stringResource(R.string.remove_pin),
            onVerify = { viewModel.removePin(it) },
            onDismiss = { showRemoveVerify = false }
        )
    }
    if (showUnlockForExport) {
        EnterPinDialog(
            title = stringResource(R.string.export_needs_unlock),
            onVerify = {
                val ok = viewModel.unlock(it)
                if (ok) { showUnlockForExport = false; launchExport() }
                ok
            },
            onDismiss = { showUnlockForExport = false }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) { content() }
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        if (icon != null) {
            icon()
            Text(text = text, modifier = Modifier.padding(start = 8.dp))
        } else {
            Text(text = text)
        }
    }
}
