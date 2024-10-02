package com.example.qrbarcodescanner

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import kotlinx.coroutines.launch

@Composable
fun CameraPreview(
    onBarcodeDetected: (String, String) -> Unit,
    isFrontCamera: Boolean,
    zoomLevel: Float
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { ContextCompat.getMainExecutor(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var camera by remember { mutableStateOf<Camera?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    this.scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { barcode, barcodeType ->
                                Log.d("CameraPreview", "Barcode detected: $barcode, Type: $barcodeType")
                                onBarcodeDetected(barcode, barcodeType)
                            })
                        }

                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                        Log.d("CameraPreview", "Camera bound to lifecycle")
                    } catch (exc: Exception) {
                        Log.e("CameraPreview", "Use case binding failed", exc)
                    }
                }, executor)

                previewView
            }
        )
    }

    LaunchedEffect(zoomLevel) {
        coroutineScope.launch {
            try {
                val maxZoomRatio = camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 1f
                val minZoomRatio = camera?.cameraInfo?.zoomState?.value?.minZoomRatio ?: 1f
                val zoomRatio = minZoomRatio + (maxZoomRatio - minZoomRatio) * zoomLevel
                camera?.cameraControl?.setZoomRatio(zoomRatio)
                Log.d("CameraPreview", "Zoom level set to: $zoomLevel, Zoom ratio: $zoomRatio")
            } catch (e: Exception) {
                Log.e("CameraPreview", "Error setting zoom ratio", e)
            }
        }
    }
}
