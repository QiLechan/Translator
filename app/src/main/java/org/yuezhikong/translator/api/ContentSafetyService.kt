package org.yuezhikong.translator.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

/**
 * 内容安全审查服务
 */
class ContentSafetyService {
    companion object {
        private const val SAFETY_API_URL = "https://safe.nanaraku.com"
        private val client = OkHttpClient()
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }

    /**
     * 检查内容是否安全
     * @param message 用户输入的内容
     * @return 如果内容安全返回true，否则返回false
     * @throws Exception 当API请求失败时抛出异常
     */
    suspend fun isContentSafe(message: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 构建请求体
                val json = JSONObject().apply {
                    put("message", message)
                }
                
                // 创建请求
                val request = Request.Builder()
                    .url(SAFETY_API_URL)
                    .post(json.toString().toRequestBody(JSON))
                    .addHeader("Content-Type", "application/json")
                    .build()

                // 执行请求
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("安全审查API请求失败: ${response.code} ${response.message}")
                    }
                    
                    val responseBody = response.body?.string()
                        ?: throw IOException("安全审查API响应体为空")
                    
                    // 解析响应
                    return@withContext parseSafetyResponse(responseBody)
                }
            } catch (e: Exception) {
                throw Exception("内容安全审查出错: ${e.message}")
            }
        }
    }
    
    /**
     * 解析安全审查API响应
     * @param responseBody API响应体
     * @return 如果内容安全返回true，否则返回false
     */
    private fun parseSafetyResponse(responseBody: String): Boolean {
        try {
            val jsonObject = JSONObject(responseBody)
            return jsonObject.getString("data") == "safe"
        } catch (e: Exception) {
            // 如果解析失败，认为内容不安全
            return false
        }
    }
}