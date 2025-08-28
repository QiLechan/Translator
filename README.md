# Translator

一个使用Jetpack Compose构建的Android翻译应用，支持使用自定义大模型API进行翻译。

## 功能特性

- 多语言翻译支持
- 历史记录功能
- 内容安全审查
- Material Design 3 (MD3) 界面设计
- 动态色彩支持
- 深色模式适配
- 响应式布局
- 预测性返回手势支持（Android 13+）

## 配置API密钥

要使用自定义大模型API进行翻译，您可以通过以下方式配置：

1. **通过设置界面配置（推荐）**：
   - 点击应用右上角的"设置"按钮
   - 在设置界面中输入API端点、API密钥和模型名称
   - 点击"保存设置"按钮

2. **通过代码配置**：
   - 打开 `app/src/main/java/org/yuezhikong/translator/config/ApiConfig.kt` 文件
   - 修改以下常量：
     - `API_ENDPOINT`: 您的OpenAI兼容API端点
     - `API_KEY`: 您的API密钥
     - `MODEL_NAME`: 您要使用的模型名称

示例配置：
```kotlin
object ApiConfig {
    // 替换为你的OpenAI兼容API端点
    var API_ENDPOINT = "https://api.openai.com/v1/chat/completions"
    
    // 替换为你的API密钥
    var API_KEY = "sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
    
    // 替换为你的模型名称
    var MODEL_NAME = "gpt-3.5-turbo"
}
```

## 构建和运行

1. 克隆项目
2. 在Android Studio中打开项目
3. 配置API密钥（如上所述）
4. 构建并运行应用

## 依赖项

- Jetpack Compose
- OkHttp3
- JSON (org.json)
- ViewModel
- Navigation

## 许可证

[添加您的许可证信息]