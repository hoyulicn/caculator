package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val isDarkMode: Boolean = true, // default dark mode since users love it!
    val language: String = "zh", // "zh" for Chinese, "en" for English
    val shortcutsJson: String = "[\"calc_sci\",\"currency\",\"unit\",\"date\",\"finance\"]", // order & selection
    val webDavUrl: String = "",
    val webDavUser: String = "",
    val webDavPass: String = "",
    val webDavEncryptKey: String = "",
    val webDavFolder: String = "SmartCalcBackup",
    val lastBackupTime: Long = 0L
)

@Entity(tableName = "calc_history")
data class CalcHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "offline_currency_rate")
data class OfflineCurrencyRate(
    @PrimaryKey val currencyCode: String, // e.g., USD, CNY, EUR, JPY
    val rateToUSD: Double, // Conversion base
    val displayNameZh: String,
    val displayNameEn: String,
    val symbol: String
)
