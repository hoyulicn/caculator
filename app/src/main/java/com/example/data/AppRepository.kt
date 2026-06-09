package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class AppRepository(private val dao: AppDao) {

    val settings: Flow<AppSettings> = dao.getSettingsFlow().map { it ?: AppSettings() }
    val history: Flow<List<CalcHistory>> = dao.getHistoryFlow()
    val currencyRates: Flow<List<OfflineCurrencyRate>> = dao.getCurrencyRatesFlow()

    suspend fun getSettings(): AppSettings {
        return dao.getSettings() ?: AppSettings()
    }

    suspend fun saveSettings(settings: AppSettings) {
        dao.saveSettings(settings)
    }

    suspend fun addHistory(expression: String, result: String) {
        dao.addHistory(CalcHistory(expression = expression, result = result))
    }

    suspend fun clearHistory() {
        dao.clearHistory()
    }

    suspend fun updateCurrencyRate(rate: OfflineCurrencyRate) {
        dao.updateCurrencyRate(rate)
    }

    suspend fun saveAllCurrencyRates(rates: List<OfflineCurrencyRate>) {
        dao.saveCurrencyRates(rates)
    }

    suspend fun initializeDefaults() {
        // Initialize default app settings if not existent
        if (dao.getSettings() == null) {
            dao.saveSettings(AppSettings())
        }

        // Initialize currency rates if empty
        val existingRates = dao.getCurrencyRates()
        if (existingRates.isEmpty()) {
            val defaults = listOf(
                OfflineCurrencyRate("USD", 1.0, "美元", "US Dollar", "$"),
                OfflineCurrencyRate("CNY", 0.138, "人民币", "Renminbi", "¥"),
                OfflineCurrencyRate("EUR", 1.08, "欧元", "Euro", "€"),
                OfflineCurrencyRate("JPY", 0.0064, "日元", "Japanese Yen", "¥"),
                OfflineCurrencyRate("GBP", 1.27, "英镑", "British Pound", "£"),
                OfflineCurrencyRate("CAD", 0.73, "加元", "Canadian Dollar", "$"),
                OfflineCurrencyRate("AUD", 0.66, "澳元", "Australian Dollar", "$"),
                OfflineCurrencyRate("HKD", 0.128, "港币", "Hong Kong Dollar", "$")
            )
            dao.saveCurrencyRates(defaults)
        }
    }
}
