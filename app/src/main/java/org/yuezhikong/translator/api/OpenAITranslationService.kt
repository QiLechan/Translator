package org.yuezhikong.translator.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import org.yuezhikong.translator.config.ApiConfig

/**
 * OpenAI兼容的API服务类
 */
class OpenAITranslationService {
    companion object {
        private val client = OkHttpClient()
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }

    /**
     * 使用OpenAI兼容API进行翻译
     * @param sourceText 源文本
     * @param sourceLang 源语言名称
     * @param targetLang 目标语言名称
     * @return 翻译结果
     */
    suspend fun translate(sourceText: String, sourceLang: String, targetLang: String): String {
        return withContext(Dispatchers.IO) {
            try {
                // 构建提示词
                val prompt = buildTranslationPrompt(sourceText, sourceLang, targetLang)
                
                // 构建请求体
                val requestBody = createRequestBody(prompt)
                
                // 创建请求
                val request = Request.Builder()
                    .url(ApiConfig.API_ENDPOINT)
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer ${ApiConfig.API_KEY}")
                    .addHeader("Content-Type", "application/json")
                    .build()

                // 执行请求
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("API请求失败: ${response.code} ${response.message}")
                    }
                    
                    val responseBody = response.body?.string()
                        ?: throw IOException("响应体为空")
                    
                    // 解析响应
                    return@withContext parseResponse(responseBody)
                }
            } catch (e: Exception) {
                throw Exception("翻译请求出错: ${e.message}")
            }
        }
    }
    
    /**
     * 构建翻译提示词
     */
    private fun buildTranslationPrompt(sourceText: String, sourceLang: String, targetLang: String): String {
        return "请将以下${sourceLang}文本翻译成${targetLang}:\n\n$sourceText\n\n只返回翻译结果："
    }
    
    /**
     * 创建请求体
     */
    private fun createRequestBody(prompt: String): RequestBody {
        // 创建消息对象
        val messageObj = JSONObject()
        messageObj.put("role", "user")
        messageObj.put("content", prompt)
        
        // 创建消息数组
        val messagesArray = JSONArray()
        messagesArray.put(messageObj)
        
        // 创建主对象
        val json = JSONObject()
        json.put("model", ApiConfig.MODEL_NAME)
        json.put("messages", messagesArray)
        json.put("temperature", 0.3) // 降低随机性以获得更一致的翻译
        
        return json.toString().toRequestBody(JSON)
    }
    
    /**
     * 解析API响应
     */
    private fun parseResponse(responseBody: String): String {
        val jsonObject = JSONObject(responseBody)
        val choices = jsonObject.getJSONArray("choices")
        
        if (choices.length() > 0) {
            val message = choices.getJSONObject(0).getJSONObject("message")
            return message.getString("content").trim()
        }
        
        throw Exception("API响应中没有找到翻译结果")
    }
}