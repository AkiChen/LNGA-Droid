@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aki.lnga.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aki.lnga.data.AppCacheManager
import com.aki.lnga.data.DEFAULT_DOUBLE_SWIPE_DISTANCE_DP
import com.aki.lnga.data.DEFAULT_DOUBLE_SWIPE_INTERVAL_MILLIS
import com.aki.lnga.data.MAX_DOUBLE_SWIPE_DISTANCE_DP
import com.aki.lnga.data.MAX_DOUBLE_SWIPE_INTERVAL_MILLIS
import com.aki.lnga.data.MIN_DOUBLE_SWIPE_DISTANCE_DP
import com.aki.lnga.data.MIN_DOUBLE_SWIPE_INTERVAL_MILLIS
import com.aki.lnga.data.FontScaleOption
import com.aki.lnga.data.ImageLoadingMode
import com.aki.lnga.data.PageControlPosition
import com.aki.lnga.ui.theme.ThemeColorOption
import com.aki.lnga.ui.theme.ThemeAppearanceOption
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
internal fun SettingsScreen(
    state: MainUiState,
    themeColor: ThemeColorOption,
    themeAppearance: ThemeAppearanceOption,
    automaticNight: Boolean,
    onThemeColorChange: (ThemeColorOption) -> Unit,
    onThemeAppearanceChange: (ThemeAppearanceOption) -> Unit,
    onAutomaticNightChange: (Boolean) -> Unit,
    onTopicSubBoardIconsChange: (Boolean) -> Unit,
    onHideTradingTopicsChange: (Boolean) -> Unit,
    onAutoNextPageChange: (Boolean) -> Unit,
    onDoubleSwipeNextPageChange: (Boolean) -> Unit,
    onDoubleSwipeIntervalChange: (Int) -> Unit,
    onDoubleSwipeDistanceChange: (Int) -> Unit,
    onResumeLastReadChange: (Boolean) -> Unit,
    onPageControlPositionChange: (PageControlPosition) -> Unit,
    onFontScaleChange: (FontScaleOption) -> Unit,
    onImageLoadingModeChange: (ImageLoadingMode) -> Unit,
    onInternalBrowserChange: (Boolean) -> Unit,
    onOpenAiSettings: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showImageDiagnostics by remember { mutableStateOf(false) }
    var cacheRevision by remember { mutableIntStateOf(0) }
    var cacheText by remember { mutableStateOf("正在统计…") }
    var clearingCache by remember { mutableStateOf(false) }

    LaunchedEffect(cacheRevision) {
        cacheText = formatCacheBytes(AppCacheManager.snapshot(context).totalBytes)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            item { SettingsSection("AI") }
            item {
                val active = state.aiConfigs.firstOrNull { it.active }
                SettingsActionRow(
                    title = "AI 服务与模型",
                    subtitle = active?.let { "当前：${it.name} · ${it.model}" } ?: "配置服务商、模型和加密 API Key",
                    icon = Icons.Default.SmartToy,
                    onClick = onOpenAiSettings,
                )
            }

            item { SettingsSection("外观") }
            item {
                SettingsChoiceGroup(
                    title = "应用主题",
                    subtitle = "极致黑夜使用纯黑背景，夜间阅读更柔和，也适合 OLED 屏幕",
                    options = ThemeAppearanceOption.entries,
                    selected = themeAppearance,
                    label = ThemeAppearanceOption::displayName,
                    onSelect = onThemeAppearanceChange,
                )
            }
            item {
                SettingsSwitchRow(
                    title = "自动切换极致黑夜",
                    subtitle = "每天 23:00 至次日 07:00 自动启用；其他时间恢复上方选择的主题",
                    checked = automaticNight,
                    onCheckedChange = onAutomaticNightChange,
                )
            }
            item {
                SettingsActionRow("主题色", themeColor.displayName, Icons.Default.Palette) { showThemeDialog = true }
            }
            item {
                SettingsChoiceGroup(
                    title = "字体大小",
                    subtitle = "在系统字体大小基础上进一步调整",
                    options = FontScaleOption.entries,
                    selected = state.fontScale,
                    label = FontScaleOption::displayName,
                    onSelect = onFontScaleChange,
                )
            }
            item {
                SettingsSwitchRow(
                    title = "标题使用子版块图标",
                    subtitle = "关闭时在主题标题前显示子版块文字标签，开启后使用自定义小图标",
                    checked = state.topicSubBoardIcons,
                    onCheckedChange = onTopicSubBoardIconsChange,
                )
            }

            item { SettingsSection("图片与网络") }
            item {
                SettingsChoiceGroup(
                    title = "帖子图片加载模式",
                    subtitle = "影响正文图片、引用缩略图和主题预览图；头像及版面图标不受影响",
                    options = ImageLoadingMode.entries,
                    selected = state.imageLoadingMode,
                    label = ImageLoadingMode::displayName,
                    onSelect = onImageLoadingModeChange,
                )
            }
            item {
                SettingsSwitchRow(
                    "自动续读上次楼层",
                    "再次打开已读主题时，恢复到上次浏览的页面和楼层",
                    state.resumeLastRead,
                    onResumeLastReadChange,
                )
            }
            item {
                SettingsSwitchRow(
                    title = "内置网页浏览",
                    subtitle = "普通 HTTP/HTTPS 链接在 App 内打开；平台媒体卡片仍交给对应 App 或浏览器",
                    checked = state.internalBrowserEnabled,
                    onCheckedChange = onInternalBrowserChange,
                )
            }
            item {
                SettingsActionRow("图片加载与查看诊断", "查看并复制最近 100 条脱敏记录", Icons.Default.Search) {
                    showImageDiagnostics = true
                }
            }
            item {
                SettingsActionRow(
                    title = if (clearingCache) "正在清理缓存" else "清理媒体与网页缓存",
                    subtitle = "当前约 $cacheText；不会删除账号、设置、历史或收藏",
                    icon = Icons.Default.Delete,
                    enabled = !clearingCache,
                ) {
                    clearingCache = true
                    scope.launch {
                        runCatching { AppCacheManager.clearMediaAndWebCache(context) }
                            .onSuccess {
                                cacheText = formatCacheBytes(it.totalBytes)
                                Toast.makeText(context, "缓存已清理", Toast.LENGTH_SHORT).show()
                            }
                            .onFailure { Toast.makeText(context, "缓存清理失败", Toast.LENGTH_SHORT).show() }
                        clearingCache = false
                        cacheRevision++
                    }
                }
            }

            item { SettingsSection("浏览") }
            item {
                SettingsSwitchRow(
                    "隐藏交易类主题",
                    "隐藏“优惠信息”和“二手交易”子版块的主题",
                    state.hideTradingTopics,
                    onHideTradingTopicsChange,
                )
            }
            item {
                SettingsSwitchRow(
                    "持续上拉停留自动翻页",
                    "到达帖子末端后继续上拉并短暂停留进入下一页",
                    state.autoNextPage,
                    onAutoNextPageChange,
                )
            }
            item {
                SettingsSwitchRow(
                    "快速连续上滑两次自动翻页",
                    "到达帖子末端后连续快速上滑两次进入下一页",
                    state.doubleSwipeNextPage,
                    onDoubleSwipeNextPageChange,
                )
            }
            item {
                SettingsThresholdSlider(
                    title = "两次上滑最大间隔",
                    value = state.doubleSwipeIntervalMillis,
                    valueRange = MIN_DOUBLE_SWIPE_INTERVAL_MILLIS..MAX_DOUBLE_SWIPE_INTERVAL_MILLIS,
                    step = 50,
                    unit = "ms",
                    enabled = state.doubleSwipeNextPage,
                    onValueChange = onDoubleSwipeIntervalChange,
                )
            }
            item {
                SettingsThresholdSlider(
                    title = "单次上滑最小距离",
                    value = state.doubleSwipeDistanceDp,
                    valueRange = MIN_DOUBLE_SWIPE_DISTANCE_DP..MAX_DOUBLE_SWIPE_DISTANCE_DP,
                    step = 4,
                    unit = "dp",
                    enabled = state.doubleSwipeNextPage,
                    onValueChange = onDoubleSwipeDistanceChange,
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        enabled = state.doubleSwipeNextPage &&
                            (state.doubleSwipeIntervalMillis != DEFAULT_DOUBLE_SWIPE_INTERVAL_MILLIS ||
                                state.doubleSwipeDistanceDp != DEFAULT_DOUBLE_SWIPE_DISTANCE_DP),
                        onClick = {
                            onDoubleSwipeIntervalChange(DEFAULT_DOUBLE_SWIPE_INTERVAL_MILLIS)
                            onDoubleSwipeDistanceChange(DEFAULT_DOUBLE_SWIPE_DISTANCE_DP)
                        },
                    ) { Text("恢复双上滑默认值") }
                }
                HorizontalDivider()
            }
            item {
                SettingsChoiceGroup(
                    title = "选页按钮位置",
                    subtitle = null,
                    options = PageControlPosition.entries,
                    selected = state.pageControlPosition,
                    label = { if (it == PageControlPosition.Left) "固定在左侧" else "固定在右侧" },
                    onSelect = onPageControlPositionChange,
                )
            }
        }
    }

    if (showThemeDialog) {
        ThemeColorDialog(
            selected = themeColor,
            onSelect = { onThemeColorChange(it); showThemeDialog = false },
            onDismiss = { showThemeDialog = false },
        )
    }
    if (showImageDiagnostics) ImageDiagnosticDialog { showImageDiagnostics = false }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        title,
        modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 6.dp),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
    HorizontalDivider()
}

@Composable
private fun SettingsThresholdSlider(
    title: String,
    value: Int,
    valueRange: IntRange,
    step: Int,
    unit: String,
    enabled: Boolean,
    onValueChange: (Int) -> Unit,
) {
    var sliderValue by remember(value) { mutableFloatStateOf(value.toFloat()) }
    val snappedValue = ((sliderValue / step).roundToInt() * step).coerceIn(valueRange)
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            )
            Text(
                "$snappedValue $unit",
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = { raw -> sliderValue = ((raw / step).roundToInt() * step).toFloat() },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            steps = ((valueRange.last - valueRange.first) / step - 1).coerceAtLeast(0),
            enabled = enabled,
            onValueChangeFinished = { onValueChange(snappedValue) },
        )
    }
}

@Composable
private fun <T> SettingsChoiceGroup(
    title: String,
    subtitle: String?,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        subtitle?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        options.forEach { option ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(option) }.padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = option == selected, onClick = { onSelect(option) })
                Text(label(option))
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (enabled) Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        else CircularProgressIndicator(modifier = Modifier.padding(2.dp), strokeWidth = 2.dp)
        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider()
}

internal fun formatCacheBytes(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0)
    else -> String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0))
}
