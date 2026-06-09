package com.example.data

import android.util.Base64
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"

    private fun deriveKeyAndIv(password: String): Pair<SecretKeySpec, IvParameterSpec> {
        // Derive key and IV safely from user's custom password using SHA-256
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
        
        // 16 bytes for AES key, 16 bytes for IV
        val keyBytes = ByteArray(16)
        val ivBytes = ByteArray(16)
        System.arraycopy(hash, 0, keyBytes, 0, 16)
        System.arraycopy(hash, 16, ivBytes, 0, 16)
        
        val keySpec = SecretKeySpec(keyBytes, "AES")
        val ivSpec = IvParameterSpec(ivBytes)
        return Pair(keySpec, ivSpec)
    }

    fun encrypt(plainText: String, password: String): String {
        if (password.isEmpty()) return plainText // raw state if no key set
        val (keySpec, ivSpec) = deriveKeyAndIv(password)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    fun decrypt(cipherText: String, password: String): String {
        if (password.isEmpty()) return cipherText
        val (keySpec, ivSpec) = deriveKeyAndIv(password)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        val decoded = Base64.decode(cipherText, Base64.NO_WRAP)
        val decrypted = cipher.doFinal(decoded)
        return String(decrypted, Charsets.UTF_8)
    }
}

class WebDavSync(
    private val url: String,
    private val user: String,
    private val pass: String,
    private val folder: String,
    private val encryptKey: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val authHeader = Credentials.basic(user, pass)

    private fun cleanUrl(baseUrl: String, suffix: String): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val relative = if (suffix.startsWith("/")) suffix.substring(1) else suffix
        return base + relative
    }

    @Throws(IOException::class)
    fun testConnection(): Boolean {
        val request = Request.Builder()
            .url(cleanUrl(url, ""))
            .addHeader("Authorization", authHeader)
            .method("PROPFIND", null)
            .build()

        client.newCall(request).execute().use { response ->
            return response.isSuccessful || response.code == 207 || response.code == 405
        }
    }

    @Throws(IOException::class)
    private fun createFolder() {
        val folderUrl = cleanUrl(url, folder)
        val request = Request.Builder()
            .url(folderUrl)
            .addHeader("Authorization", authHeader)
            .method("MKCOL", null)
            .build()

        client.newCall(request).execute().use { response ->
            // 201 Created or 405 Method Not Allowed (meaning folder already exists)
            if (!response.isSuccessful && response.code != 405 && response.code != 207) {
                throw IOException("Failed to create folder on WebDAV: ${response.code} ${response.message}")
            }
        }
    }

    @Throws(IOException::class)
    fun uploadBackup(backupJsonStr: String): String {
        // Step 1: Create WebDAV folder if not exists
        try {
            createFolder()
        } catch (e: Exception) {
            // Proceed anyway as it may already exists
        }

        // Step 2: Encrypt
        val finalPayload = if (encryptKey.isNotEmpty()) {
            CryptoUtils.encrypt(backupJsonStr, encryptKey)
        } else {
            backupJsonStr
        }

        // Step 3: Put file
        val fileUrl = cleanUrl(url, "$folder/backup_data.enc")
        val requestBody = finalPayload.toRequestBody("text/plain".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(fileUrl)
            .addHeader("Authorization", authHeader)
            .put(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Upload failed with status code ${response.code}")
            }
            return "SUCCESS"
        }
    }

    @Throws(IOException::class)
    fun downloadBackup(): String {
        val fileUrl = cleanUrl(url, "$folder/backup_data.enc")
        val request = Request.Builder()
            .url(fileUrl)
            .addHeader("Authorization", authHeader)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                if (response.code == 404) {
                    throw IOException("Backup file not found on WebDAV server.")
                }
                throw IOException("Download failed with status code ${response.code}")
            }
            val bodyString = response.body?.string() ?: throw IOException("Empty response body")
            
            // Decrypt
            return try {
                if (encryptKey.isNotEmpty()) {
                    CryptoUtils.decrypt(bodyString, encryptKey)
                } else {
                    bodyString
                }
            } catch (e: Exception) {
                throw IOException("Decryption failed. Please verify your Encryption Key.")
            }
        }
    }

    companion object {
        fun convertLocalDataToJson(
            settings: AppSettings,
            history: List<CalcHistory>,
            rates: List<OfflineCurrencyRate>
        ): String {
            val root = JSONObject()
            
            // Settings
            val settingsObj = JSONObject().apply {
                put("isDarkMode", settings.isDarkMode)
                put("language", settings.language)
                put("shortcutsJson", settings.shortcutsJson)
                put("webDavUrl", settings.webDavUrl)
                put("webDavUser", settings.webDavUser)
                put("webDavPass", settings.webDavPass)
                put("webDavFolder", settings.webDavFolder)
                put("webDavEncryptKey", settings.webDavEncryptKey)
            }
            root.put("settings", settingsObj)

            // History
            val historyArray = JSONArray()
            for (h in history) {
                val item = JSONObject().apply {
                    put("expression", h.expression)
                    put("result", h.result)
                    put("timestamp", h.timestamp)
                }
                historyArray.put(item)
            }
            root.put("history", historyArray)

            // Rates
            val ratesArray = JSONArray()
            for (r in rates) {
                val item = JSONObject().apply {
                    put("currencyCode", r.currencyCode)
                    put("rateToUSD", r.rateToUSD)
                    put("displayNameZh", r.displayNameZh)
                    put("displayNameEn", r.displayNameEn)
                    put("symbol", r.symbol)
                }
                ratesArray.put(item)
            }
            root.put("rates", ratesArray)

            return root.toString()
        }

        fun parseRestoreJson(
            jsonString: String
        ): Triple<AppSettings, List<CalcHistory>, List<OfflineCurrencyRate>> {
            val root = JSONObject(jsonString)

            // Settings
            val sObj = root.optJSONObject("settings") ?: JSONObject()
            val settings = AppSettings(
                isDarkMode = sObj.optBoolean("isDarkMode", true),
                language = sObj.optString("language", "zh"),
                shortcutsJson = sObj.optString("shortcutsJson", "[\"calc_sci\",\"currency\",\"unit\",\"date\",\"finance\"]"),
                webDavUrl = sObj.optString("webDavUrl", ""),
                webDavUser = sObj.optString("webDavUser", ""),
                webDavPass = sObj.optString("webDavPass", ""),
                webDavFolder = sObj.optString("webDavFolder", "SmartCalcBackup"),
                webDavEncryptKey = sObj.optString("webDavEncryptKey", ""),
                lastBackupTime = System.currentTimeMillis()
            )

            // History
            val hArr = root.optJSONArray("history")
            val historyList = mutableListOf<CalcHistory>()
            if (hArr != null) {
                for (i in 0 until hArr.length()) {
                    val obj = hArr.getJSONObject(i)
                    historyList.add(
                        CalcHistory(
                            expression = obj.optString("expression", ""),
                            result = obj.optString("result", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Rates
            val rArr = root.optJSONArray("rates")
            val ratesList = mutableListOf<OfflineCurrencyRate>()
            if (rArr != null) {
                for (i in 0 until rArr.length()) {
                    val obj = rArr.getJSONObject(i)
                    ratesList.add(
                        OfflineCurrencyRate(
                            currencyCode = obj.optString("currencyCode", ""),
                            rateToUSD = obj.optDouble("rateToUSD", 1.0),
                            displayNameZh = obj.optString("displayNameZh", ""),
                            displayNameEn = obj.optString("displayNameEn", ""),
                            symbol = obj.optString("symbol", "")
                        )
                    )
                }
            }

            return Triple(settings, historyList, ratesList)
        }
    }
}
