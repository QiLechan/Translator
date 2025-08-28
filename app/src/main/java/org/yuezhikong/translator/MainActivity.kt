@file:Suppress("UnusedMaterial3ScaffoldPaddingParameter")

package org.yuezhikong.translator

import android.os.Build
import android.os.Bundle
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.yuezhikong.translator.api.OpenAITranslationService

// ===== Routes =====
object Routes {
    const val Translate = "translate"
    const val History = "history"
}

// ===== MainActivity =====
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TranslatorApp() }
    }
}

// ===== Theme（MD3 动态色 + 低版本回退）=====
@Composable
fun TranslatorTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val colorScheme =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (darkTheme) darkColorScheme() else lightColorScheme()
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        shapes = Shapes(),
        content = content
    )
}

// ===== 数据模型 =====
data class Language(val code: String, val name: String)
data class TranslationItem(
    val id: Long,
    val sourceLang: Language,
    val targetLang: Language,
    val sourceText: String,
    val translatedText: String
)

// ===== ViewModel（使用OpenAI兼容API）=====
class TranslateViewModel : ViewModel() {
    private val translationService = OpenAITranslationService()
    private val availableLanguages = listOf(
        Language("auto", "自动检测"),
        Language("en", "英语"),
        Language("zh", "中文"),
        Language("ja", "日语"),
        Language("ko", "韩语"),
        Language("es", "西班牙语"),
        Language("fr", "法语"),
    )

    var sourceLang by mutableStateOf(availableLanguages[0]); private set
    var targetLang by mutableStateOf(availableLanguages[2]); private set

    var sourceText by mutableStateOf(""); private set
    var translatedText by mutableStateOf(""); private set

    var history = mutableStateListOf<TranslationItem>(); private set
    
    // 添加加载状态
    var isLoading by mutableStateOf(false); private set

    fun updateSourceLang(lang: Language) { sourceLang = lang }
    fun updateTargetLang(lang: Language) { targetLang = lang }
    fun updateSourceText(text: String) { sourceText = text }

    fun swapLanguages() {
        if (sourceLang.code != "auto") {
            val tmp = sourceLang
            sourceLang = targetLang
            targetLang = tmp
        }
    }

    fun translate() {
        viewModelScope.launch {
            if (sourceText.isBlank()) {
                translatedText = ""
                return@launch
            }
            
            isLoading = true
            try {
                // 使用OpenAI兼容API进行翻译
                val result = translationService.translate(
                    sourceText,
                    getSourceLanguageName(),
                    getTargetLanguageName()
                )
                translatedText = result
                
                // 添加到历史记录
                if (translatedText.isNotBlank()) {
                    history.add(0, TranslationItem(
                        id = System.currentTimeMillis(),
                        sourceLang = sourceLang,
                        targetLang = targetLang,
                        sourceText = sourceText,
                        translatedText = translatedText
                    ))
                    if (history.size > 20) history.removeAt(history.lastIndex)
                }
            } catch (e: Exception) {
                translatedText = "翻译出错: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    private fun getSourceLanguageName(): String {
        return when (sourceLang.code) {
            "auto" -> "任意语言"
            "en" -> "英语"
            "zh" -> "中文"
            "ja" -> "日语"
            "ko" -> "韩语"
            "es" -> "西班牙语"
            "fr" -> "法语"
            else -> sourceLang.name
        }
    }
    
    private fun getTargetLanguageName(): String {
        return when (targetLang.code) {
            "en" -> "英语"
            "zh" -> "中文"
            "ja" -> "日语"
            "ko" -> "韩语"
            "es" -> "西班牙语"
            "fr" -> "法语"
            else -> targetLang.name
        }
    }

    fun languages(): List<Language> = availableLanguages
}

// ===== 语言选择行 =====
@Composable
fun LanguageRow(
    source: Language,
    target: Language,
    onPickSource: () -> Unit,
    onPickTarget: () -> Unit,
    onSwap: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilterChip(
            selected = true,
            onClick = onPickSource,
            label = { Text(source.name) },
            leadingIcon = { Icon(Icons.Rounded.Language, contentDescription = null) }
        )
        IconButton(onClick = onSwap) {
            Icon(Icons.Rounded.SwapHoriz, contentDescription = "交换语言")
        }
        AssistChip(
            onClick = onPickTarget,
            label = { Text(target.name) },
            leadingIcon = { Icon(Icons.Rounded.Translate, contentDescription = null) }
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = { /* TODO: 收藏功能 */ }) {
            Icon(Icons.Rounded.BookmarkAdd, contentDescription = "收藏")
        }
    }
}

// ===== 翻译结果卡片 =====
@Composable
fun ResultCard(text: String, onCopySuccess: () -> Unit = {}) {
    val context = LocalContext.current
    val clipboardManager = ContextCompat.getSystemService(context, ClipboardManager::class.java)
    
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Output, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("翻译结果", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { 
                    // 复制到剪贴板
                    val clip = ClipData.newPlainText("翻译结果", text)
                    clipboardManager?.setPrimaryClip(clip)
                    
                    // 调用成功回调
                    onCopySuccess()
                }) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = "复制")
                }
                IconButton(onClick = { /* TODO: 朗读 */ }) {
                    Icon(Icons.Rounded.VolumeUp, contentDescription = "朗读")
                }
            }
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// ===== 历史记录 =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(items: List<TranslationItem>) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (items.isEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.History, contentDescription = null)
                Spacer(Modifier.height(8.dp))
                Text("暂无历史记录", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    ListItem(
                        headlineContent = {
                            Text("${item.sourceLang.name} → ${item.targetLang.name}")
                        },
                        supportingContent = {
                            Text(item.sourceText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        trailingContent = {
                            Icon(Icons.Rounded.Output, contentDescription = null)
                        }
                    )
                    Divider()
                }
            }
        }
    }
}

