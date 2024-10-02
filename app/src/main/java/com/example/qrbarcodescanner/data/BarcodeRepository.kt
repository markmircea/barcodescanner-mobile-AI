package com.example.qrbarcodescanner.data

import kotlinx.coroutines.flow.Flow

class BarcodeRepository(private val barcodeDao: BarcodeDao) {

    val allBarcodes: Flow<List<BarcodeEntity>> = barcodeDao.getAllBarcodes()

    suspend fun insertBarcode(barcode: BarcodeEntity) {
        barcodeDao.insertBarcode(barcode)
    }

    suspend fun deleteBarcode(barcode: BarcodeEntity) {
        barcodeDao.deleteBarcode(barcode)
    }

    suspend fun deleteAllBarcodes() {
        barcodeDao.deleteAllBarcodes()
    }

    fun searchBarcodes(query: String): Flow<List<BarcodeEntity>> {
        return barcodeDao.searchBarcodes(query)
    }

    suspend fun getBarcodeById(id: Int): BarcodeEntity? {
        return barcodeDao.getBarcodeById(id)
    }

    suspend fun getBarcodeByContent(content: String): BarcodeEntity? {
        return barcodeDao.getBarcodeByContent(content)
    }

    suspend fun updateBarcode(barcode: BarcodeEntity) {
        barcodeDao.updateBarcode(barcode)
    }
}