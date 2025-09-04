package org.yuezhikong.translator.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Api
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.ModelTraining
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.yuezhikong.translator.config.ApiConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    var apiEndpoint by remember { mutableStateOf(ApiConfig.API_ENDPOINT) }
    var apiKey by remember { mutableStateOf(ApiConfig.API_KEY) }
    var transModelName by remember { mutableStateOf(ApiConfig.TRANS_MODEL_NAME) }
    var audioInputModelName by remember { mutableStateOf(ApiConfig.AUDIO_INPUT_MODEL_NAME) }

    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("设置") },
//                navigationIcon = {
//                    IconButton(onClick = onNavigateBack) {
//                        Icon(
//                            Icons.AutoMirrored.Rounded.ArrowBack,
//                            contentDescription = "返回"
//                        )
//                    }
//                }
//            )
//        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // API设置部分
            Text(
                "API设置",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            OutlinedTextField(
                value = apiEndpoint,
                onValueChange = { apiEndpoint = it },
                label = { Text("API端点") },
                leadingIcon = {
                    Icon(Icons.Rounded.Api, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API密钥") },
                leadingIcon = {
                    Icon(Icons.Rounded.Key, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = transModelName,
                onValueChange = { transModelName = it },
                label = { Text("翻译模型") },
                leadingIcon = {
                    Icon(Icons.Rounded.ModelTraining, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = audioInputModelName,
                onValueChange = { audioInputModelName = it },
                label = { Text("语音输入模型") },
                leadingIcon = {
                    Icon(Icons.Rounded.ModelTraining, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth()
            )
            
//            Spacer(modifier = Modifier.height(16.dp))
//
//            Text(
//                "语音识别API设置",
//                style = MaterialTheme.typography.titleLarge,
//                fontWeight = FontWeight.Bold
//            )
//
//            Text(
//                "注意：语音识别功能需要SiliconFlow API密钥，与翻译API密钥相同",
//                style = MaterialTheme.typography.bodyMedium,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//
            Spacer(modifier = Modifier.weight(1f))
            
            // 保存按钮
            Button(
                onClick = {
                    // 保存设置
                    ApiConfig.API_ENDPOINT = apiEndpoint
                    ApiConfig.API_KEY = apiKey
                    ApiConfig.TRANS_MODEL_NAME = transModelName
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存设置")
            }
        }
    }
}