// ===== 语言选择底部弹窗 =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerSheet(
    title: String,
    languages: List<Language>,
    onDismiss: () -> Unit,
    onChoose: (Language) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(title, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleLarge)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        ) {
            items(languages) { lang ->
                ListItem(
                    headlineContent = { Text(lang.name) },
                    supportingContent = { Text(lang.code) },
                    leadingContent = { Icon(Icons.Rounded.Flag, contentDescription = null) },
                    modifier = Modifier
                        .clickable { onChoose(lang) }
                        .padding(horizontal = 8.dp)
                )
                Divider()
            }
        }
    }
}

// ===== 翻译主界面 =====
@Composable
fun TranslateScreen(
    vm: TranslateViewModel,
    onPickSource: () -> Unit,
    onPickTarget: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val halfScreenHeight = configuration.screenHeightDp.dp * 0.5f

    // 添加 verticalScroll
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()), // ← 支持整体滚动
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LanguageRow(
            source = vm.sourceLang,
            target = vm.targetLang,
            onPickSource = onPickSource,
            onPickTarget = onPickTarget,
            onSwap = vm::swapLanguages
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = vm.sourceText,
                onValueChange = vm::updateSourceText,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = if (expanded) configuration.screenHeightDp.dp else halfScreenHeight),
                minLines = 6,
                maxLines = Int.MAX_VALUE,
                placeholder = { Text("输入要翻译的文本…") },
                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { vm.translate() }),
                singleLine = false
            )
            IconButton(onClick = { expanded = !expanded },
                modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(
                    if (expanded) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                    contentDescription = if (expanded) "退出全屏" else "全屏"
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = vm::translate, 
                modifier = Modifier.weight(1f),
                enabled = !vm.isLoading // 在加载时禁用按钮
            ) {
                if (vm.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("翻译")
                }
            }
            OutlinedButton(onClick = { vm.updateSourceText("") }) {
                Icon(Icons.Rounded.Delete, contentDescription = "清空")
            }
        }

        AnimatedVisibility(
            visible = vm.translatedText.isNotBlank(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ResultCard(
                text = vm.translatedText,
                onCopySuccess = {
                    // 显示Snackbar消息
                    vm.viewModelScope.launch {
                        snackbarHostState.showSnackbar("已复制到剪贴板")
                    }
                }
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}


// ===== App Scaffold =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorApp(vm: TranslateViewModel = viewModel()) {
    TranslatorTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        val navController = rememberNavController()
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        var showLangPicker by remember { mutableStateOf<LangPickMode?>(null) }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Text("菜单", modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium)
                    NavigationDrawerItem(
                        label = { Text("翻译") },
                        selected = currentRoute(navController) == Routes.Translate,
                        onClick = {
                            navController.navigate(Routes.Translate) {
                                popUpTo(navController.graph.startDestinationId) { inclusive = false }
                                launchSingleTop = true
                            }
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Rounded.Translate, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text("历史记录") },
                        selected = currentRoute(navController) == Routes.History,
                        onClick = {
                            navController.navigate(Routes.History) {
                                popUpTo(navController.graph.startDestinationId) { inclusive = false }
                                launchSingleTop = true
                            }
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Rounded.History, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        ) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            val title = when (currentRoute(navController)) {
                                Routes.History -> "历史记录"
                                else -> "翻译"
                            }
                            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Rounded.Menu, contentDescription = "菜单")
                            }
                        },
                        actions = {
                            IconButton(onClick = { /* TODO: 设置 */ }) {
                                Icon(Icons.Rounded.Settings, contentDescription = "设置")
                            }
                        }
                    )
                },
                floatingActionButton = {
                    if (currentRoute(navController) == Routes.Translate) {
                        ExtendedFloatingActionButton(
                            text = { Text("语音输入") },
                            icon = { Icon(Icons.Rounded.Mic, contentDescription = null) },
                            onClick = { /* TODO: 语音识别入口 */ }
                        )
                    }
                }
            ) { innerPadding ->
                Surface(Modifier.padding(innerPadding)) {
                    NavHost(navController, startDestination = Routes.Translate) {
                        composable(Routes.Translate) {
                            TranslateScreen(
                                vm = vm,
                                onPickSource = { showLangPicker = LangPickMode.Source },
                                onPickTarget = { showLangPicker = LangPickMode.Target },
                                snackbarHostState = snackbarHostState
                            )
                        }
                        composable(Routes.History) {
                            HistoryScreen(items = vm.history)
                        }
                    }
                }

                if (showLangPicker != null) {
                    LanguagePickerSheet(
                        title = if (showLangPicker == LangPickMode.Source) "选择源语言" else "选择目标语言",
                        languages = vm.languages(),
                        onDismiss = { showLangPicker = null },
                        onChoose = { lang ->
                            if (showLangPicker == LangPickMode.Source) vm.updateSourceLang(lang)
                            else vm.updateTargetLang(lang)
                            showLangPicker = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun currentRoute(navController: NavHostController): String? {
    val backStackEntry by navController.currentBackStackEntryAsState()
    return backStackEntry?.destination?.route
}

enum class LangPickMode { Source, Target }



// ===== 设计要点（简述）=====
/*
1) 色彩：优先使用动态取色（Dynamic Color）与色彩层级（primary/secondary/tertiary、surfaceContainer）。重要操作使用 Filled/FilledTonal 按钮，次要操作用 Outline。
2) 排版：titleLarge/Medium、bodyLarge，避免自定义字号；长文案使用 bodyLarge 提升易读性。
3) 触达：触控目标≥48dp；水平内边距≥16dp；组件间距使用 8/12/16dp 的 4dp 基数。
4) 导航：单 Activity + TopAppBar；语音入口使用 FAB，强调高频场景。
5) 动效：进入/结果切换使用 fade 动画；避免过度动画；遵循 Material Motion。
6) 无障碍：为图标提供 contentDescription；对比度≥4.5；支持动态字体与横屏；语音朗读兼容。
7) 平台特性：边到边（edge-to-edge）、沉浸式系统栏、键盘 Insets；深色模式。
8) 国际化：避免硬编码字符串；使用 string 资源与 plural；考虑断句、LTR/RTL；对专有名词保留大小写。
9) 性能：状态提升 + remember；列表使用 Lazy 组件；避免在 Composition 执行重型工作。
10) 隐私：离线翻译优先；明确网络调用与日志策略；麦克风权限前置教育说明。
*/

// ===== 下一步集成建议 =====
/*
- 引擎接入：
  · 本地：谷歌 ML Kit On-Device Translation（支持部分语种，离线包可下载）
  · 云端：自建服务或三方 API（例如 DeepL、Google Cloud Translate、OpenAI 等）
- 语音：SpeechRecognizer 或语音 SDK；结果写入 sourceText 并自动触发 translate()
- 历史/收藏：Room 持久化 + DataStore 偏好；分享/复制/朗读功能完善
- 测试：UI 测试（Compose UITest）、无障碍检查（Accessibility Scanner）、CI 构建
*/
