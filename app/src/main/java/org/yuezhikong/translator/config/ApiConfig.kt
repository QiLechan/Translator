package org.yuezhikong.translator.config

object ApiConfig {
    // OpenAI兼容API端点
    var API_ENDPOINT = "https://api.siliconflow.cn/v1/chat/completions"

    val AUDIO_API_URL = "https://api.siliconflow.cn/v1/audio/transcriptions"
    
    // API密钥
    var API_KEY = "sk-bolahwnzsngvspbioljfnmsemuujkrdbvzccqkrykobqffsh"
    
    // 翻译模型名称
    var TRANS_MODEL_NAME = "Qwen/Qwen3-Coder-30B-A3B-Instruct"

    // 语音输入模型名称
    var AUDIO_INPUT_MODEL_NAME = "FunAudioLLM/SenseVoiceSmall"

    // 语音输出模型名称
    var AUDIO_OUTPUT_MODEL_NAME = "fnlp/MOSS-TTSD-v0.5"
}