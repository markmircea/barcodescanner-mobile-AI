package com.example.qrbarcodescanner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.qrbarcodescanner.ui.theme.QRBarcodeScannerTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import android.Manifest

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel
    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.scanImageFromUri(it) }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            viewModel = viewModel()
            QRBarcodeScannerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
                    
                    LaunchedEffect(key1 = true) {
                        cameraPermissionState.launchPermissionRequest()
                    }
                    
                    QRBarcodeScannerApp(viewModel, ::openGallery)
                }
            }
        }
    }

    private fun openGallery() {
        getContent.launch("image/*")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRBarcodeScannerApp(viewModel: MainViewModel, openGallery: () -> Unit) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(
                viewModel = viewModel,
                onNavigateToHistory = { navController.navigate("history") },
                onNavigateToMyQR = { navController.navigate("myqr") },
                onNavigateToSettings = { navController.navigate("settings") },
                openGallery = openGallery
            )
        }
        composable("history") {
            val barcodes by viewModel.allBarcodes.collectAsState(initial = emptyList())
            HistoryScreen(
                barcodes = barcodes,
                onBackClick = { navController.popBackStack() },
                onDeleteBarcode = { barcode -> viewModel.deleteBarcode(barcode) },
                onClearHistory = { viewModel.clearHistory() }
            )
        }
        composable("myqr") {
            MyQRScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = viewModel,
                onNavigateToPrivacyPolicy = { navController.navigate("privacy_policy") }
            )
        }
        composable("privacy_policy") {
            PrivacyPolicyScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToMyQR: () -> Unit,
    onNavigateToSettings: () -> Unit,
    openGallery: () -> Unit
) {
    val showMenu by viewModel.showMenu.collectAsState()
    val scannedBarcode by viewModel.scannedBarcode.collectAsState()
    val barcodeDescription by viewModel.barcodeDescription.collectAsState()
    val isFlashOn by viewModel.isFlashOn.collectAsState()
    val isFrontCamera by viewModel.isFrontCamera.collectAsState()
    val zoomLevel by viewModel.zoomLevel.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(scannedBarcode) {
        Log.d("MainScreen", "Scanned barcode updated: $scannedBarcode")
    }

    LaunchedEffect(barcodeDescription) {
        Log.d("MainScreen", "Barcode description updated: $barcodeDescription")
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("QR & Barcode Scanner") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.toggleMenu() }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = openGallery) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery")
                    }
                    IconButton(onClick = { viewModel.toggleFlash() }) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flash"
                        )
                    }
                    IconButton(onClick = { viewModel.toggleCamera() }) {
                        Icon(
                            imageVector = if (isFrontCamera) Icons.Default.CameraRear else Icons.Default.CameraFront,
                            contentDescription = "Switch Camera"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            CameraPreview(
                onBarcodeDetected = { barcode, barcodeType, aiInput, hasProductInfo ->
                    Log.d("MainScreen", "Barcode detected: $barcode, Type: $barcodeType, AI Input: $aiInput, Has Product Info: $hasProductInfo")
                    viewModel.onBarcodeDetected(barcode, barcodeType, aiInput, hasProductInfo)
                },
                isFrontCamera = isFrontCamera,
                zoomLevel = zoomLevel
            )
            
            Slider(
                value = zoomLevel,
                onValueChange = { viewModel.updateZoomLevel(it) },
                valueRange = 0f..1f,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(0.8f)
            )

            scannedBarcode?.let { (content, type, aiInput, hasProductInfo) ->
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(16.dp)
                ) {
                    if (content.startsWith("http://") || content.startsWith("https://")) {
                        ClickableText(
                            text = AnnotatedString(
                                text = "Scanned: $content",
                                spanStyle = SpanStyle(
                                    color = Color.White,
                                    textDecoration = TextDecoration.Underline
                                )
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(content))
                                startActivity(context, intent, null)
                            }
                        )
                    } else {
                        Text(
                            text = "Scanned: $content",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Type: $type",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (hasProductInfo) "Product Info: $aiInput" else "Barcode: $aiInput",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Description: ${barcodeDescription ?: "Loading..."}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val searchQuery = Uri.encode(content)
                            val searchUrl = "https://www.google.com/search?q=$searchQuery"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
                            startActivity(context, intent, null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Search Online")
                    }
                }
            }
        }
    }

    if (showMenu) {
        ExpandableMenu(
            onDismiss = { viewModel.toggleMenu() },
            onMenuItemClick = { menuItem ->
                viewModel.toggleMenu()
                when (menuItem) {
                    "Scan" -> { /* Already on scan screen */ }
                    "Scan Image" -> openGallery()
                    "History" -> onNavigateToHistory()
                    "My QR" -> onNavigateToMyQR()
                    "Settings" -> onNavigateToSettings()
                }
            }
        )
    }
}

@Composable
fun ExpandableMenu(
    onDismiss: () -> Unit,
    onMenuItemClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(250.dp)
            .padding(16.dp)
    ) {
        Text("Menu", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        MenuButton("Scan", Icons.Default.QrCodeScanner, onMenuItemClick)
        MenuButton("Scan Image", Icons.Default.PhotoLibrary, onMenuItemClick)
        MenuButton("History", Icons.Default.History, onMenuItemClick)
        MenuButton("My QR", Icons.Default.QrCode2, onMenuItemClick)
        MenuButton("Settings", Icons.Default.Settings, onMenuItemClick)
    }
}

@Composable
fun MenuButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: (String) -> Unit) {
    Button(
        onClick = { onClick(text) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}