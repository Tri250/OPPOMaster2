package com.silas.omaster.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.silas.omaster.R
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.ui.components.OMasterTopAppBar

@Composable
fun ApiConfigScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }

    var aiEndpoint by remember { mutableStateOf(settingsManager.aiApiEndpoint) }
    var presetEndpoint by remember { mutableStateOf(settingsManager.presetApiEndpoint) }
    var authEndpoint by remember { mutableStateOf(settingsManager.authApiEndpoint) }
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.api_config_reset)) },
            text = { Text(stringResource(R.string.api_config_reset_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    settingsManager.resetApiEndpoints()
                    aiEndpoint = settingsManager.aiApiEndpoint
                    presetEndpoint = settingsManager.presetApiEndpoint
                    authEndpoint = settingsManager.authApiEndpoint
                    showResetDialog = false
                    Toast.makeText(context, context.getString(R.string.api_config_saved), Toast.LENGTH_SHORT).show()
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            textContentColor = MaterialTheme.colorScheme.onBackground
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        OMasterTopAppBar(
            title = stringResource(R.string.api_config_title),
            onBack = onBack,
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = aiEndpoint,
                onValueChange = { aiEndpoint = it },
                label = { Text(stringResource(R.string.api_endpoint_ai)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = presetEndpoint,
                onValueChange = { presetEndpoint = it },
                label = { Text(stringResource(R.string.api_endpoint_preset)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = authEndpoint,
                onValueChange = { authEndpoint = it },
                label = { Text(stringResource(R.string.api_endpoint_auth)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                singleLine = true
            )

            Button(
                onClick = {
                    settingsManager.setCustomApiEndpoints(
                        aiEndpoint = aiEndpoint,
                        presetEndpoint = presetEndpoint,
                        authEndpoint = authEndpoint
                    )
                    Toast.makeText(context, context.getString(R.string.api_config_saved), Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.api_config_reset))
            }
        }
    }
}
