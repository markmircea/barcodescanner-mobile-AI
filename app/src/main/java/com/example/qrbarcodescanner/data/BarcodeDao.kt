package com.example.qrbarcodescanner.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BarcodeDao {
    @Query("SELECT * FROM barcodes ORDER BY timestamp DESC")
    fun getAllBarcodes(): Flow<List<BarcodeEntity>>

    @Query("SELECT * FROM barcodes WHERE id = :id")
    suspend fun getBarcodeById(id: Int): BarcodeEntity?

    @Query("SELECT * FROM barcodes WHERE content = :content LIMIT 1")
    suspend fun getBarcodeByContent(content: String): BarcodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBarcode(barcode: BarcodeEntity)

    @Update
    suspend fun updateBarcode(barcode: BarcodeEntity)

    @Delete
    suspend fun deleteBarcode(barcode: BarcodeEntity)

    @Query("DELETE FROM barcodes")
    suspend fun deleteAllBarcodes()

    @Query("SELECT * FROM barcodes WHERE content LIKE '%' || :query || '%' OR type LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR productInfo LIKE '%' || :query || '%'")
    fun searchBarcodes(query: String): Flow<List<BarcodeEntity>>
}