package com.mechvac.task

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Talks to the MechVac Task website with this phone's token. Call from a background thread only. */
object Api {
    class Result(val code: Int, val json: JSONObject?)

    fun call(ctx: Context, action: String, tokenOverride: String? = null, form: Map<String, String>? = null): Result {
        val prefs = Prefs(ctx)
        val token = tokenOverride ?: prefs.token ?: return Result(401, null)
        val base = prefs.baseUrl
        if (base.isEmpty()) return Result(0, null)
        var conn: HttpURLConnection? = null
        return try {
            conn = URL("$base/app_api.php?action=$action").openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            conn.setRequestProperty("X-Device-Token", token)
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) MechVacTaskApp/" + BuildConfig.VERSION_NAME)
            // Free hosts (e.g. InfinityFree) only answer requests that carry the security cookie the in-app browser received
            val cookies = try { android.webkit.CookieManager.getInstance().getCookie(base) } catch (e: Throwable) { null }
            if (!cookies.isNullOrEmpty()) conn.setRequestProperty("Cookie", cookies)
            if (form != null) {
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
                val body = form.entries.joinToString("&") { java.net.URLEncoder.encode(it.key, "UTF-8") + "=" + java.net.URLEncoder.encode(it.value, "UTF-8") }
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            val json = runCatching { JSONObject(text) }.getOrNull()
            if (tokenOverride == null) {
                if (code == 401) prefs.clearLink()   // signed out on the website or account deactivated
                // a web page instead of data: the hosting's security check blocked the app until it is opened again
                prefs.serverBlockedAt = if (json == null && code in 200..299 && text.contains("<html", ignoreCase = true)) System.currentTimeMillis()
                    else if (json != null) 0L else prefs.serverBlockedAt
            }
            Result(code, json)
        } catch (e: Exception) {
            Result(0, null)
        } finally {
            conn?.disconnect()
        }
    }
}
