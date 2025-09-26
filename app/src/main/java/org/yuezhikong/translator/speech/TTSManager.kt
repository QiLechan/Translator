package org.yuezhikong.translator.speech

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.yuezhikong.translator.config.ApiConfig
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class TTSManager(private val context: Context) {
    private val client = OkHttpClient()
    private var mediaPlayer: MediaPlayer? = null
    private var onCompletionListener: (() -> Unit)? = null
    private var onErrorListener: ((String) -> Unit)? = null
    
    companion object {
        private const val TAG = "TTSManager"
    }
    
    /**
     * 文本转语音
     */
    fun speakText(text: String) {
        if (text.isBlank()) {
            Log.w(TAG, "文本为空，无法朗读")
            return
        }
        
        if (ApiConfig.API_KEY.isBlank()) {
            onErrorListener?.invoke("API密钥未配置，请在设置中配置SiliconFlow API密钥")
            return
        }
        
        // 构建请求体
        val json = JSONObject().apply {
            put("model", ApiConfig.AUDIO_OUTPUT_MODEL_NAME)
            put("input", text)
            put("voice", "IndexTeam/IndexTTS-2:alex")
        }
        
        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        
        // 构建请求
        val request = Request.Builder()
            .url(ApiConfig.AUDIO_OUTPUT_API_URL)
            .addHeader("Authorization", "Bearer ${ApiConfig.API_KEY}")
            .post(requestBody)
            .build()
        
        // 发送请求
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "API请求失败", e)
                onErrorListener?.invoke("网络请求失败: ${e.message}")
            }
            
            override fun onResponse(call: Call, response: Response) {
                try {
                    if (response.isSuccessful) {
                        val responseBody = response.body
                        if (responseBody != null) {
                            // 创建临时音频文件
                            val fileName = "tts_${System.currentTimeMillis()}.mp3"
                            val file = File(context.cacheDir, fileName)
                            
                            // 写入音频数据
                            val fos = FileOutputStream(file)
                            fos.use {
                                it.write(responseBody.bytes())
                            }
                            
                            // 播放音频
                            playAudioFile(file)
                        } else {
                            val error = "API响应为空"
                            Log.e(TAG, error)
                            onErrorListener?.invoke(error)
                        }
                    } else {
                        val responseBody = response.body?.string()
                        val error = "API请求失败: ${response.code} - $responseBody"
                        Log.e(TAG, error)
                        onErrorListener?.invoke(error)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "处理音频数据失败", e)
                    onErrorListener?.invoke("处理音频数据失败: ${e.message}")
                } finally {
                    response.close()
                }
            }
        })
    }
    
    /**
     * 播放音频文件
     */
    private fun playAudioFile(file: File) {
        try {
            // 释放之前的MediaPlayer资源
            mediaPlayer?.release()
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepareAsync() // 异步准备
                
                setOnPreparedListener { mp ->
                    mp.start()
                }
                
                setOnCompletionListener { mp ->
                    // 播放完成后删除临时文件
                    try {
                        file.delete()
                    } catch (e: Exception) {
                        Log.w(TAG, "删除临时音频文件失败", e)
                    }
                    
                    // 释放MediaPlayer资源
                    mp.release()
                    mediaPlayer = null
                    
                    // 调用完成回调
                    onCompletionListener?.invoke()
                }
                
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer错误: what=$what, extra=$extra")
                    try {
                        file.delete()
                    } catch (e: Exception) {
                        Log.w(TAG, "删除临时音频文件失败", e)
                    }
                    onErrorListener?.invoke("播放音频失败")
                    false
                }
                
                // 设置音量
                setVolume(1.0f, 1.0f)
            }
        } catch (e: Exception) {
            Log.e(TAG, "播放音频文件失败", e)
            try {
                file.delete()
            } catch (ex: Exception) {
                Log.w(TAG, "删除临时音频文件失败", ex)
            }
            onErrorListener?.invoke("播放音频文件失败: ${e.message}")
        }
    }
    
    /**
     * 停止播放
     */
    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "停止播放失败", e)
        }
    }
    
    /**
     * 设置播放完成监听器
     */
    fun setOnCompletionListener(listener: () -> Unit) {
        onCompletionListener = listener
    }
    
    /**
     * 设置错误监听器
     */
    fun setOnErrorListener(listener: (String) -> Unit) {
        onErrorListener = listener
    }
    
    /**
     * 释放资源
     */
    fun release() {
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "释放资源失败", e)
        }
    }
}