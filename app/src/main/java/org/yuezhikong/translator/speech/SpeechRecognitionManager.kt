package org.yuezhikong.translator.speech

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import org.yuezhikong.translator.config.ApiConfig
import java.io.File
import java.io.IOException

class SpeechRecognitionManager(private val context: Context) {
    private val client = OkHttpClient()
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isRecording = false
    private var onResultListener: ((String) -> Unit)? = null
    private var onErrorListener: ((String) -> Unit)? = null
    
    companion object {
        private const val TAG = "SpeechRecognition"
    }
    
    /**
     * 检查是否具有录音权限
     */
    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 开始录音
     */
    fun startRecording(): Boolean {
        if (!hasRecordPermission()) {
            Log.e(TAG, "没有录音权限")
            return false
        }
        
        return try {
            // 创建临时音频文件，使用MP3格式
            val fileName = "speech_recording_${System.currentTimeMillis()}.mp3"
            audioFile = File(context.cacheDir, fileName)
            
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4) // MP3使用MPEG_4格式
                setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC) // 使用HE_AAC编码器支持MP3
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }
            
            isRecording = true
            Log.d(TAG, "开始录音")
            true
        } catch (e: Exception) {
            Log.e(TAG, "录音初始化失败", e)
            false
        }
    }
    
    /**
     * 停止录音
     */
    fun stopRecording(): Boolean {
        if (!isRecording) return false
        
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            Log.d(TAG, "录音已停止")
            true
        } catch (e: Exception) {
            Log.e(TAG, "停止录音失败", e)
            false
        }
    }
    
    /**
     * 设置结果监听器
     */
    fun setOnResultListener(listener: (String) -> Unit) {
        onResultListener = listener
    }
    
    /**
     * 设置错误监听器
     */
    fun setOnErrorListener(listener: (String) -> Unit) {
        onErrorListener = listener
    }
    
    /**
     * 上传音频文件进行语音识别
     */
    fun transcribeAudio() {
        val file = audioFile
        if (file == null || !file.exists()) {
            onErrorListener?.invoke("音频文件不存在")
            return
        }
        
        if (ApiConfig.API_KEY.isBlank()) {
            onErrorListener?.invoke("API密钥未配置，请在设置中配置SiliconFlow API密钥")
            return
        }
        
        // 构建请求体
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("model", ApiConfig.AUDIO_INPUT_MODEL_NAME)
            .addFormDataPart("file", file.name, file.asRequestBody("audio/mpeg".toMediaType()))
            .build()
        
        // 构建请求
        val request = Request.Builder()
            .url(ApiConfig.AUDIO_API_URL)
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
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        // 解析响应
                        val jsonObject = JSONObject(responseBody)
                        val text = jsonObject.optString("text", "")
                        Log.d(TAG, "语音识别结果: $text")
                        onResultListener?.invoke(text)
                    } else {
                        val error = "API请求失败: ${response.code} - ${responseBody}"
                        Log.e(TAG, error)
                        onErrorListener?.invoke(error)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "解析响应失败", e)
                    onErrorListener?.invoke("解析响应失败: ${e.message}")
                } finally {
                    response.close()
                    // 删除临时音频文件
                    try {
                        file.delete()
                    } catch (e: Exception) {
                        Log.w(TAG, "删除临时音频文件失败", e)
                    }
                }
            }
        })
    }
    
    /**
     * 释放资源
     */
    fun release() {
        try {
            mediaRecorder?.release()
            mediaRecorder = null
            audioFile?.delete()
            audioFile = null
            isRecording = false
        } catch (e: Exception) {
            Log.e(TAG, "释放资源失败", e)
        }
    }
}