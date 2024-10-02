package com.example.qrbarcodescanner

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ProductLookupService {
    suspend fun lookupProduct(barcode: String, barcodeType: String): String {
        return withContext(Dispatchers.IO) {
            when {
                isUPCBarcode(barcodeType) -> lookupUPCProduct(barcode)
                else -> "Not a UPC barcode. Content: $barcode"
            }
        }
    }

    private fun isUPCBarcode(barcodeType: String): Boolean {
        return barcodeType == "UPC-A" || barcodeType == "UPC-E" || barcodeType == "EAN-13" || barcodeType == "EAN-8"
    }

    private suspend fun lookupUPCProduct(barcode: String): String {
        return withContext(Dispatchers.IO) {
            val url = URL("https://api.upcitemdb.com/prod/trial/lookup?upc=$barcode")
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)
                    val items = jsonObject.getJSONArray("items")
                    if (items.length() > 0) {
                        val item = items.getJSONObject(0)
                        val title = item.getString("title")
                        val brand = item.optString("brand", "Unknown Brand")
                        "$title by $brand"
                    } else {
                        "Product not found in database"
                    }
                } else {
                    "Error looking up product: HTTP $responseCode"
                }
            } catch (e: Exception) {
                "Error looking up product: ${e.message}"
            } finally {
                connection.disconnect()
            }
        }
    }
}