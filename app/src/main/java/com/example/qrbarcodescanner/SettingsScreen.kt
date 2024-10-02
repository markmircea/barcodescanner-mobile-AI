package com.example.qrbarcodescanner

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: MainViewModel,
    onNavigateToPrivacyPolicy: () -> Unit
) {
    val copyToClipboard by viewModel.copyToClipboard.collectAsState(initial = false)
    val retrieveUrlInfo by viewModel.retrieveUrlInfo.collectAsState(initial = false)
    val autoFocus by viewModel.autoFocus.collectAsState(initial = true)
    val touchFocus by viewModel.touchFocus.collectAsState(initial = false)
    val keepDuplicates by viewModel.keepDuplicates.collectAsState(initial = false)
    val useInAppBrowser by viewModel.useInAppBrowser.collectAsState(initial = true)
    val addScansToHistory by viewModel.addScansToHistory.collectAsState(initial = true)

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            SettingsSwitch(
                title = "Copy to Clipboard",
                isChecked = copyToClipboard,
                onCheckedChange = { viewModel.updateCopyToClipboard(it) }
            )
            SettingsSwitch(
                title = "Retrieve URL Info",
                isChecked = retrieveUrlInfo,
                onCheckedChange = { viewModel.updateRetrieveUrlInfo(it) }
            )
            SettingsSwitch(
                title = "Auto Focus",
                isChecked = autoFocus,
                onCheckedChange = { viewModel.updateAutoFocus(it) }
            )
            SettingsSwitch(
                title = "Touch Focus",
                isChecked = touchFocus,
                onCheckedChange = { viewModel.updateTouchFocus(it) },
                enabled = autoFocus
            )
            SettingsSwitch(
                title = "Keep Duplicates",
                isChecked = keepDuplicates,
                onCheckedChange = { viewModel.updateKeepDuplicates(it) }
            )
            SettingsSwitch(
                title = "Use In-App Browser",
                isChecked = useInAppBrowser,
                onCheckedChange = { viewModel.updateUseInAppBrowser(it) }
            )
            SettingsSwitch(
                title = "Add Scans to History",
                isChecked = addScansToHistory,
                onCheckedChange = { viewModel.updateAddScansToHistory(it) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { /* TODO: Implement camera selection */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select Camera")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onNavigateToPrivacyPolicy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Privacy Policy")
            }
        }
    }
}

@Composable
fun SettingsSwitch(
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title)
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}