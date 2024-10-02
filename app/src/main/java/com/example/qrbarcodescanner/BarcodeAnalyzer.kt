package com.example.qrbarcodescanner

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BarcodeAnalyzer(
    private val coroutineScope: CoroutineScope,
    private val onBarcodeDetected: (String, String, String, Boolean) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_ITF,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_PDF417,
                Barcode.FORMAT_AZTEC
            )
            .build()
    )

    private val productLookupService = ProductLookupService()
    private var lastProcessingTimestamp = 0L
    private val processingInterval = 1000L // 1 second interval

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        if (currentTimestamp - lastProcessingTimestamp < processingInterval) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            
            Log.d("BarcodeAnalyzer", "Analyzing image...")
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    Log.d("BarcodeAnalyzer", "Barcodes detected: ${barcodes.size}")
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { barcodeValue ->
                            val barcodeType = getBarcodeTypeString(barcode.format)
                            Log.d("BarcodeAnalyzer", "Barcode detected: $barcodeValue, Type: $barcodeType")
                            coroutineScope.launch {
                                val productInfo = productLookupService.lookupProduct(barcodeValue, barcodeType)
                                val hasProductInfo = productInfo.isNotBlank() && productInfo != "Product not found"
                                val aiInput = if (hasProductInfo) productInfo else barcodeValue
                                onBarcodeDetected(barcodeValue, barcodeType, aiInput, hasProductInfo)
                            }
                        }
                    }
                    lastProcessingTimestamp = currentTimestamp
                }
                .addOnFailureListener { e ->
                    Log.e("BarcodeAnalyzer", "Error processing image", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            Log.w("BarcodeAnalyzer", "Unable to get media image, skipping analysis")
            imageProxy.close()
        }
    }

    private fun getBarcodeTypeString(format: Int): String {
        return when (format) {
            Barcode.FORMAT_UNKNOWN -> "Unknown"
            Barcode.FORMAT_CODE_128 -> "Code 128"
            Barcode.FORMAT_CODE_39 -> "Code 39"
            Barcode.FORMAT_CODE_93 -> "Code 93"
            Barcode.FORMAT_CODABAR -> "Codabar"
            Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
            Barcode.FORMAT_EAN_13 -> "EAN-13"
            Barcode.FORMAT_EAN_8 -> "EAN-8"
            Barcode.FORMAT_ITF -> "ITF"
            Barcode.FORMAT_QR_CODE -> "QR Code"
            Barcode.FORMAT_UPC_A -> "UPC-A"
            Barcode.FORMAT_UPC_E -> "UPC-E"
            Barcode.FORMAT_PDF417 -> "PDF417"
            Barcode.FORMAT_AZTEC -> "Aztec"
            else -> "Unknown"
        }
    }
}