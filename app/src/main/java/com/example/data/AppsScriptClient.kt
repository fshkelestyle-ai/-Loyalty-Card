package com.example.data

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AppsScriptClient {
  private val client =
    OkHttpClient.Builder()
      .connectTimeout(20, TimeUnit.SECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .writeTimeout(30, TimeUnit.SECONDS)
      .followRedirects(true)
      .followSslRedirects(true)
      .build()

  private val textJsonMedia = "text/plain; charset=utf-8".toMediaType()

  suspend fun postAction(
    webAppUrl: String,
    action: String,
    payload: Map<String, Any?> = emptyMap(),
  ): Result<JSONObject> =
    withContext(Dispatchers.IO) {
      runCatching {
        val cleanUrl = webAppUrl.trim()
        require(cleanUrl.startsWith("https://")) {
          "رابط Google Apps Script غير صالح. يجب أن يبدأ بـ https://"
        }

        val bodyJson = JSONObject()
        bodyJson.put("action", action)
        payload.forEach { (k, v) ->
          when (v) {
            is Map<*, *> -> bodyJson.put(k, JSONObject(v))
            else -> bodyJson.put(k, v)
          }
        }

        val request =
          Request.Builder()
            .url(cleanUrl)
            .post(bodyJson.toString().toRequestBody(textJsonMedia))
            .build()

        client.newCall(request).execute().use { response ->
          val raw = response.body?.string().orEmpty()
          if (!response.isSuccessful) {
            throw IllegalStateException("خطأ في الخادم (${response.code})")
          }
          if (raw.trimStart().startsWith("<")) {
            throw IllegalStateException(
              "تعذّر قراءة الاستجابة: تأكد من نشر السكربت بصلاحية 'Anyone'"
            )
          }
          val obj = JSONObject(raw)
          if (!obj.optBoolean("ok", false)) {
            throw IllegalStateException(obj.optString("error", "حدث خطأ غير معروف"))
          }
          obj
        }
      }
    }
}
