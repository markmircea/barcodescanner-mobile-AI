package com.example.qrbarcodescanner

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrbarcodescanner.data.AppDatabase
import com.example.qrbarcodescanner.data.BarcodeEntity
import com.example.qrbarcodescanner.data.BarcodeRepository
import com.example.qrbarcodescanner.data.SettingsRepository
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val barcodeRepository: BarcodeRepository
    private val settingsRepository: SettingsRepository
    val allBarcodes: StateFlow<List<BarcodeEntity>>

    private val _showMenu = MutableStateFlow(false)
    val showMenu: StateFlow<Boolean> = _showMenu.asStateFlow()

    private val _scannedBarcode = MutableStateFlow<Triple<String, String, String>?>(null)
    val scannedBarcode: StateFlow<Triple<String, String, String>?> = _scannedBarcode.asStateFlow()

    private val _barcodeDescription = MutableStateFlow<String?>(null)
    val barcodeDescription: StateFlow<String?> = _barcodeDescription.asStateFlow()

    private val _isFlashOn = MutableStateFlow(false)
    val isFlashOn: StateFlow<Boolean> = _isFlashOn.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(false)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    val zoomLevel = MutableStateFlow(0f)

    // Settings
    val copyToClipboard: StateFlow<Boolean>
    val retrieveUrlInfo: StateFlow<Boolean>
    val autoFocus: StateFlow<Boolean>
    val touchFocus: StateFlow<Boolean>
    val keepDuplicates: StateFlow<Boolean>
    val useInAppBrowser: StateFlow<Boolean>
    val addScansToHistory: StateFlow<Boolean>

    private val productLookupService = ProductLookupService()

    init {
        val barcodeDao = AppDatabase.getDatabase(application).barcodeDao()
        barcodeRepository = BarcodeRepository(barcodeDao)
        settingsRepository = SettingsRepository(application)
        allBarcodes = barcodeRepository.allBarcodes.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        copyToClipboard = settingsRepository.copyToClipboard.stateIn(viewModelScope, SharingStarted.Lazily, false)
        retrieveUrlInfo = settingsRepository.retrieveUrlInfo.stateIn(viewModelScope, SharingStarted.Lazily, false)
        autoFocus = settingsRepository.autoFocus.stateIn(viewModelScope, SharingStarted.Lazily, true)
        touchFocus = settingsRepository.touchFocus.stateIn(viewModelScope, SharingStarted.Lazily, false)
        keepDuplicates = settingsRepository.keepDuplicates.stateIn(viewModelScope, SharingStarted.Lazily, false)
        useInAppBrowser = settingsRepository.useInAppBrowser.stateIn(viewModelScope, SharingStarted.Lazily, true)
        addScansToHistory = settingsRepository.addScansToHistory.stateIn(viewModelScope, SharingStarted.Lazily, true)

        Log.d("MainViewModel", "ViewModel initialized")
    }

    fun toggleMenu() {
        _showMenu.value = !_showMenu.value
        Log.d("MainViewModel", "Menu toggled: ${_showMenu.value}")
    }

    fun toggleFlash() {
        _isFlashOn.value = !_isFlashOn.value
        Log.d("MainViewModel", "Flash toggled: ${_isFlashOn.value}")
    }

    fun toggleCamera() {
        _isFrontCamera.value = !_isFrontCamera.value
        Log.d("MainViewModel", "Camera toggled: ${if (_isFrontCamera.value) "Front" else "Back"}")
    }

    fun updateZoomLevel(level: Float) {
        zoomLevel.value = level
        Log.d("MainViewModel", "Zoom level updated: $level")
    }

    fun onBarcodeDetected(barcode: String, barcodeType: String, productInfo: String) {
        Log.d("MainViewModel", "Barcode detected: $barcode, Type: $barcodeType, Product: $productInfo")
        _scannedBarcode.value = Triple(barcode, barcodeType, productInfo)
        _barcodeDescription.value = null // Clear previous description

        viewModelScope.launch {
            try {
                Log.d("MainViewModel", "Fetching description for barcode: $barcode")
                val description = OpenAIService.getDescriptionForBarcode(barcode)
                Log.d("MainViewModel", "Description fetched: $description")
                _barcodeDescription.value = description
                if (addScansToHistory.value) {
                    Log.d("MainViewModel", "Saving barcode to history")
                    saveToHistory(barcode, barcodeType, description, productInfo)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching description", e)
                _barcodeDescription.value = "Error fetching description: ${e.message}"
            }
        }
    }

    private suspend fun saveToHistory(barcode: String, barcodeType: String, description: String, productInfo: String) {
        val barcodeEntity = BarcodeEntity(
            content = barcode,
            type = barcodeType,
            description = description,
            productInfo = productInfo,
            timestamp = System.currentTimeMillis()
        )
        
        if (keepDuplicates.value) {
            barcodeRepository.insertBarcode(barcodeEntity)
            Log.d("MainViewModel", "Barcode saved to history: $barcode, Type: $barcodeType")
        } else {
            val existingBarcode = barcodeRepository.getBarcodeByContent(barcode)
            if (existingBarcode == null) {
                barcodeRepository.insertBarcode(barcodeEntity)
                Log.d("MainViewModel", "New barcode saved to history: $barcode, Type: $barcodeType")
            } else {
                barcodeRepository.updateBarcode(barcodeEntity.copy(id = existingBarcode.id))
                Log.d("MainViewModel", "Existing barcode updated in history: $barcode, Type: $barcodeType")
            }
        }
    }

    fun deleteBarcode(barcode: BarcodeEntity) {
        viewModelScope.launch {
            barcodeRepository.deleteBarcode(barcode)
            Log.d("MainViewModel", "Barcode deleted: ${barcode.content}")
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            barcodeRepository.deleteAllBarcodes()
            Log.d("MainViewModel", "History cleared")
        }
    }

    fun searchBarcodes(query: String) = barcodeRepository.searchBarcodes(query)

    // Settings update functions
    fun updateCopyToClipboard(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateCopyToClipboard(value)
            Log.d("MainViewModel", "Copy to clipboard setting updated: $value")
        }
    }

    fun updateRetrieveUrlInfo(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateRetrieveUrlInfo(value)
            Log.d("MainViewModel", "Retrieve URL info setting updated: $value")
        }
    }

    fun updateAutoFocus(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAutoFocus(value)
            Log.d("MainViewModel", "Auto focus setting updated: $value")
        }
    }

    fun updateTouchFocus(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateTouchFocus(value)
            Log.d("MainViewModel", "Touch focus setting updated: $value")
        }
    }

    fun updateKeepDuplicates(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateKeepDuplicates(value)
            Log.d("MainViewModel", "Keep duplicates setting updated: $value")
        }
    }

    fun updateUseInAppBrowser(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateUseInAppBrowser(value)
            Log.d("MainViewModel", "Use in-app browser setting updated: $value")
        }
    }

    fun updateAddScansToHistory(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAddScansToHistory(value)
            Log.d("MainViewModel", "Add scans to history setting updated: $value")
        }
    }

    fun scanImageFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                Log.d("MainViewModel", "Scanning image from URI: $uri")
                val image = InputImage.fromFilePath(getApplication(), uri)
                val scanner = BarcodeScanning.getClient()

                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isNotEmpty()) {
                            val barcode = barcodes[0]
                            Log.d("MainViewModel", "Barcode found in image: ${barcode.rawValue}")
                            barcode.rawValue?.let { barcodeValue ->
                                val barcodeType = getBarcodeTypeString(barcode.format)
                                viewModelScope.launch {
                                    val productInfo = productLookupService.lookupProduct(barcodeValue, barcodeType)
                                    onBarcodeDetected(barcodeValue, barcodeType, productInfo)
                                }
                            }
                        } else {
                            Log.d("MainViewModel", "No barcode found in image")
                            _barcodeDescription.value = "No barcode found in the image"
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("MainViewModel", "Error scanning image", e)
                        _barcodeDescription.value = "Error scanning image: ${e.message}"
                    }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error processing image", e)
                _barcodeDescription.value = "Error processing image: ${e.message}"
            }
        }
    }

    private fun getBarcodeTypeString(format: Int): String {
        return when (format) {
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UNKNOWN -> "Unknown"
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_ALL_FORMATS -> "All Formats"
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODE_128 -> "Code 128"
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODE_39 -> "Code 39"
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODE_93 -> "Code 93"
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODABAR -> "Codabar"
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_13 -> "EAN-13"
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_8 -> "EAN-8"
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_ITF -> "ITF"
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE -> "QR Code"
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_A -> "UPC-A"
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_E -> "UPC-E"
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_PDF417 -> "PDF417"
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_AZTEC -> "Aztec"
            else -> "Unknown"
        }
    }
}