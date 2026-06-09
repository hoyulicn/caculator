package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AppSettings)

    @Query("SELECT * FROM calc_history ORDER BY timestamp DESC LIMIT 50")
    fun getHistoryFlow(): Flow<List<CalcHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addHistory(history: CalcHistory)

    @Query("DELETE FROM calc_history")
    suspend fun clearHistory()

    @Query("SELECT * FROM offline_currency_rate ORDER BY currencyCode ASC")
    fun getCurrencyRatesFlow(): Flow<List<OfflineCurrencyRate>>

    @Query("SELECT * FROM offline_currency_rate ORDER BY currencyCode ASC")
    suspend fun getCurrencyRates(): List<OfflineCurrencyRate>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCurrencyRates(rates: List<OfflineCurrencyRate>)

    @Update
    suspend fun updateCurrencyRate(rate: OfflineCurrencyRate)
}
