package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.dao()
    val repository = AppRepository(dao)

    // DB States
    val settingsState = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    val historyState = repository.history.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val currencyRatesState = repository.currencyRates.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Scientific Calculator State
    var calcExpression = MutableStateFlow("")
    var calcResult = MutableStateFlow("")

    // Currency Converter State
    var currencyAmount = MutableStateFlow("100")
    var currencyFromSelected = MutableStateFlow("USD")
    var currencyToSelected = MutableStateFlow("CNY")
    var currencyConvertedResult = MutableStateFlow("0.00")

    // Unit Converter State
    var unitCategory = MutableStateFlow("length") // length, weight, area, volume
    var unitInputValue = MutableStateFlow("1")
    var unitFromSelected = MutableStateFlow("unit_m")
    var unitToSelected = MutableStateFlow("unit_cm")
    var unitResultValue = MutableStateFlow("100.0")

    // Date Calculator State
    var dateDiffStart = MutableStateFlow(getFormattedToday())
    var dateDiffEnd = MutableStateFlow(getFormattedToday())
    var dateDiffResult = MutableStateFlow("")

    var dateOffsetSource = MutableStateFlow(getFormattedToday())
    var dateOffsetCount = MutableStateFlow("30")
    var dateOffsetIsAdd = MutableStateFlow(true)
    var dateOffsetResult = MutableStateFlow("")

    // Financial Calculator State
    var loanPrincipal = MutableStateFlow("1000000") // 1 Million CNY
    var loanRate = MutableStateFlow("4.5") // 4.5% interest
    var loanYears = MutableStateFlow("30") // 30 Years
    var loanIsEqualPI = MutableStateFlow(true) // true for Amortized, false for Equal Principal
    var loanMonthlyPaymentDesc = MutableStateFlow("")
    var loanTotalInterest = MutableStateFlow("")
    var loanTotalPayment = MutableStateFlow("")

    var compoundPrincipal = MutableStateFlow("50000")
    var compoundRate = MutableStateFlow("5.0")
    var compoundYears = MutableStateFlow("10")
    var compoundFreq = MutableStateFlow("annual") // annual, quarterly, monthly
    var compoundResultBalance = MutableStateFlow("")
    var compoundResultInterest = MutableStateFlow("")

    // Sync state
    var syncInProgress = MutableStateFlow(false)
    var syncStatusMessage = MutableStateFlow<String?>(null)

    // Active View / Sidebar selection
    var currentView = MutableStateFlow("calc_sci") // default view
    
    // Customize shortcut toolbar state
    var isEditingShortcuts = MutableStateFlow(false)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.initializeDefaults()
            // Auto calculate initial states
            calculateCurrency()
            calculateUnits()
            calculateDateDifference()
            calculateDateOffset()
            calculateLoanPayment()
            calculateCompoundInterest()
        }
    }

    // Toggle app language dynamically
    fun toggleLanguage() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = settingsState.value
            val newLang = if (current.language == "zh") "en" else "zh"
            repository.saveSettings(current.copy(language = newLang))
        }
    }

    // Toggle Dark Theme dynamically
    fun toggleTheme(enableDark: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = settingsState.value
            repository.saveSettings(current.copy(isDarkMode = enableDark))
        }
    }

    // Save customized shortcut bar selections
    fun saveShortcuts(keys: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = settingsState.value
            val jsonArray = JSONArray(keys)
            repository.saveSettings(current.copy(shortcutsJson = jsonArray.toString()))
        }
    }

    // Parse Pinned Shortcuts from JSON
    fun getPinnedShortcuts(): List<String> {
        return try {
            val js = settingsState.value.shortcutsJson
            val arr = JSONArray(js)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            list
        } catch (e: Exception) {
            listOf("calc_sci", "currency", "unit", "date", "finance")
        }
    }

    // ----------------- Scientific Calculator -----------------
    fun appendToExpression(text: String) {
        if (text == "C") {
            calcExpression.value = ""
            calcResult.value = ""
        } else if (text == "⌫" || text == "DEL") {
            val exp = calcExpression.value
            if (exp.isNotEmpty()) {
                calcExpression.value = exp.substring(0, exp.length - 1)
            }
        } else if (text == "=") {
            evaluateScientific()
        } else {
            calcExpression.value += text
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearHistory()
        }
    }

    fun runFormula(expr: String) {
        calcExpression.value = expr
        evaluateScientific()
    }

    private fun evaluateScientific() {
        val expr = calcExpression.value
        if (expr.isEmpty()) return
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val value = evaluateExpression(expr)
                val rounded = if (Math.abs(value - Math.round(value)) < 1e-9) {
                    Math.round(value).toString()
                } else {
                    String.format(Locale.US, "%.6f", value).trimEnd('0').trimEnd('.')
                }
                calcResult.value = rounded
                // save history
                repository.addHistory(expr, rounded)
            } catch (e: Exception) {
                calcResult.value = "Error"
            }
        }
    }

    // Elegant parsing of mathematical equations completely offline
    private fun evaluateExpression(expr: String): Double {
        return ScientificEvaluator(expr).parse()
    }


    // ----------------- Currency Converter -----------------
    fun updateCurrencyAmount(amt: String) {
        currencyAmount.value = amt
        calculateCurrency()
    }

    fun updateCurrencySelection(from: String, to: String) {
        currencyFromSelected.value = from
        currencyToSelected.value = to
        calculateCurrency()
    }

    fun modifyCurrencyRate(code: String, rateToUSD: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val rates = currencyRatesState.value
            val target = rates.find { it.currencyCode == code }
            if (target != null) {
                repository.updateCurrencyRate(target.copy(rateToUSD = rateToUSD))
                calculateCurrency()
            }
        }
    }

    fun calculateCurrency() {
        val amount = currencyAmount.value.toDoubleOrNull() ?: 0.0
        val rates = currencyRatesState.value
        if (rates.isEmpty()) return

        val fromRate = rates.find { it.currencyCode == currencyFromSelected.value }?.rateToUSD ?: 1.0
        val toRate = rates.find { it.currencyCode == currencyToSelected.value }?.rateToUSD ?: 1.0

        // convert to base USD then to destination currency
        val parsedUSD = amount * fromRate
        val finalRate = parsedUSD / toRate
        currencyConvertedResult.value = String.format(Locale.US, "%.2f", finalRate)
    }


    // ----------------- Unit Converter -----------------
    fun updateUnitCategory(category: String) {
        unitCategory.value = category
        // Setup initial selections based on category to prevent mismatched units
        when (category) {
            "length" -> {
                unitFromSelected.value = "unit_m"
                unitToSelected.value = "unit_cm"
            }
            "weight" -> {
                unitFromSelected.value = "unit_kg"
                unitToSelected.value = "unit_g"
            }
            "area" -> {
                unitFromSelected.value = "unit_m2"
                unitToSelected.value = "unit_km2"
            }
            "volume" -> {
                unitFromSelected.value = "unit_l"
                unitToSelected.value = "unit_ml"
            }
        }
        calculateUnits()
    }

    fun updateUnitValues(valStr: String, from: String, to: String) {
        unitInputValue.value = valStr
        unitFromSelected.value = from
        unitToSelected.value = to
        calculateUnits()
    }

    fun calculateUnits() {
        val amount = unitInputValue.value.toDoubleOrNull() ?: 0.0
        val from = unitFromSelected.value
        val to = unitToSelected.value
        val cat = unitCategory.value

        // Conversion multipliers (base scale relative to Base unit)
        val fromMultiplier = getUnitRatio(from, cat)
        val toMultiplier = getUnitRatio(to, cat)

        // Convert value to base base, then scale to selection
        val inBaseValue = amount * fromMultiplier
        val result = inBaseValue / toMultiplier
        
        unitResultValue.value = if (result == 0.0) "0.0" else if (Math.abs(result - Math.round(result)) < 1e-7) {
            Math.round(result).toString()
        } else {
            String.format(Locale.US, "%.5f", result).trimEnd('0').trimEnd('.')
        }
    }

    private fun getUnitRatio(unitKey: String, category: String): Double {
        return when (category) {
            "length" -> when (unitKey) {
                "unit_m" -> 1.0
                "unit_cm" -> 0.01
                "unit_mm" -> 0.001
                "unit_km" -> 1000.0
                "unit_inch" -> 0.0254
                "unit_ft" -> 0.3048
                else -> 1.0
            }
            "weight" -> when (unitKey) {
                "unit_g" -> 1.0
                "unit_kg" -> 1000.0
                "unit_oz" -> 28.3495
                "unit_lb" -> 453.592
                else -> 1.0
            }
            "area" -> when (unitKey) {
                "unit_m2" -> 1.0
                "unit_km2" -> 1000000.0
                "unit_hectare" -> 10000.0
                "unit_acre" -> 4046.86
                else -> 1.0
            }
            "volume" -> when (unitKey) {
                "unit_l" -> 1.0
                "unit_ml" -> 0.001
                "unit_m3" -> 1000.0
                "unit_gal" -> 3.78541
                else -> 1.0
            }
            else -> 1.0
        }
    }


    // ----------------- Date Calculator -----------------
    private fun getFormattedToday(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun calculateDateDifference() {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val d1 = sdf.parse(dateDiffStart.value) ?: Date()
            val d2 = sdf.parse(dateDiffEnd.value) ?: Date()

            val diffMs = Math.abs(d2.time - d1.time)
            val diffDays = Math.round(diffMs.toDouble() / (1000 * 60 * 60 * 24))
            dateDiffResult.value = diffDays.toString()
        } catch (e: Exception) {
            dateDiffResult.value = "Format Error (YYYY-MM-DD)"
        }
    }

    fun calculateDateOffset() {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val source = sdf.parse(dateOffsetSource.value) ?: Date()
            val days = dateOffsetCount.value.toIntOrNull() ?: 0

            val cal = Calendar.getInstance()
            cal.time = source
            val multiplier = if (dateOffsetIsAdd.value) 1 else -1
            cal.add(Calendar.DAY_OF_YEAR, days * multiplier)

            dateOffsetResult.value = sdf.format(cal.time)
        } catch (e: Exception) {
            dateOffsetResult.value = "Format Error"
        }
    }


    // ----------------- Financial Calculator -----------------
    fun calculateLoanPayment() {
        val p = loanPrincipal.value.toDoubleOrNull() ?: 0.0
        val r = (loanRate.value.toDoubleOrNull() ?: 0.0) / 100.0
        val years = loanYears.value.toIntOrNull() ?: 0
        if (p <= 0.0 || r <= 0.0 || years <= 0) {
            loanMonthlyPaymentDesc.value = "--"
            loanTotalInterest.value = "--"
            loanTotalPayment.value = "--"
            return
        }

        val months = years * 12
        val monthlyRate = r / 12.0

        if (loanIsEqualPI.value) {
            // Amortized Equal Principal & Interest: M = P * [ i * (1+i)^n ] / [ (1+i)^n - 1 ]
            val powered = Math.pow(1 + monthlyRate, months.toDouble())
            val m = p * (monthlyRate * powered) / (powered - 1)
            val totalPay = m * months
            val totalInt = totalPay - p

            loanMonthlyPaymentDesc.value = String.format(Locale.US, "%.2f", m)
            loanTotalInterest.value = String.format(Locale.US, "%.2f", totalInt)
            loanTotalPayment.value = String.format(Locale.US, "%.2f", totalPay)
        } else {
            // Equal Principal: Pay = (P / n) + RemainingPrincipal * monthlyRate
            val basePrincipal = p / months
            val firstMonthPay = basePrincipal + p * monthlyRate
            val lastMonthPay = basePrincipal + (p - (months - 1) * basePrincipal) * monthlyRate
            val decay = basePrincipal * monthlyRate

            // Sum of all payments: n * P_base + P * i * (n + 1) / 2
            val totalPay = months * basePrincipal + (p * monthlyRate * (months + 1)) / 2.0
            val totalInt = totalPay - p

            val formattedFirst = String.format(Locale.US, "%.2f", firstMonthPay)
            val formattedLast = String.format(Locale.US, "%.2f", lastMonthPay)
            val formattedDecay = String.format(Locale.US, "%.2f", decay)

            loanMonthlyPaymentDesc.value = "$formattedFirst ~ $formattedLast"
            loanTotalInterest.value = String.format(Locale.US, "%.2f", totalInt)
            loanTotalPayment.value = String.format(Locale.US, "%.2f", totalPay)
        }
    }

    fun calculateCompoundInterest() {
        val p = compoundPrincipal.value.toDoubleOrNull() ?: 0.0
        val r = (compoundRate.value.toDoubleOrNull() ?: 0.0) / 100.0
        val years = compoundYears.value.toIntOrNull() ?: 0
        if (p <= 0.0 || r <= 0.0 || years <= 0) {
            compoundResultBalance.value = "--"
            compoundResultInterest.value = "--"
            return
        }

        val freqMultiplier = when (compoundFreq.value) {
            "quarterly" -> 4
            "monthly" -> 12
            else -> 1 // annual
        }

        // A = P * (1 + r/n)^(n*t)
        val n = freqMultiplier.toDouble()
        val exponent = freqMultiplier * years
        val balance = p * Math.pow(1 + r / n, exponent.toDouble())
        val interest = balance - p

        compoundResultBalance.value = String.format(Locale.US, "%.2f", balance)
        compoundResultInterest.value = String.format(Locale.US, "%.2f", interest)
    }


    // ----------------- WebDAV Synchronization -----------------
    fun updateWebDavSettings(
        url: String, user: String, pass: String, folder: String, key: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = settingsState.value
            val next = current.copy(
                webDavUrl = url,
                webDavUser = user,
                webDavPass = pass,
                webDavFolder = folder,
                webDavEncryptKey = key
            )
            repository.saveSettings(next)
        }
    }

    fun testWebDavConnection() {
        val s = settingsState.value
        if (s.webDavUrl.isEmpty() || s.webDavUser.isEmpty() || s.webDavPass.isEmpty()) {
            syncStatusMessage.value = "ERROR: Missing WebDAV credentials"
            return
        }
        syncInProgress.value = true
        syncStatusMessage.value = null
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = WebDavSync(s.webDavUrl, s.webDavUser, s.webDavPass, s.webDavFolder, s.webDavEncryptKey)
                val success = client.testConnection()
                syncStatusMessage.value = if (success) "SUCCESS_CONN" else "FAIL_CONN"
            } catch (e: Exception) {
                syncStatusMessage.value = "FAIL_CONN_ERR: ${e.localizedMessage ?: "Unknown network error"}"
            } finally {
                syncInProgress.value = false
            }
        }
    }

    fun backupToWebDav() {
        val s = settingsState.value
        if (s.webDavUrl.isEmpty() || s.webDavUser.isEmpty() || s.webDavPass.isEmpty()) {
            syncStatusMessage.value = "ERROR: Setup WebDAV settings first"
            return
        }
        syncInProgress.value = true
        syncStatusMessage.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Get fresh data values from Flows
                val history = historyState.value
                val rates = currencyRatesState.value
                val payload = WebDavSync.convertLocalDataToJson(s, history, rates)

                val client = WebDavSync(s.webDavUrl, s.webDavUser, s.webDavPass, s.webDavFolder, s.webDavEncryptKey)
                client.uploadBackup(payload)

                val updatedSettings = s.copy(lastBackupTime = System.currentTimeMillis())
                repository.saveSettings(updatedSettings)

                syncStatusMessage.value = "SUCCESS_BACKUP"
            } catch (e: Exception) {
                syncStatusMessage.value = "FAIL_BACKUP: ${e.localizedMessage ?: "Network error"}"
            } finally {
                syncInProgress.value = false
            }
        }
    }

    fun restoreFromWebDav() {
        val s = settingsState.value
        if (s.webDavUrl.isEmpty() || s.webDavUser.isEmpty() || s.webDavPass.isEmpty()) {
            syncStatusMessage.value = "ERROR: Setup WebDAV settings first"
            return
        }
        syncInProgress.value = true
        syncStatusMessage.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = WebDavSync(s.webDavUrl, s.webDavUser, s.webDavPass, s.webDavFolder, s.webDavEncryptKey)
                val rawPayload = client.downloadBackup()

                val (restoredSettings, restoredHistory, restoredRates) = WebDavSync.parseRestoreJson(rawPayload)
                
                // Save back into DB
                // Keep the credentials from current settings to keep it logged in, but merge visual preferences
                val mergedSettings = restoredSettings.copy(
                    id = 1,
                    webDavUrl = s.webDavUrl,
                    webDavUser = s.webDavUser,
                    webDavPass = s.webDavPass,
                    webDavFolder = s.webDavFolder,
                    webDavEncryptKey = s.webDavEncryptKey,
                    lastBackupTime = System.currentTimeMillis()
                )
                repository.saveSettings(mergedSettings)
                
                // For history and currencies, update database
                repository.clearHistory()
                for (h in restoredHistory) {
                    dao.addHistory(h)
                }
                if (restoredRates.isNotEmpty()) {
                    repository.saveAllCurrencyRates(restoredRates)
                }

                syncStatusMessage.value = "SUCCESS_RESTORE"
                // trigger re-evaluations
                calculateCurrency()
            } catch (e: Exception) {
                syncStatusMessage.value = "FAIL_RESTORE: ${e.localizedMessage ?: "Restore failure"}"
            } finally {
                syncInProgress.value = false
            }
        }
    }


    // ----------------- Local Data Management -----------------
    fun wipeLocalDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearHistory()
            val defaultSettings = AppSettings(id = 1)
            dao.saveSettings(defaultSettings)
            // Restore defaults for currency
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
            
            syncStatusMessage.value = "WIPED_CLEAN"
            // Re-eval values
            calculateCurrency()
            calculateUnits()
        }
    }

    fun exportLocalDataAsJson(): String {
        return WebDavSync.convertLocalDataToJson(
            settingsState.value,
            historyState.value,
            currencyRatesState.value
        )
    }

    fun importLocalDataFromJson(json: String): Boolean {
        return try {
            val (restoredSettings, restoredHistory, restoredRates) = WebDavSync.parseRestoreJson(json)
            viewModelScope.launch(Dispatchers.IO) {
                repository.saveSettings(restoredSettings.copy(id = 1))
                repository.clearHistory()
                for (h in restoredHistory) {
                    dao.addHistory(h)
                }
                if (restoredRates.isNotEmpty()) {
                    repository.saveAllCurrencyRates(restoredRates)
                }
                calculateCurrency()
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}

class ScientificEvaluator(private val sText: String) {
    private val s = sText.replace("π", Math.PI.toString())
        .replace("e", Math.E.toString())
        .replace("×", "*")
        .replace("÷", "/")
        .trim()

    private var pos = -1
    private var ch = -1

    private fun nextChar() {
        ch = if (++pos < s.length) s[pos].code else -1
    }

    private fun eat(charToEat: Int): Boolean {
        while (ch == ' '.code) nextChar()
        if (ch == charToEat) {
            nextChar()
            return true
        }
        return false
    }

    fun parse(): Double {
        nextChar()
        val x = parseExpression()
        if (pos < s.length) throw RuntimeException("Unexpected: " + s[pos].toChar())
        return x
    }

    private fun parseExpression(): Double {
        var x = parseTerm()
        while (true) {
            if (eat('+'.code)) x += parseTerm()
            else if (eat('-'.code)) x -= parseTerm()
            else break
        }
        return x
    }

    private fun parseTerm(): Double {
        var x = parseFactor()
        while (true) {
            if (eat('*'.code)) x *= parseFactor()
            else if (eat('/'.code)) {
                val denom = parseFactor()
                if (denom == 0.0) throw RuntimeException("Division by zero")
                x /= denom
            } else break
        }
        return x
    }

    private fun parseFactor(): Double {
        if (eat('+'.code)) return parseFactor()
        if (eat('-'.code)) return -parseFactor()

        var x: Double
        val startPos = pos
        if (eat('('.code)) {
            x = parseExpression()
            eat(')'.code)
        } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
            while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
            x = s.substring(startPos, pos).toDouble()
        } else if (ch >= 'a'.code && ch <= 'z'.code || ch == '√'.code) {
            while (ch >= 'a'.code && ch <= 'z'.code || ch == '√'.code) nextChar()
            val func = s.substring(startPos, pos)
            x = parseFactor()
            x = when (func) {
                "sin" -> Math.sin(Math.toRadians(x))
                "cos" -> Math.cos(Math.toRadians(x))
                "tan" -> Math.tan(Math.toRadians(x))
                "log" -> Math.log10(x)
                "ln" -> Math.log(x)
                "√", "sqrt" -> Math.sqrt(x)
                else -> throw RuntimeException("Unknown function: $func")
            }
        } else {
            throw RuntimeException("Unexpected character: " + ch.toChar())
        }

        if (eat('^'.code)) x = Math.pow(x, parseFactor())

        return x
    }
}
