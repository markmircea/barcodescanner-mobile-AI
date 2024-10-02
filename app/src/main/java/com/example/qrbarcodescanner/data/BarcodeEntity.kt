package com.example.qrbarcodescanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "barcodes")
data class BarcodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val content: String,
    val type: String,
    val description: String,
    val productInfo: String,
    val timestamp: Long
)