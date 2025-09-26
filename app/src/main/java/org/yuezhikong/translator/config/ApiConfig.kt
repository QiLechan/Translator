package org.yuezhikong.translator.config

object ApiConfig {
    // OpenAI兼容API端点
    var API_ENDPOINT = "https://api.siliconflow.cn/v1/chat/completions"

    val AUDIO_INPUT_API_URL = "https://api.siliconflow.cn/v1/audio/transcriptions"

    val AUDIO_OUTPUT_API_URL = "https://api.siliconflow.cn/v1/audio/speech"
    // API密钥
    var API_KEY = ""
    
    // 翻译模型名称
    var TRANS_MODEL_NAME = "Qwen/Qwen3-Coder-30B-A3B-Instruct"

    // 语音输入模型名称
    var AUDIO_INPUT_MODEL_NAME = "FunAudioLLM/SenseVoiceSmall"

    // 语音输出模型名称
    var AUDIO_OUTPUT_MODEL_NAME = "IndexTeam/IndexTTS-2"
}