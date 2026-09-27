@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package com.aki.lnga.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ActivityInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.abs
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.aki.lnga.data.TopicHotRange
import com.aki.lnga.data.TopicSortMode
import com.aki.lnga.model.BbNode
import com.aki.lnga.data.preferenceKey
import com.aki.lnga.data.BrowseHistoryEntry
import com.aki.lnga.data.TopicFavoriteEntry
import com.aki.lnga.data.PageControlPosition
import com.aki.lnga.data.ImageLoadingMode
import com.aki.lnga.data.ImageDownloader
import com.aki.lnga.data.AttachmentDownloader
import com.aki.lnga.data.ImageDiagnosticStore
import com.aki.lnga.data.BilibiliPreview
import com.aki.lnga.data.AiConversationKind
import com.aki.lnga.data.completedAssistantText
import com.aki.lnga.data.BilibiliPreviewLoader
import com.aki.lnga.data.isBilibiliUrl
import com.aki.lnga.data.DouyinPreview
import com.aki.lnga.data.DouyinPreviewLoader
import com.aki.lnga.data.isDouyinUrl
import com.aki.lnga.data.WeiboPreview
import com.aki.lnga.data.WeiboPreviewLoader
import com.aki.lnga.data.isWeiboUrl
import com.aki.lnga.data.XiaohongshuPreview
import com.aki.lnga.data.XiaohongshuPreviewLoader
import com.aki.lnga.data.isXiaohongshuUrl
import com.aki.lnga.model.ForumBoard
import com.aki.lnga.model.NgaSession
import com.aki.lnga.model.Post
import com.aki.lnga.model.PostAttachment
import com.aki.lnga.model.Topic
import com.aki.lnga.parser.BbCodeParser
import com.aki.lnga.ui.theme.ThemeColorOption
import com.aki.lnga.ui.theme.ThemeAppearanceOption
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

private val LocalSelectionGeneration = staticCompositionLocalOf { 0 }
private val LocalSelectionActivated = staticCompositionLocalOf<() -> Unit> { {} }
private val LocalBilibiliLoadingChanged = staticCompositionLocalOf<(Boolean) -> Unit> { {} }
private val LocalLinkOpener = staticCompositionLocalOf<(String) -> Unit> { {} }
private val LocalAutomaticImageLoading = staticCompositionLocalOf { true }
private val LocalNgaSession = staticCompositionLocalOf<NgaSession?> { null }
private data class PostImageGallery(val urls: List<String>, val onOpen: (String) -> Unit)
private val LocalPostImageGallery = staticCompositionLocalOf<PostImageGallery?> { null }

@Composable
internal fun LngaApp(
    viewModel: MainViewModel,
    themeColor: ThemeColorOption,
    themeAppearance: ThemeAppearanceOption,
    automaticNight: Boolean,
    onThemeColorChange: (ThemeColorOption) -> Unit,
    onThemeAppearanceChange: (ThemeAppearanceOption) -> Unit,
    onAutomaticNightChange: (Boolean) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val systemDensity = LocalDensity.current
    val isWifi = rememberWifiConnection()
    val automaticImages = state.imageLoadingMode == ImageLoadingMode.Always ||
        state.imageLoadingMode == ImageLoadingMode.WifiOnly && isWifi
    val openLink: (String) -> Unit = remember(state.internalBrowserEnabled, context, viewModel) {
        { url ->
            if (shouldOpenInInternalBrowser(url, state.internalBrowserEnabled)) {
                viewModel.openWebView(url)
            } else {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                    .onFailure { Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    CompositionLocalProvider(
        LocalDensity provides Density(systemDensity.density, systemDensity.fontScale * state.fontScale.factor),
        LocalLinkOpener provides openLink,
        LocalAutomaticImageLoading provides automaticImages,
        LocalNgaSession provides state.session,
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
      Surface(modifier = Modifier.fillMaxSize()) {
        when (state.destination) {
            Destination.Login -> LoginScreen(
                loading = state.loading,
                onSubmit = viewModel::saveCredentials,
                onWebLogin = viewModel::openWebLogin,
            )
            Destination.WebLogin -> WebLoginScreen(
                verifying = state.loading,
                resetSessionBeforeLoad = state.webLoginResetRequired,
                onSessionReset = viewModel::webLoginResetCompleted,
                onCredentials = viewModel::completeWebLogin,
                onCancel = viewModel::cancelWebLogin,
            )
            Destination.Boards -> BoardBrowserScreen(
                state = state,
                onSearch = viewModel::searchForums,
                onOpenFavorites = viewModel::openFavorites,
                onSelectBoard = viewModel::selectBoard,
                onToggleFavorite = viewModel::toggleFavoriteBoard,
                onMoveFavorite = viewModel::moveFavoriteBoard,
                onSetBoardIcon = viewModel::setBoardIconSelection,
                onOpenSettings = viewModel::openSettings,
                onOpenAiResult = {
                    viewModel.selectMostRecentAiResult()
                },
                onOpenHistory = viewModel::openHistory,
                onLogout = viewModel::logout,
            )
            Destination.Settings -> SettingsScreen(
                state = state,
                themeColor = themeColor,
                themeAppearance = themeAppearance,
                automaticNight = automaticNight,
                onThemeColorChange = onThemeColorChange,
                onThemeAppearanceChange = onThemeAppearanceChange,
                onAutomaticNightChange = onAutomaticNightChange,
                onTopicSubBoardIconsChange = viewModel::setTopicSubBoardIcons,
                onHideTradingTopicsChange = viewModel::setHideTradingTopics,
                onAutoNextPageChange = viewModel::setAutoNextPage,
                onDoubleSwipeNextPageChange = viewModel::setDoubleSwipeNextPage,
                onDoubleSwipeIntervalChange = viewModel::setDoubleSwipeIntervalMillis,
                onDoubleSwipeDistanceChange = viewModel::setDoubleSwipeDistanceDp,
                onResumeLastReadChange = viewModel::setResumeLastRead,
                onPageControlPositionChange = viewModel::setPageControlPosition,
                onFontScaleChange = viewModel::setFontScale,
                onImageLoadingModeChange = viewModel::setImageLoadingMode,
                onInternalBrowserChange = viewModel::setInternalBrowserEnabled,
                onOpenAiSettings = viewModel::openAiSettings,
                onBack = viewModel::closeSettings,
            )
            Destination.AiSettings -> AiSettingsScreen(
                state = state,
                onSave = viewModel::saveAiConfig,
                onActivate = viewModel::setActiveAiConfig,
                onDelete = viewModel::deleteAiConfig,
                onDeleteKey = viewModel::deleteAiApiKey,
                onLoadModels = viewModel::loadAiModels,
                onTest = viewModel::testAiConnection,
                onClearStatus = viewModel::clearAiStatus,
                onBack = viewModel::closeAiSettings,
            )
            Destination.AiChat -> AiChatScreen(
                state = state,
                onSend = viewModel::sendAiMessage,
                onStop = viewModel::stopAiGeneration,
                onRetry = viewModel::retryLastAiResponse,
                onNewConversation = viewModel::newAiConversation,
                onSelectConversation = viewModel::selectAiConversation,
                onDeleteConversation = viewModel::deleteAiConversation,
                onSelectConfig = viewModel::selectCurrentAiConversationConfig,
                onOpenSettings = viewModel::openAiSettings,
                onOpenLink = openLink,
                onBack = viewModel::closeAiChat,
            )
            Destination.WebView -> GeneralWebViewScreen(url = state.webViewUrl, onBack = viewModel::closeWebView)
            Destination.Favorites -> FavoriteTopicsScreen(
                entries = state.favoriteTopics,
                onOpen = viewModel::openFavorite,
                onDelete = viewModel::removeFavorite,
                onBack = viewModel::backFromFavorites,
            )
            Destination.History -> HistoryScreen(
                entries = state.historyEntries,
                onOpen = viewModel::openHistoryEntry,
                onClear = viewModel::clearHistory,
                onBack = viewModel::backFromHistory,
            )
            Destination.Profile -> UserProfileScreen(
                state = state,
                onBack = viewModel::backFromProfile,
                onAnalyze = viewModel::startUserAnalysis,
                onRetryActivity = viewModel::retryUserActivity,
                onOpenAnalysis = {
                    viewModel.selectAiConversationForContext(AiConversationKind.UserAnalysis, state.profileUid)
                },
                onOpenLink = openLink,
            )
            Destination.Topics -> TopicListScreen(
                state = state,
                onLoad = { viewModel.loadTopics(page = 1) },
                onLoadNext = viewModel::loadNextTopics,
                onSortMode = viewModel::setTopicSortMode,
                onHotRange = viewModel::setTopicHotRange,
                onScrollResetApplied = viewModel::topicScrollResetApplied,
                onOpenTopic = viewModel::openTopic,
                onLoadPreview = viewModel::loadTopicPreview,
                onOpenAiResult = {
                    viewModel.selectMostRecentAiResult()
                },
                onBack = viewModel::backToBoards,
            )
            Destination.Thread -> ThreadScreen(
                state = state,
                onBack = viewModel::backFromThread,
                autoNextPage = state.autoNextPage,
                doubleSwipeNextPage = state.doubleSwipeNextPage,
                doubleSwipeIntervalMillis = state.doubleSwipeIntervalMillis,
                doubleSwipeDistanceDp = state.doubleSwipeDistanceDp,
                pageControlPosition = state.pageControlPosition,
                isFavorite = state.selectedTopic?.tid?.let { tid ->
                    state.favoriteTopics.any { it.tid == tid }
                } == true,
                onToggleFavorite = viewModel::toggleSelectedTopicFavorite,
                onLoadIpLocation = viewModel::loadPostIpLocation,
                onOpenUser = viewModel::openUserProfile,
                onPage = { page -> state.selectedTopic?.let { viewModel.openTopicPage(it, page) } },
                onSummarize = viewModel::summarizeSelectedTopic,
                onOpenSummary = {
                    state.selectedTopic?.tid?.let { tid ->
                        viewModel.selectAiConversationForContext(AiConversationKind.TopicSummary, tid)
                    }
                },
                onReadingPosition = viewModel::recordReadingPosition,
                onResumeApplied = viewModel::resumeFloorApplied,
                onScrollRestoreApplied = viewModel::threadScrollRestoreApplied,
            )
        }
      }
      }

      state.error?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            title = { Text("请求失败") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::clearError) { Text("知道了") } },
        )
      }
    }
}

@Composable
private fun BoardBrowserScreen(
    state: MainUiState,
    onSearch: (String) -> Unit,
    onOpenFavorites: () -> Unit,
    onSelectBoard: (ForumBoard) -> Unit,
    onToggleFavorite: (ForumBoard) -> Unit,
    onMoveFavorite: (String, Int) -> Unit,
    onSetBoardIcon: (ForumBoard, String?) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAiResult: () -> Unit,
    onOpenHistory: () -> Unit,
    onLogout: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var arrangingFavorites by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    var pendingFavoriteRemoval by remember { mutableStateOf<ForumBoard?>(null) }
    var editingIconBoard by remember { mutableStateOf<ForumBoard?>(null) }
    val normalizedQuery = query.trim()
    LaunchedEffect(normalizedQuery) { onSearch(normalizedQuery) }
    val categories = remember(state.forumCategories, normalizedQuery) {
        if (normalizedQuery.isBlank()) state.forumCategories
        else state.forumCategories.mapNotNull { category ->
            val groups = category.groups.mapNotNull { group ->
                val boards = group.boards.filter { board ->
                    board.name.contains(normalizedQuery, ignoreCase = true) ||
                        board.info.contains(normalizedQuery, ignoreCase = true) ||
                        board.fid.toString() == normalizedQuery || board.stid.toString() == normalizedQuery
                }
                group.takeIf { boards.isNotEmpty() }?.copy(boards = boards)
            }
            category.takeIf { groups.isNotEmpty() }?.copy(groups = groups)
        }
    }
    val allBoards = remember(state.forumCategories, state.forumSearchResults, state.favoriteBoardSnapshots) {
        (state.forumCategories.flatMap { category -> category.groups.flatMap { it.boards } } +
            state.forumSearchResults + state.favoriteBoardSnapshots)
            .distinctBy(ForumBoard::preferenceKey)
    }
    val boardsByKey = remember(allBoards) { allBoards.associateBy(ForumBoard::preferenceKey) }
    val favoriteBoards = remember(state.favoriteBoardKeys, boardsByKey) {
        state.favoriteBoardKeys.mapNotNull(boardsByKey::get)
    }
    val favoriteKeys = state.favoriteBoardKeys.toSet()
    val localResultKeys = remember(categories) {
        categories.flatMap { category -> category.groups.flatMap { it.boards } }
            .mapTo(mutableSetOf(), ForumBoard::preferenceKey)
    }
    val serverResults = remember(
        normalizedQuery,
        state.forumSearchQuery,
        state.forumSearchResults,
        localResultKeys,
    ) {
        if (normalizedQuery.isBlank() || state.forumSearchQuery != normalizedQuery) emptyList()
        else state.forumSearchResults.filterNot { it.preferenceKey() in localResultKeys }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    AiTaskStatusButton(state = state, onClick = onOpenAiResult)
                    if (favoriteBoards.isNotEmpty()) {
                        IconButton(onClick = { arrangingFavorites = !arrangingFavorites }) {
                            Icon(Icons.Default.SwapVert, contentDescription = "调整收藏顺序")
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "浏览设置")
                    }
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Default.History, contentDescription = "浏览历史")
                    }
                    IconButton(onClick = onOpenFavorites) {
                        Icon(Icons.Default.Star, contentDescription = "收藏主题")
                    }
                    IconButton(onClick = { confirmLogout = true }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "退出登录")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text("搜索版面名称或 fid") },
                singleLine = true,
            )
            Box(Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (normalizedQuery.isBlank() && favoriteBoards.isNotEmpty()) {
                        item(key = "favorites-header", span = { GridItemSpan(maxLineSpan) }) {
                            SectionHeader(if (arrangingFavorites) "⭐ 收藏版面 · 使用箭头调整顺序" else "⭐ 收藏版面")
                        }
                        gridItems(favoriteBoards, key = ForumBoard::preferenceKey) { board ->
                            BoardTile(
                                board = board,
                                iconSelection = state.boardIconSelections[board.preferenceKey()],
                                favorite = true,
                                arranging = arrangingFavorites,
                                onClick = { onSelectBoard(board) },
                                onToggleFavorite = { pendingFavoriteRemoval = board },
                                onMove = { offset -> onMoveFavorite(board.preferenceKey(), offset) },
                                onLongClick = { editingIconBoard = board },
                            )
                        }
                    }
                    categories.forEachIndexed { categoryIndex, category ->
                        val visibleGroups = category.groups.mapNotNull { group ->
                            val boards = if (normalizedQuery.isBlank()) {
                                group.boards.filterNot { it.preferenceKey() in favoriteKeys }
                            } else group.boards
                            group.takeIf { boards.isNotEmpty() }?.copy(boards = boards)
                        }
                        if (visibleGroups.isNotEmpty()) {
                            item(
                                key = boardCategoryGridKey(categoryIndex, category.id),
                                span = { GridItemSpan(maxLineSpan) },
                            ) {
                                SectionHeader(category.name)
                            }
                        }
                        visibleGroups.forEachIndexed { groupIndex, group ->
                            if (group.name.isNotBlank()) {
                                item(
                                    key = boardGroupGridKey(categoryIndex, groupIndex, category.id),
                                    span = { GridItemSpan(maxLineSpan) },
                                ) {
                                    Text(
                                        group.name,
                                        modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            group.boards.forEachIndexed { boardIndex, board ->
                                item(
                                    key = boardItemGridKey(
                                        categoryIndex = categoryIndex,
                                        groupIndex = groupIndex,
                                        boardIndex = boardIndex,
                                        board = board,
                                    ),
                                ) {
                                    BoardTile(
                                        board = board,
                                        iconSelection = state.boardIconSelections[board.preferenceKey()],
                                        favorite = board.preferenceKey() in favoriteKeys,
                                        arranging = false,
                                        onClick = { onSelectBoard(board) },
                                        onToggleFavorite = {
                                            if (board.preferenceKey() in favoriteKeys) pendingFavoriteRemoval = board
                                            else onToggleFavorite(board)
                                        },
                                        onMove = {},
                                        onLongClick = { editingIconBoard = board },
                                    )
                                }
                            }
                        }
                    }
                    if (normalizedQuery.isNotBlank() && serverResults.isNotEmpty()) {
                        item(key = "server-search-header", span = { GridItemSpan(maxLineSpan) }) {
                            SectionHeader("更多搜索结果")
                        }
                        gridItems(serverResults, key = { board -> "server:${board.preferenceKey()}" }) { board ->
                            BoardTile(
                                board = board,
                                iconSelection = state.boardIconSelections[board.preferenceKey()],
                                favorite = board.preferenceKey() in favoriteKeys,
                                arranging = false,
                                onClick = { onSelectBoard(board) },
                                onToggleFavorite = {
                                    if (board.preferenceKey() in favoriteKeys) pendingFavoriteRemoval = board
                                    else onToggleFavorite(board)
                                },
                                onMove = {},
                                onLongClick = { editingIconBoard = board },
                            )
                        }
                    }
                    if (normalizedQuery.isNotBlank() && state.forumSearchLoading) {
                        item(key = "server-search-loading", span = { GridItemSpan(maxLineSpan) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                Text(
                                    "正在搜索 NGA 完整版面…",
                                    modifier = Modifier.padding(start = 10.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    if (normalizedQuery.isNotBlank() && state.forumSearchError != null) {
                        item(key = "server-search-error", span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                "服务器搜索失败，已保留本地结果：${state.forumSearchError}",
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
                if (state.loading) CircularProgressIndicator(Modifier.align(Alignment.Center))
                if (!state.loading && !state.forumSearchLoading && categories.isEmpty() && serverResults.isEmpty()) {
                    Text(
                        if (query.isBlank()) "没有取得版面列表" else "没有匹配的版面",
                        Modifier.align(Alignment.Center),
                    )
                }
            }
        }
    }
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("退出登录？") },
            text = { Text("确定退出当前账号吗？本地收藏、浏览历史和设置不会删除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmLogout = false
                        onLogout()
                    },
                ) { Text("退出登录", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("取消") } },
        )
    }
    pendingFavoriteRemoval?.let { board ->
        AlertDialog(
            onDismissRequest = { pendingFavoriteRemoval = null },
            title = { Text("取消收藏版面？") },
            text = { Text("确定从首页收藏中移除“${board.name}”吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingFavoriteRemoval = null
                        onToggleFavorite(board)
                    },
                ) { Text("取消收藏", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingFavoriteRemoval = null }) { Text("保留") }
            },
        )
    }
    editingIconBoard?.let { board ->
        BoardIconPickerDialog(
            board = board,
            currentSelection = state.boardIconSelections[board.preferenceKey()],
            onSave = { selection ->
                onSetBoardIcon(board, selection)
                editingIconBoard = null
            },
            onDismiss = { editingIconBoard = null },
        )
    }
}

internal fun boardCategoryGridKey(categoryIndex: Int, categoryId: String): String =
    "category:$categoryIndex:$categoryId"

internal fun boardGroupGridKey(categoryIndex: Int, groupIndex: Int, categoryId: String): String =
    "group:$categoryIndex:$groupIndex:$categoryId"

internal fun boardItemGridKey(
    categoryIndex: Int,
    groupIndex: Int,
    boardIndex: Int,
    board: ForumBoard,
): String = "board:$categoryIndex:$groupIndex:$boardIndex:${board.preferenceKey()}"

@Composable
internal fun ImageDiagnosticDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var revision by remember { mutableIntStateOf(0) }
    val events = remember(revision) { ImageDiagnosticStore.events(context) }
    val report = remember(revision) { ImageDiagnosticStore.report(context) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("图片加载与查看诊断") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "共 ${events.size} 条记录。报告不包含 Cookie、UID、CID、完整查询参数或图片内容。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SelectionContainer {
                    Text(
                        report,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 430.dp)
                            .verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (events.isNotEmpty()) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            ImageDiagnosticStore.clear(context)
                            revision++
                        },
                    ) { Text("清空诊断记录") }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("LNGA 图片加载与查看诊断", report))
                    Toast.makeText(context, "脱敏诊断已复制", Toast.LENGTH_SHORT).show()
                },
            ) { Text("复制报告") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

@Composable
internal fun ThemeColorDialog(
    selected: ThemeColorOption,
    onSelect: (ThemeColorOption) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("主题色") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ThemeColorOption.entries.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { option ->
                            Column(
                                modifier = Modifier.width(72.dp).clickable { onSelect(option) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Surface(
                                    modifier = Modifier.size(44.dp),
                                    shape = CircleShape,
                                    color = option.lightPrimary,
                                    tonalElevation = if (option == selected) 8.dp else 0.dp,
                                ) {
                                    if (option == selected) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(option.displayName, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun BoardIconPickerDialog(
    board: ForumBoard,
    currentSelection: String?,
    onSave: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember(board.preferenceKey(), currentSelection) { mutableStateOf(currentSelection) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置“${board.name}”图标") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BoardIcon(board = board, selection = selected, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(board.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            when {
                                selected == null -> "自动匹配"
                                selected == BOARD_ICON_OFFICIAL -> "使用官方图标"
                                else -> storedBoardIconPreset(selected)?.displayName ?: "自动匹配"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { selected = null }, modifier = Modifier.weight(1f)) {
                        Text("自动匹配")
                    }
                    OutlinedButton(
                        onClick = { selected = BOARD_ICON_OFFICIAL },
                        enabled = board.iconUrl.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    ) { Text("官方图标") }
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    BoardIconCategory.entries.forEach { category ->
                        item(key = "icon-category-${category.name}", span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                category.displayName,
                                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        gridItems(
                            BoardIconPreset.entries.filter { it.category == category },
                            key = BoardIconPreset::name,
                        ) { preset ->
                            val stored = preset.storedValue()
                            val isSelected = selected == stored
                            Column(
                                modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                    .then(
                                        if (isSelected) Modifier.border(
                                            2.dp,
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(10.dp),
                                        ) else Modifier,
                                    )
                                    .clickable { selected = stored }
                                    .padding(vertical = 7.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    preset.vector(),
                                    contentDescription = preset.displayName,
                                    modifier = Modifier.size(25.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(preset.displayName, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(selected) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun BoardTile(
    board: ForumBoard,
    iconSelection: String?,
    favorite: Boolean,
    arranging: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMove: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().height(if (arranging) 126.dp else 102.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                BoardIcon(board, iconSelection)
                Spacer(Modifier.height(5.dp))
                Text(
                    board.name,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                if (arranging) {
                    Row {
                        IconButton(onClick = { onMove(-1) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "前移", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { onMove(1) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "后移", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.align(Alignment.TopEnd).size(28.dp),
            ) {
                Icon(
                    if (favorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (favorite) "取消收藏" else "收藏版面",
                    modifier = Modifier.size(17.dp),
                    tint = if (favorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BoardIcon(
    board: ForumBoard,
    selection: String?,
    modifier: Modifier = Modifier.size(42.dp),
) {
    val selectedPreset = storedBoardIconPreset(selection)
    val useOfficial = selectedPreset == null && board.iconUrl.isNotBlank() &&
        (selection == null || selection == BOARD_ICON_OFFICIAL)
    var failed by remember(board.iconUrl, selection) { mutableStateOf(false) }
    var loaded by remember(board.iconUrl, selection) { mutableStateOf(false) }
    val fallbackPreset = selectedPreset ?: automaticBoardIconPreset(board.name)
    Box(
        modifier = modifier.clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (!loaded || !useOfficial) {
            Icon(
                fallbackPreset.vector(),
                contentDescription = "${board.name}图标",
                modifier = Modifier.fillMaxSize().padding(8.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        if (useOfficial && !failed) {
            AsyncImage(
                model = board.iconUrl,
                contentDescription = "${board.name}图标",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onSuccess = { loaded = true },
                onError = { failed = true },
            )
        }
    }
}

@Composable
private fun LoginScreen(
    loading: Boolean,
    onSubmit: (String, String) -> Unit,
    onWebLogin: () -> Unit,
) {
    var uid by remember { mutableStateOf("") }
    var cid by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("LNGA Droid") }) }) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(24.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("开发预览", style = MaterialTheme.typography.headlineSmall)
            Text(
                "推荐直接使用 NGA 网页登录。登录成功后，App 会自动读取并验证会话凭据，再使用 Android Keystore 加密保存在本机。",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = onWebLogin,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("NGA 网页登录")
            }
            Text(
                "开发者凭据导入",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = uid,
                onValueChange = { uid = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("ngaPassportUid") },
                singleLine = true,
            )
            OutlinedTextField(
                value = cid,
                onValueChange = { cid = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("ngaPassportCid") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
            )
            OutlinedButton(
                onClick = { onSubmit(uid, cid) },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("使用 UID / CID")
            }
            Text(
                "可填写纯值，也可直接粘贴包含 ngaPassportUid/ngaPassportCid 的整段 Cookie。请勿在截图、日志或问题反馈中公开这些值。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HistoryScreen(
    entries: List<BrowseHistoryEntry>,
    onOpen: (BrowseHistoryEntry) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("浏览历史") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回首页")
                    }
                },
                actions = {
                    if (entries.isNotEmpty()) {
                        IconButton(onClick = onClear) {
                            Icon(Icons.Default.Delete, contentDescription = "清空浏览历史")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize()) {
                items(entries, key = BrowseHistoryEntry::tid) { entry ->
                    Surface(onClick = { onOpen(entry) }) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                            Text(entry.subject, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(5.dp))
                            Text(
                                listOf(entry.boardName, entry.author, formatDate(entry.visitedAtMillis / 1000))
                                    .filter(String::isNotBlank)
                                    .joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
            if (entries.isEmpty()) Text("还没有浏览记录", Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun FavoriteTopicsScreen(
    entries: List<TopicFavoriteEntry>,
    onOpen: (TopicFavoriteEntry) -> Unit,
    onDelete: (TopicFavoriteEntry) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("收藏主题") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回首页")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize()) {
                items(entries, key = TopicFavoriteEntry::tid) { entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onOpen(entry) }
                            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.subject, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(5.dp))
                            Text(
                                listOf(entry.boardName, entry.author, formatDate(entry.savedAtMillis / 1000))
                                    .filter(String::isNotBlank)
                                    .joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onDelete(entry) }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除收藏")
                        }
                    }
                    HorizontalDivider()
                }
            }
            if (entries.isEmpty()) Text("还没有收藏主题", Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun TopicListScreen(
    state: MainUiState,
    onLoad: () -> Unit,
    onLoadNext: () -> Unit,
    onSortMode: (TopicSortMode) -> Unit,
    onHotRange: (TopicHotRange) -> Unit,
    onScrollResetApplied: () -> Unit,
    onOpenTopic: (Topic) -> Unit,
    onLoadPreview: (Topic) -> Unit,
    onOpenAiResult: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val selectedBoard = remember(state.forumCategories, state.fid, state.stid) {
        val fid = state.fid.toIntOrNull()
        val stid = state.stid.toIntOrNull()
        state.forumCategories.asSequence()
            .flatMap { it.groups.asSequence() }
            .flatMap { it.boards.asSequence() }
            .firstOrNull { board ->
                fid != null && board.fid == fid || stid != null && board.stid == stid
            }
    }
    val boardsByFid = remember(state.forumCategories) {
        state.forumCategories.asSequence()
            .flatMap { it.groups.asSequence() }
            .flatMap { it.boards.asSequence() }
            .filter { it.fid != 0 }
            .associateBy { it.fid.toLong() }
    }
    val boardsByName = remember(state.forumCategories) {
        state.forumCategories.asSequence()
            .flatMap { it.groups.asSequence() }
            .flatMap { it.boards.asSequence() }
            .associateBy { it.name.trim() }
    }
    val selectedIndex = state.selectedTopic?.let { selected ->
        state.topics.indexOfFirst { it.tid == selected.tid }.takeIf { it >= 0 }?.plus(1) ?: 0
    } ?: 0
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val scrollScope = rememberCoroutineScope()
    var userHasScrolled by remember { mutableStateOf(false) }
    var showSortBar by remember(state.fid, state.stid, selectedIndex) { mutableStateOf(selectedIndex == 0) }
    val inlineSortBarVisible by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 }
    }
    val atEnd by remember {
        derivedStateOf { listState.layoutInfo.totalItemsCount > 0 && !listState.canScrollForward }
    }
    val scrollProgress by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            val total = layout.totalItemsCount
            val visible = layout.visibleItemsInfo.size
            if (total <= visible || visible == 0) 0f else {
                val first = layout.visibleItemsInfo.first()
                val fractionalOffset = if (first.size > 0) {
                    (-first.offset).coerceAtLeast(0).toFloat() / first.size
                } else 0f
                (first.index + fractionalOffset) / (total - visible).coerceAtLeast(1)
            }
        }
    }
    val showScrollProgress by remember {
        derivedStateOf { listState.canScrollBackward || listState.canScrollForward }
    }
    LaunchedEffect(listState) {
        var previous = TopicListScrollPosition(
            index = listState.firstVisibleItemIndex,
            offset = listState.firstVisibleItemScrollOffset,
        )
        snapshotFlow {
            TopicListScrollSnapshot(
                position = TopicListScrollPosition(
                    index = listState.firstVisibleItemIndex,
                    offset = listState.firstVisibleItemScrollOffset,
                ),
                scrolling = listState.isScrollInProgress,
            )
        }.collect { snapshot ->
            if (snapshot.scrolling) {
                userHasScrolled = true
                showSortBar = topicSortBarVisibleAfterScroll(
                    previous = previous,
                    current = snapshot.position,
                    currentlyVisible = showSortBar,
                )
            }
            previous = snapshot.position
        }
    }
    LaunchedEffect(state.pendingTopicScrollReset) {
        if (state.pendingTopicScrollReset) {
            showSortBar = true
            listState.scrollToItem(0)
            userHasScrolled = false
            onScrollResetApplied()
        }
    }
    LaunchedEffect(atEnd, state.topicPage, state.topicTotalPages, state.loading) {
        if (userHasScrolled && atEnd && state.topicPage < state.topicTotalPages && !state.loading) {
            delay(150)
            if (!listState.canScrollForward) {
                userHasScrolled = false
                onLoadNext()
            }
        }
    }
    Scaffold(
        modifier = Modifier.edgeSwipeBack(onBack),
        topBar = {
            TopAppBar(
                modifier = Modifier.clickable { scrollScope.launch { listState.animateScrollToItem(0) } },
                title = { Text(state.forumName.ifBlank { "主题列表" }) },
                navigationIcon = selectedBoard?.let { board ->
                    {
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            BoardIcon(
                                board,
                                state.boardIconSelections[board.preferenceKey()],
                                Modifier.size(34.dp),
                            )
                        }
                    }
                } ?: {},
                actions = {
                    AiTaskStatusButton(state = state, onClick = onOpenAiResult)
                    IconButton(onClick = onLoad) { Icon(Icons.Default.Refresh, contentDescription = "刷新") }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                item(key = "topic-sort-header") {
                    TopicSortBar(
                        selectedMode = state.topicSortMode,
                        selectedHotRange = state.topicHotRange,
                        onModeSelected = onSortMode,
                        onHotRangeSelected = onHotRange,
                    )
                }
                items(state.topics, key = Topic::tid) { topic ->
                    val topicBoard = boardsByFid[topic.fid] ?: boardsByName[topic.boardName.trim()]
                    TopicRow(
                        topic = topic,
                        useBoardIcon = state.topicSubBoardIcons,
                        boardIconSelection = state.boardIconSelections[
                            topicBoard?.preferenceKey() ?: "fid:${topic.fid}",
                        ],
                        boardOfficialIconUrl = topicBoard?.iconUrl.orEmpty(),
                        revision = state.topicRevision,
                        viewed = topic.tid in state.viewedTopicIds,
                        onLoadPreview = { onLoadPreview(topic) },
                        onClick = { onOpenTopic(topic) },
                    )
                    HorizontalDivider()
                }
                if (state.loading && state.topics.isNotEmpty()) {
                    item(key = "loading-more") {
                        Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(24.dp))
                        }
                    }
                }
            }
            if (state.loading && state.topics.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    state.loadingMessage.takeIf(String::isNotBlank)?.let { message ->
                        Spacer(Modifier.height(12.dp))
                        Text(
                            message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (!state.loading && state.topics.isEmpty()) {
                Text("暂无主题，或尚未加载", Modifier.align(Alignment.Center))
            }
            if (showScrollProgress) {
                TopicScrollProgress(
                    progress = scrollProgress,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(vertical = 8.dp, horizontal = 2.dp),
                )
            }
            AnimatedVisibility(
                visible = floatingTopicSortBarVisible(showSortBar, inlineSortBarVisible),
                modifier = Modifier.align(Alignment.TopCenter),
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 }),
            ) {
                TopicSortBar(
                    selectedMode = state.topicSortMode,
                    selectedHotRange = state.topicHotRange,
                    onModeSelected = onSortMode,
                    onHotRangeSelected = onHotRange,
                )
            }
        }
    }
}

internal data class TopicListScrollPosition(
    val index: Int,
    val offset: Int,
)

private data class TopicListScrollSnapshot(
    val position: TopicListScrollPosition,
    val scrolling: Boolean,
)

internal fun topicSortBarVisibleAfterScroll(
    previous: TopicListScrollPosition,
    current: TopicListScrollPosition,
    currentlyVisible: Boolean,
): Boolean = when {
    current.index == 0 && current.offset == 0 -> true
    current.index != previous.index -> current.index < previous.index
    current.offset != previous.offset -> current.offset < previous.offset
    else -> currentlyVisible
}

internal fun floatingTopicSortBarVisible(
    requestedByScrollDirection: Boolean,
    inlineSortBarVisible: Boolean,
): Boolean = requestedByScrollDirection && !inlineSortBarVisible

@Composable
private fun TopicSortBar(
    selectedMode: TopicSortMode,
    selectedHotRange: TopicHotRange,
    onModeSelected: (TopicSortMode) -> Unit,
    onHotRangeSelected: (TopicHotRange) -> Unit,
) {
    Surface(tonalElevation = 1.dp) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth()) {
                TopicSortTab("新回复", selectedMode == TopicSortMode.NewReply) {
                    onModeSelected(TopicSortMode.NewReply)
                }
                TopicSortTab("新发布", selectedMode == TopicSortMode.NewPost) {
                    onModeSelected(TopicSortMode.NewPost)
                }
                TopicSortTab("热帖", selectedMode == TopicSortMode.Hot) {
                    onModeSelected(TopicSortMode.Hot)
                }
            }
            if (selectedMode == TopicSortMode.Hot) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TopicHotRange.entries.forEach { range ->
                        TopicHotRangeChip(
                            label = when (range) {
                                TopicHotRange.Day -> "24小时"
                                TopicHotRange.Week -> "本周"
                                TopicHotRange.Month -> "本月"
                            },
                            selected = selectedHotRange == range,
                            onClick = { onHotRangeSelected(range) },
                        )
                    }
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TopicSortTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.weight(1f).clickable(onClick = onClick).padding(top = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(7.dp))
        Box(
            Modifier.width(30.dp).height(3.dp).clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent),
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TopicHotRangeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.weight(1f).clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun TopicScrollProgress(progress: Float, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.width(3.dp).fillMaxHeight()) {
        val thumbHeight = 34.dp
        val travel = (maxHeight - thumbHeight).coerceAtLeast(0.dp)
        Box(
            Modifier.fillMaxWidth().height(thumbHeight)
                .offset(y = travel * progress.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.78f)),
        )
    }
}

@Composable
private fun TopicRow(
    topic: Topic,
    useBoardIcon: Boolean,
    boardIconSelection: String?,
    boardOfficialIconUrl: String,
    revision: Int,
    viewed: Boolean,
    onLoadPreview: () -> Unit,
    onClick: () -> Unit,
) {
    val automaticImages = LocalAutomaticImageLoading.current
    LaunchedEffect(topic.tid, revision, automaticImages) {
        if (automaticImages) onLoadPreview()
    }
    Surface(onClick = onClick) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth()) {
            val titleColor = topicTitleColor(topic.titleFont)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val boardIconKey = "topic-board-icon"
                val titleText = buildAnnotatedString {
                        if (topic.boardName.isNotBlank()) {
                            if (useBoardIcon) {
                                appendInlineContent(boardIconKey, "[${topic.boardName}]")
                                append(" ")
                            } else {
                                withStyle(
                                    SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)),
                                ) {
                                    append(topicBoardTextPrefix(topic.boardName, useBoardIcon))
                                }
                            }
                        }
                        if (topic.type and 1024 != 0) {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.outline)) { append("[锁定]") }
                        }
                        withStyle(
                            SpanStyle(
                                color = titleColor,
                                fontWeight = if (topic.titleFont and 32 != 0) FontWeight.Bold else FontWeight.Normal,
                                fontStyle = if (topic.titleFont and 64 != 0) FontStyle.Italic else FontStyle.Normal,
                            ),
                        ) { append(topic.subject) }
                    }
                BasicText(
                    text = titleText,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    inlineContent = if (!useBoardIcon || topic.boardName.isBlank()) emptyMap() else mapOf(
                        boardIconKey to InlineTextContent(
                            Placeholder(
                                width = 1.15.em,
                                height = 1.15.em,
                                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                            ),
                        ) {
                            CompactBoardIcon(
                                boardName = topic.boardName,
                                officialIconUrl = boardOfficialIconUrl,
                                selection = boardIconSelection,
                            )
                        },
                    ),
                )
                if (viewed) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = "已浏览",
                        modifier = Modifier.padding(top = 2.dp).size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f),
                    )
                }
            }
            if (topic.previewImages.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                TopicImagePreviews(topic.previewImages)
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${topic.author} · ${formatCompactPostDate(topic.postDateSeconds)}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = replyCountColor(
                                    replies = topic.replies,
                                    neutral = MaterialTheme.colorScheme.onSurfaceVariant,
                                    accent = MaterialTheme.colorScheme.error,
                                ),
                            ),
                        ) {
                            append(topic.replies.toString())
                        }
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            append("回复 · ${formatTopicUpdateTime(topic.lastPostDateSeconds)}")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

internal fun topicBoardTextPrefix(boardName: String, useBoardIcon: Boolean): String =
    if (useBoardIcon || boardName.isBlank()) "" else "[$boardName] "

@Composable
private fun CompactBoardIcon(
    boardName: String,
    officialIconUrl: String,
    selection: String?,
) {
    val selectedPreset = storedBoardIconPreset(selection)
    val useOfficial = selectedPreset == null && officialIconUrl.isNotBlank() &&
        (selection == null || selection == BOARD_ICON_OFFICIAL)
    var officialFailed by remember(officialIconUrl, selection) { mutableStateOf(false) }
    val preset = selectedPreset ?: automaticBoardIconPreset(boardName)
    Box(
        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            preset.vector(),
            contentDescription = "子版块：$boardName",
            modifier = Modifier.fillMaxSize().padding(2.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        if (useOfficial && !officialFailed) {
            AsyncImage(
                model = officialIconUrl,
                contentDescription = "子版块：$boardName",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = { officialFailed = true },
            )
        }
    }
}

@Composable
private fun TopicImagePreviews(images: List<String>) {
    if (!LocalAutomaticImageLoading.current) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        images.take(3).forEach { url ->
            AsyncImage(
                model = rememberNgaImageRequest(url, "topic-preview"),
                contentDescription = "主题图片概览",
                modifier = (if (images.size == 1) Modifier.fillMaxWidth(0.78f) else Modifier.weight(1f))
                    .height(118.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

internal data class ThreadScrollTarget(val index: Int, val offset: Int)

internal fun threadScrollTarget(
    pendingScrollIndex: Int?,
    pendingScrollOffset: Int,
    pendingResumeFloor: Int?,
    posts: List<Post>,
    hasOpeningPost: Boolean,
): ThreadScrollTarget {
    val openingOffset = if (hasOpeningPost) 1 else 0
    val itemCount = posts.size + openingOffset
    if (itemCount <= 0) return ThreadScrollTarget(0, 0)
    pendingScrollIndex?.let { index ->
        return ThreadScrollTarget(
            index = index.coerceIn(0, itemCount - 1),
            offset = pendingScrollOffset.coerceAtLeast(0),
        )
    }
    val index = pendingResumeFloor?.let { targetFloor ->
        if (targetFloor <= 0 && hasOpeningPost) 0
        else posts.indices.minByOrNull { postIndex -> abs(posts[postIndex].floor - targetFloor) }
            ?.plus(openingOffset)
            ?: 0
    } ?: 0
    return ThreadScrollTarget(index, 0)
}

@Composable
private fun ThreadScreen(
    state: MainUiState,
    onBack: () -> Unit,
    onPage: (Int) -> Unit,
    autoNextPage: Boolean,
    doubleSwipeNextPage: Boolean,
    doubleSwipeIntervalMillis: Int,
    doubleSwipeDistanceDp: Int,
    pageControlPosition: PageControlPosition,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onLoadIpLocation: (Post) -> Unit,
    onOpenUser: (Post, Int, Int) -> Unit,
    onSummarize: (Boolean) -> Unit,
    onOpenSummary: () -> Unit,
    onReadingPosition: (Int, Int) -> Unit,
    onResumeApplied: () -> Unit,
    onScrollRestoreApplied: () -> Unit,
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val saveReadingPosition = {
        if (!state.loading) {
            val index = listState.firstVisibleItemIndex
            val hasOpening = state.threadPage == 1 && state.openingPost != null
            val floor = when {
                hasOpening && index == 0 -> 0
                else -> state.posts.getOrNull(index - if (hasOpening) 1 else 0)?.floor
                    ?: state.posts.firstOrNull()?.floor
                    ?: 0
            }
            onReadingPosition(state.threadPage, floor)
        }
    }
    val saveAndBack = { saveReadingPosition(); onBack() }
    BackHandler(onBack = saveAndBack)
    val scrollScope = rememberCoroutineScope()
    val pageControlSide = rememberPageControlSide(pageControlPosition)
    var showNextPrompt by remember(state.threadPage) { mutableStateOf(false) }
    var showPageControl by remember(state.threadPage) { mutableStateOf(true) }
    var selectionResetKey by remember(state.threadPage) { mutableIntStateOf(0) }
    var selectionActive by remember(state.threadPage) { mutableStateOf(false) }
    var renderingReplies by remember(state.selectedTopic?.tid, state.threadPage) { mutableStateOf(false) }
    var bilibiliLoads by remember(state.selectedTopic?.tid, state.threadPage) { mutableIntStateOf(0) }
    var showSummaryOptions by remember(state.selectedTopic?.tid) { mutableStateOf(false) }
    var showTitleMenu by remember(state.selectedTopic?.tid) { mutableStateOf(false) }
    val hasCurrentSummary = state.selectedTopic?.tid?.let { tid ->
        state.aiConversations.any { it.kind == AiConversationKind.TopicSummary && it.sourceId == tid }
    } == true
    val analyzedUserIds = remember(state.aiConversations) {
        state.aiConversations.asSequence()
            .filter { it.kind == AiConversationKind.UserAnalysis && it.completedAssistantText() != null }
            .map { it.sourceId }
            .filter { it > 0 }
            .toSet()
    }
    val onBilibiliLoadingChanged = remember(state.selectedTopic?.tid, state.threadPage) {
        { loading: Boolean ->
            bilibiliLoads = (bilibiliLoads + if (loading) 1 else -1).coerceAtLeast(0)
        }
    }
    val atEnd by remember {
        derivedStateOf { listState.layoutInfo.totalItemsCount > 0 && !listState.canScrollForward }
    }
    LaunchedEffect(state.selectedTopic?.tid, state.threadPage, state.loading) {
        if (!state.loading) {
            showPageControl = true
            val target = threadScrollTarget(
                pendingScrollIndex = state.pendingThreadScrollIndex,
                pendingScrollOffset = state.pendingThreadScrollOffset,
                pendingResumeFloor = state.pendingResumeFloor,
                posts = state.posts,
                hasOpeningPost = state.threadPage == 1 && state.openingPost != null,
            )
            listState.requestScrollToItem(target.index, target.offset)
            delay(16)
            listState.scrollToItem(target.index, target.offset)
            if (state.pendingThreadScrollIndex != null) onScrollRestoreApplied()
            if (state.pendingResumeFloor != null) onResumeApplied()
        }
    }
    LaunchedEffect(state.loading, state.posts, state.openingPost) {
        if (!state.loading) {
            renderingReplies = true
            withFrameNanos { }
            withFrameNanos { }
            renderingReplies = false
        }
    }
    LaunchedEffect(listState, state.loading) {
        var previousIndex = listState.firstVisibleItemIndex
        var previousOffset = listState.firstVisibleItemScrollOffset
        snapshotFlow {
            Triple(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
                listState.layoutInfo.totalItemsCount > 0 && !listState.canScrollForward,
            )
        }.collect { (index, offset, reachedEnd) ->
                if (!state.loading) {
                    if (reachedEnd) {
                        showPageControl = true
                    } else if (index > previousIndex || (index == previousIndex && offset > previousOffset)) {
                        showPageControl = false
                    } else if (index < previousIndex || (index == previousIndex && offset < previousOffset)) {
                        showPageControl = true
                    }
                }
                previousIndex = index
                previousOffset = offset
        }
    }
    LaunchedEffect(listState, state.selectedTopic?.tid, state.threadPage, state.posts, state.openingPost, state.loading) {
        snapshotFlow { listState.isScrollInProgress to listState.firstVisibleItemIndex }
            .collectLatest { (scrolling, index) ->
                if (scrolling || state.loading) return@collectLatest
                delay(350)
                if (listState.isScrollInProgress) return@collectLatest
                val hasOpening = state.threadPage == 1 && state.openingPost != null
                val floor = when {
                    hasOpening && index == 0 -> 0
                    else -> state.posts.getOrNull(index - if (hasOpening) 1 else 0)?.floor
                        ?: state.posts.firstOrNull()?.floor
                        ?: 0
                }
                onReadingPosition(state.threadPage, floor)
            }
    }
    LaunchedEffect(atEnd, state.threadPage, state.threadTotalPages, state.loading) {
        showNextPrompt = false
        if ((autoNextPage || doubleSwipeNextPage) && atEnd &&
            state.threadPage < state.threadTotalPages && !state.loading
        ) {
            delay(200)
            if (!listState.canScrollForward) showNextPrompt = true
        }
    }
    Box(Modifier.fillMaxSize()) {
    CompositionLocalProvider(
        LocalSelectionGeneration provides selectionResetKey,
        LocalSelectionActivated provides { selectionActive = true },
        LocalBilibiliLoadingChanged provides onBilibiliLoadingChanged,
    ) {
    Scaffold(
        modifier = Modifier.edgeSwipeBack(saveAndBack),
        topBar = {
            TopAppBar(
                title = {
                    Box {
                        Text(
                            state.threadTitle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { scrollScope.launch { listState.animateScrollToItem(0) } },
                                    onLongClick = { showTitleMenu = true },
                                )
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    repeatDelayMillis = 1_200,
                                    initialDelayMillis = 1_500,
                                    spacing = MarqueeSpacing(32.dp),
                                    velocity = 18.dp,
                                ),
                            maxLines = 1,
                            softWrap = false,
                        )
                        DropdownMenu(expanded = showTitleMenu, onDismissRequest = { showTitleMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("复制完整标题") },
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("主题标题", state.threadTitle))
                                    showTitleMenu = false
                                    Toast.makeText(context, "标题已复制", Toast.LENGTH_SHORT).show()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(if (isFavorite) "取消收藏" else "收藏主题") },
                                leadingIcon = {
                                    Icon(if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder, null)
                                },
                                onClick = {
                                    onToggleFavorite()
                                    showTitleMenu = false
                                },
                            )
                        }
                    }
                },
                actions = {
                    state.selectedTopic?.tid?.let { tid ->
                        val taskMatches = state.aiTaskKind == AiConversationKind.TopicSummary &&
                            state.aiTaskSourceId == tid && state.aiTaskPhase != AiTaskPhase.Idle
                        val taskRunning = taskMatches &&
                            (state.aiTaskPhase == AiTaskPhase.Preparing || state.aiTaskPhase == AiTaskPhase.Streaming)
                        AiTaskStatusButton(
                            state = state,
                            kind = AiConversationKind.TopicSummary,
                            sourceId = tid,
                            onClick = {
                                if (hasCurrentSummary || taskRunning) onOpenSummary()
                                else showSummaryOptions = true
                            },
                            onLongClick = if (hasCurrentSummary && !taskRunning) {
                                { showSummaryOptions = true }
                            } else null,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (!state.loading && showPageControl) {
                PageFloatingControl(state.threadPage, state.threadTotalPages, false, onPage)
            }
        },
        floatingActionButtonPosition = if (pageControlSide == PageControlSide.Left) FabPosition.Start else FabPosition.End,
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (state.loading) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator()
                    Text(
                        state.loadingMessage.ifBlank { "正在加载回复…" },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                        modifier = Modifier.fillMaxSize().pullAndHoldForNextPage(
                            enabled = showNextPrompt && autoNextPage,
                        ) {
                            showNextPrompt = false
                            onPage(state.threadPage + 1)
                        }.doubleSwipeForNextPage(
                            enabled = showNextPrompt && doubleSwipeNextPage,
                            maximumIntervalMillis = doubleSwipeIntervalMillis.toLong(),
                            minimumDistanceDp = doubleSwipeDistanceDp,
                        ) {
                            showNextPrompt = false
                            onPage(state.threadPage + 1)
                        },
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 6.dp, bottom = 88.dp),
                    ) {
                        state.openingPost?.takeIf { state.threadPage == 1 }?.let { opening ->
                            item(key = "opening-${opening.tid}") {
                                PostCard(
                                    opening,
                                    isTopic = true,
                                    ipLocation = state.postIpLocations[opening.authorId].orEmpty(),
                                    onLoadIpLocation = onLoadIpLocation,
                                    onOpenUser = { post ->
                                        saveReadingPosition()
                                        onOpenUser(
                                            post,
                                            listState.firstVisibleItemIndex,
                                            listState.firstVisibleItemScrollOffset,
                                        )
                                    },
                                    analyzedUserIds = analyzedUserIds,
                                )
                            }
                        }
                        items(state.posts, key = Post::pid) { post ->
                            PostCard(
                                post,
                                ipLocation = state.postIpLocations[post.authorId].orEmpty(),
                                onLoadIpLocation = onLoadIpLocation,
                                onOpenUser = { selectedPost ->
                                    saveReadingPosition()
                                    onOpenUser(
                                        selectedPost,
                                        listState.firstVisibleItemIndex,
                                        listState.firstVisibleItemScrollOffset,
                                    )
                                },
                                analyzedUserIds = analyzedUserIds,
                            )
                        }
                    }
                if (state.posts.isEmpty() && !(state.threadPage == 1 && state.openingPost != null)) {
                    Text("本页没有可显示的楼层", Modifier.align(Alignment.Center))
                }
                if (showNextPrompt) {
                    NextPagePrompt(
                        autoNextPage = autoNextPage,
                        doubleSwipeNextPage = doubleSwipeNextPage,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
                if (renderingReplies) {
                    LoadingStatusPill(
                        text = "正在渲染回复…",
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
                    )
                } else if (bilibiliLoads > 0) {
                    LoadingStatusPill(
                        text = "正在加载 B站视频信息${if (bilibiliLoads > 1) "（$bilibiliLoads）" else ""}…",
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
                    )
                }
            }
        }
    }
    }
    if (selectionActive) {
        Box(
            Modifier.fillMaxSize().clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                selectionActive = false
                selectionResetKey++
            },
        )
    }
    if (showSummaryOptions) {
        AlertDialog(
            onDismissRequest = { showSummaryOptions = false },
            title = { Text(if (hasCurrentSummary) "重新生成 AI 总结" else "AI 总结范围") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (hasCurrentSummary) {
                        Text(
                            "开始生成新摘要时会替换当前主题之前的摘要会话。",
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text("默认只发送当前第 ${state.threadPage} 页已经加载的楼层。")
                    if (state.threadTotalPages > 1) {
                        Text(
                            "总结整个主题会读取第 1–${state.threadTotalPages} 页，最多额外产生 ${state.threadTotalPages - 1} 次 NGA 网络请求。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showSummaryOptions = false; onSummarize(false) }) { Text("总结当前页") }
            },
            dismissButton = {
                Row {
                    if (state.threadTotalPages > 1) {
                        TextButton(onClick = { showSummaryOptions = false; onSummarize(true) }) { Text("总结整个主题") }
                    }
                    TextButton(onClick = { showSummaryOptions = false }) { Text("取消") }
                }
            },
        )
    }
    }
}

@Composable
private fun LoadingStatusPill(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun NextPagePrompt(
    autoNextPage: Boolean,
    doubleSwipeNextPage: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.padding(bottom = 76.dp).width(150.dp),
        shape = RoundedCornerShape(9.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
        tonalElevation = 2.dp,
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(3.dp).height(24.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.primary))
            Text(
                when {
                    autoNextPage && doubleSwipeNextPage -> "上拉停留或连续上滑\n进入下一页"
                    doubleSwipeNextPage -> "快速连续上滑两次\n进入下一页"
                    else -> "继续上拉并停留\n进入下一页"
                },
                modifier = Modifier.padding(start = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
            )
        }
    }
}

private fun Modifier.pullAndHoldForNextPage(enabled: Boolean, onNext: () -> Unit): Modifier =
    pointerInput(enabled) {
        if (!enabled) return@pointerInput
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var upwardDistance = 0f
            var event = awaitPointerEvent(PointerEventPass.Final)
            while (event.changes.any { it.pressed }) {
                event.changes.forEach { change ->
                    val delta = change.position.y - change.previousPosition.y
                    if (delta < 0f) upwardDistance -= delta
                    else upwardDistance = (upwardDistance - delta).coerceAtLeast(0f)
                }
                if (upwardDistance >= 48.dp.toPx()) {
                    val releasedBeforeHold = withTimeoutOrNull(550L) {
                        var holdEvent = awaitPointerEvent(PointerEventPass.Final)
                        while (holdEvent.changes.any { it.pressed }) {
                            holdEvent = awaitPointerEvent(PointerEventPass.Final)
                        }
                        true
                    }
                    if (releasedBeforeHold == null) onNext()
                    break
                }
                event = awaitPointerEvent(PointerEventPass.Final)
            }
        }
    }

private fun Modifier.doubleSwipeForNextPage(
    enabled: Boolean,
    maximumIntervalMillis: Long,
    minimumDistanceDp: Int,
    onNext: () -> Unit,
): Modifier =
    pointerInput(enabled, maximumIntervalMillis, minimumDistanceDp) {
        if (!enabled) return@pointerInput
        var previousSwipeTime = 0L
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var upwardDistance = 0f
            var releaseTime = down.uptimeMillis
            var event = awaitPointerEvent(PointerEventPass.Final)
            while (event.changes.any { it.pressed }) {
                event.changes.forEach { change ->
                    val delta = change.position.y - change.previousPosition.y
                    if (delta < 0f) upwardDistance -= delta
                }
                event = awaitPointerEvent(PointerEventPass.Final)
            }
            event.changes.firstOrNull()?.let {
                releaseTime = it.uptimeMillis
            }
            val isQuickSwipe = isQuickUpwardSwipe(
                distancePx = upwardDistance,
                durationMillis = releaseTime - down.uptimeMillis,
                minimumDistancePx = minimumDistanceDp.dp.toPx(),
            )
            if (isQuickSwipe) {
                if (isSecondQuickSwipe(previousSwipeTime, releaseTime, maximumIntervalMillis)) {
                    previousSwipeTime = 0L
                    onNext()
                } else {
                    previousSwipeTime = releaseTime
                }
            }
        }
    }

@Composable
private fun PageFloatingControl(page: Int, totalPages: Int, loading: Boolean, onPage: (Int) -> Unit) {
    val pageCount = totalPages.coerceAtLeast(1)
    var showPagePicker by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
    ) {
    Row(
        modifier = Modifier.padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onPage(page - 1) }, enabled = !loading && page > 1) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "上一页")
        }
        TextButton(
            onClick = { showPagePicker = true },
            enabled = !loading && pageCount > 1,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        ) {
            Text("$page / $pageCount", style = MaterialTheme.typography.labelLarge)
        }
        IconButton(onClick = { onPage(page + 1) }, enabled = !loading && page < totalPages) {
            Icon(Icons.Default.ChevronRight, contentDescription = "下一页")
        }
    }
    }
    if (showPagePicker) {
        PagePickerDialog(
            currentPage = page,
            totalPages = pageCount,
            onDismiss = { showPagePicker = false },
            onConfirm = { selectedPage ->
                showPagePicker = false
                if (selectedPage != page) onPage(selectedPage)
            },
        )
    }
}

@Composable
private fun PagePickerDialog(
    currentPage: Int,
    totalPages: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val normalizedCurrentPage = currentPage.coerceIn(1, totalPages)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = normalizedCurrentPage - 1)
    val coroutineScope = rememberCoroutineScope()
    val selectedPage by remember(listState, totalPages) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo
                .minByOrNull { item -> abs(item.offset + item.size / 2 - viewportCenter) }
                ?.index
                ?.plus(1)
                ?.coerceIn(1, totalPages)
                ?: normalizedCurrentPage
        }
    }
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("跳转到页面") },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {}
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    flingBehavior = snapBehavior,
                    contentPadding = PaddingValues(vertical = 96.dp),
                ) {
                    items(count = totalPages, key = { it }) { index ->
                        val itemPage = index + 1
                        val distance = abs(itemPage - selectedPage)
                        Box(
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                                .clickable {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(itemPage - 1)
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "第 $itemPage 页",
                                style = if (distance == 0) MaterialTheme.typography.titleMedium
                                else MaterialTheme.typography.bodyLarge,
                                color = when (distance) {
                                    0 -> MaterialTheme.colorScheme.onSecondaryContainer
                                    1 -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
                                },
                                fontWeight = if (distance == 0) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedPage) }) {
                Text("跳转")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun topicTitleColor(titleFont: Int): Color = when (titleFont and 0x1f) {
    1 -> Color(0xFFD32F2F)
    2 -> Color(0xFF1976D2)
    4 -> Color(0xFF2E7D32)
    8 -> Color(0xFFEF6C00)
    16 -> MaterialTheme.colorScheme.outline
    else -> MaterialTheme.colorScheme.onSurface
}

@Composable
private fun PostCard(
    post: Post,
    isTopic: Boolean = false,
    ipLocation: String = "",
    onLoadIpLocation: (Post) -> Unit = {},
    onOpenUser: (Post) -> Unit = {},
    analyzedUserIds: Set<Long> = emptySet(),
) {
    LaunchedEffect(post.authorId, post.isAnonymous) {
        if (ipLocation.isBlank()) onLoadIpLocation(post)
    }
    Card(Modifier.padding(horizontal = 12.dp).fillMaxWidth()) {
        Column(Modifier.padding(14.dp).fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().clickable(
                    enabled = !post.isAnonymous && post.authorId > 0,
                    onClick = { onOpenUser(post) },
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                UserAvatar(post, post.authorId in analyzedUserIds)
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Text(
                        if (post.isAnonymous) "${post.author}（匿名）" else post.author,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (ipLocation.isNotBlank()) {
                        Text(
                            "IP属地 $ipLocation",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(if (isTopic) "主题" else "#${post.floor}", color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(10.dp))
            if (isTopic && post.subject.isNotBlank()) {
                TrackedSelectionContainer {
                    Text(post.subject, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                if (post.content.isNotBlank()) Spacer(Modifier.height(10.dp))
            }
            if (post.content.isNotBlank()) {
                CompositionLocalProvider(
                    LocalImageDiagnosticContext provides "tid=${post.tid} pid=${post.pid} floor=${post.floor}",
                ) {
                    BbCodeContent(post.content)
                }
            }
            PostAttachments(post.attachments, post.content)
            if (post.comments.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    post.comments.forEach { comment -> CommentCard(comment, onOpenUser, analyzedUserIds) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ThumbUp,
                        contentDescription = "点赞数",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        post.score.toString(),
                        modifier = Modifier.padding(start = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    formatPostTime(post.postDateSeconds),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CommentCard(
    comment: Post,
    onOpenUser: (Post) -> Unit = {},
    analyzedUserIds: Set<Long> = emptySet(),
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(9.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            Row(
                modifier = Modifier.clickable(
                    enabled = !comment.isAnonymous && comment.authorId > 0,
                    onClick = { onOpenUser(comment) },
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                UserAvatar(comment, comment.authorId in analyzedUserIds)
                Column(Modifier.padding(start = 8.dp)) {
                    Text(
                        if (comment.isAnonymous) "${comment.author}（匿名）" else comment.author,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.height(7.dp))
            CompositionLocalProvider(
                LocalImageDiagnosticContext provides "tid=${comment.tid} pid=${comment.pid} floor=${comment.floor}",
            ) {
                BbCodeContent(comment.content)
            }
            PostAttachments(comment.attachments, comment.content)
            if (comment.comments.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(start = 10.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    comment.comments.forEach { nested -> CommentCard(nested, onOpenUser, analyzedUserIds) }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "👍 ${comment.score}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    formatPostTime(comment.postDateSeconds),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PostAttachments(attachments: List<PostAttachment>, content: String) {
    if (attachments.isEmpty()) return
    val inlineUrls = remember(content) { collectPostAttachmentUrls(BbCodeParser().parse(content)) }
    val remaining = remember(attachments, inlineUrls) {
        attachments.filterNot { attachment ->
            inlineUrls.any { inlineUrl -> attachmentUrlKey(inlineUrl) == attachmentUrlKey(attachment.url) }
        }
    }
    if (remaining.isEmpty()) return
    Column(
        modifier = Modifier.padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        remaining.forEach { attachment ->
            AttachmentCard(attachment.url, attachment.name, attachment.sizeLabel)
        }
    }
}

internal fun collectPostAttachmentUrls(nodes: List<BbNode>): Set<String> = buildSet {
    fun collect(children: List<BbNode>) {
        children.forEach { node ->
            when (node) {
                is BbNode.Attachment -> add(node.url)
                is BbNode.Image -> add(node.url)
                is BbNode.Video -> add(node.url)
                is BbNode.Bold -> collect(node.children)
                is BbNode.Italic -> collect(node.children)
                is BbNode.Underline -> collect(node.children)
                is BbNode.Strikethrough -> collect(node.children)
                is BbNode.Color -> collect(node.children)
                is BbNode.Span -> collect(node.children)
                is BbNode.Quote -> collect(node.children)
                is BbNode.Collapse -> collect(node.children)
                is BbNode.Warning -> collect(node.children)
                is BbNode.Album -> collect(node.children)
                is BbNode.Heading -> collect(node.children)
                is BbNode.Paragraph -> collect(node.children)
                is BbNode.Align -> collect(node.children)
                is BbNode.Subscript -> collect(node.children)
                is BbNode.Superscript -> collect(node.children)
                is BbNode.Link -> collect(node.children)
                is BbNode.BulletList -> node.items.forEach(::collect)
                is BbNode.Table -> node.rows.forEach { row -> row.forEach(::collect) }
                is BbNode.Text, is BbNode.Code, is BbNode.Emotion, BbNode.HorizontalRule -> Unit
            }
        }
    }
    collect(nodes)
}

internal fun attachmentUrlKey(url: String): String = url.trim()
    .substringAfter("://", url.trim())
    .substringBefore('#')
    .lowercase(Locale.ROOT)

@Composable
private fun AttachmentCard(
    url: String,
    name: String,
    sizeLabel: String = "",
    compact: Boolean = false,
) {
    val context = LocalContext.current
    val session = LocalNgaSession.current
    val scope = rememberCoroutineScope()
    var preparing by remember(url) { mutableStateOf(false) }

    fun startDownload() {
        if (preparing) return
        if (session == null) {
            Toast.makeText(context, "请登录后下载附件", Toast.LENGTH_SHORT).show()
            return
        }
        preparing = true
        scope.launch {
            withContext(Dispatchers.IO) {
                AttachmentDownloader.enqueue(
                    context.applicationContext,
                    PostAttachment(url = url, name = name, sizeLabel = sizeLabel),
                    session,
                )
            }.onSuccess {
                Toast.makeText(context, "已加入系统下载：${it.fileName}", Toast.LENGTH_LONG).show()
            }.onFailure {
                Toast.makeText(context, "附件下载失败：${it.message ?: "未知错误"}", Toast.LENGTH_LONG).show()
            }
            preparing = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startDownload()
        else Toast.makeText(context, "需要存储权限才能下载附件", Toast.LENGTH_SHORT).show()
    }
    fun downloadWithPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        ) startDownload() else permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(9.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (compact) 0.55f else 0.72f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = if (compact) 5.dp else 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.AttachFile,
                contentDescription = null,
                modifier = Modifier.size(if (compact) 18.dp else 22.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Column(Modifier.padding(start = 8.dp).weight(1f)) {
                Text(
                    name.ifBlank { "NGA 附件" },
                    maxLines = if (compact) 1 else 2,
                    style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                if (!compact) {
                    Text(
                        sizeLabel.ifBlank { "点击下载附件" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f),
                    )
                }
            }
            if (preparing) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            } else if (compact) {
                IconButton(
                    onClick = ::downloadWithPermission,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(Icons.Default.Download, contentDescription = "下载附件", modifier = Modifier.size(18.dp))
                }
            } else {
                OutlinedButton(
                    onClick = ::downloadWithPermission,
                    modifier = Modifier.width(68.dp).height(34.dp),
                    contentPadding = PaddingValues(4.dp),
                ) { Text("下载附件", style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}

@Composable
private fun UserAvatar(post: Post, hasAnalysis: Boolean = false) {
    var failed by remember(post.avatarUrl) { mutableStateOf(false) }
    var loaded by remember(post.avatarUrl) { mutableStateOf(false) }
    Box(Modifier.size(42.dp)) {
        Box(
            modifier = Modifier.size(38.dp).align(Alignment.BottomStart).clip(RoundedCornerShape(19.dp))
                .background(defaultAvatarColor(post.authorId, post.author)),
            contentAlignment = Alignment.Center,
        ) {
            if (!loaded) {
                Text(post.author.take(1).ifBlank { "?" }, color = Color.White, fontWeight = FontWeight.Bold)
            }
            if (!failed && post.avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = post.avatarUrl,
                    contentDescription = "${post.author}头像",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onSuccess = { loaded = true },
                    onError = { failed = true },
                )
            }
        }
        if (hasAnalysis) {
            CompletedAiMark(Modifier.align(Alignment.TopEnd).size(14.dp))
        }
    }
}

@Composable
private fun BbCodeContent(content: String) {
    val openLink = LocalLinkOpener.current
    if (remember(content) { looksLikeMarkdown(content) }) {
        MarkdownContent(content, openLink)
        return
    }
    val nodes = remember(content) { BbCodeParser().parse(content) }
    val galleryUrls = remember(nodes) { collectPostImageUrls(nodes) }
    var previewIndex by remember(content) { mutableStateOf<Int?>(null) }
    CompositionLocalProvider(
        LocalPostImageGallery provides PostImageGallery(galleryUrls) { url ->
            previewIndex = galleryUrls.indexOf(url).takeIf { it >= 0 }
        },
    ) {
        RenderNodes(trimNodeWhitespace(nodes))
    }
    previewIndex?.let { initialIndex ->
        ZoomableImageGalleryDialog(
            urls = galleryUrls,
            initialIndex = initialIndex,
            onDismiss = { previewIndex = null },
        )
    }
}

internal fun collectPostImageUrls(nodes: List<BbNode>): List<String> {
    val result = linkedSetOf<String>()
    fun collect(children: List<BbNode>) {
        children.forEach { node ->
            when (node) {
                is BbNode.Image -> node.url.takeIf(String::isNotBlank)?.let(result::add)
                // Quoted media has its own compact preview and must not enter the reply gallery.
                is BbNode.Quote -> Unit
                is BbNode.Bold -> collect(node.children)
                is BbNode.Italic -> collect(node.children)
                is BbNode.Underline -> collect(node.children)
                is BbNode.Strikethrough -> collect(node.children)
                is BbNode.Color -> collect(node.children)
                is BbNode.Span -> collect(node.children)
                is BbNode.Collapse -> collect(node.children)
                is BbNode.Warning -> collect(node.children)
                is BbNode.Album -> collect(node.children)
                is BbNode.Heading -> collect(node.children)
                is BbNode.Paragraph -> collect(node.children)
                is BbNode.Align -> collect(node.children)
                is BbNode.Subscript -> collect(node.children)
                is BbNode.Superscript -> collect(node.children)
                is BbNode.Link -> collect(node.children)
                is BbNode.BulletList -> node.items.forEach(::collect)
                is BbNode.Table -> node.rows.forEach { row -> row.forEach(::collect) }
                is BbNode.Text, is BbNode.Code, is BbNode.Video, is BbNode.Attachment,
                is BbNode.Emotion, BbNode.HorizontalRule -> Unit
            }
        }
    }
    collect(nodes)
    return result.toList()
}

private enum class MediaRenderMode { Normal, CompactQuote }

@Composable
private fun RenderNodes(
    nodes: List<BbNode>,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    mediaMode: MediaRenderMode = MediaRenderMode.Normal,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val inline = mutableListOf<BbNode>()
        var trimLeadingInlineWhitespace = false

        @Composable
        fun FlushInline() {
            if (inline.isNotEmpty()) {
                RichText(
                    inline.toList(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = contentColor),
                )
                inline.clear()
            }
        }

        nodes.forEach { node ->
            when (node) {
                is BbNode.Quote -> {
                    FlushInline()
                    QuoteBlock(node)
                    trimLeadingInlineWhitespace = true
                }
                is BbNode.Collapse -> {
                    FlushInline()
                    ExpandableBbCodeBlock(title = node.title, initiallyExpanded = false) {
                        RenderNodes(node.children, contentColor, mediaMode)
                    }
                }
                is BbNode.Album -> {
                    FlushInline()
                    ExpandableBbCodeBlock(title = "相册 · ${node.title}", initiallyExpanded = true) {
                        RenderNodes(node.children, contentColor, mediaMode)
                    }
                }
                is BbNode.Warning -> {
                    FlushInline()
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(10.dp)) {
                            Text(
                                "警告",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            RenderNodes(node.children, MaterialTheme.colorScheme.onErrorContainer, mediaMode)
                        }
                    }
                }
                is BbNode.Heading -> {
                    FlushInline()
                    RichText(
                        node.children,
                        MaterialTheme.typography.titleMedium.copy(
                            color = contentColor,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    HorizontalDivider()
                }
                is BbNode.Paragraph -> {
                    FlushInline()
                    RenderNodes(node.children, contentColor, mediaMode)
                }
                is BbNode.Align -> {
                    FlushInline()
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = when (node.value) {
                            "center" -> Alignment.Center
                            "right" -> Alignment.CenterEnd
                            else -> Alignment.CenterStart
                        },
                    ) {
                        RenderNodes(node.children, contentColor, mediaMode)
                    }
                }
                BbNode.HorizontalRule -> {
                    FlushInline()
                    HorizontalDivider()
                }
                is BbNode.Image -> {
                    FlushInline()
                    if (mediaMode == MediaRenderMode.CompactQuote) {
                        CompactQuotedImage(node.url)
                    } else {
                        PostInlineImage(node.url)
                    }
                }
                is BbNode.Video -> {
                    FlushInline()
                    if (mediaMode == MediaRenderMode.CompactQuote) CompactQuotedVideo(node.url)
                    else InlineVideoPlayer(node.url)
                }
                is BbNode.Attachment -> {
                    FlushInline()
                    AttachmentCard(node.url, node.name, compact = mediaMode == MediaRenderMode.CompactQuote)
                }
                is BbNode.Link -> when {
                    isBilibiliUrl(node.href) -> {
                        FlushInline()
                        BilibiliLinkCard(node.href, compact = mediaMode == MediaRenderMode.CompactQuote)
                    }
                    isWeiboUrl(node.href) -> {
                        FlushInline()
                        WeiboLinkCard(node.href, compact = mediaMode == MediaRenderMode.CompactQuote)
                    }
                    isXiaohongshuUrl(node.href) -> {
                        FlushInline()
                        XiaohongshuLinkCard(node.href, compact = mediaMode == MediaRenderMode.CompactQuote)
                    }
                    isDouyinUrl(node.href) -> {
                        FlushInline()
                        DouyinLinkCard(node.href, compact = mediaMode == MediaRenderMode.CompactQuote)
                    }
                    else -> {
                        inline += node
                        trimLeadingInlineWhitespace = false
                    }
                }
                is BbNode.Code -> {
                    FlushInline()
                    TrackedSelectionContainer {
                        Text(
                            node.value,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .horizontalScroll(rememberScrollState())
                                .padding(12.dp),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodyMedium.copy(color = contentColor),
                        )
                    }
                }
                is BbNode.BulletList -> {
                    FlushInline()
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        node.items.forEach { item ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("•")
                                Box(Modifier.weight(1f)) { RenderNodes(item, contentColor, mediaMode) }
                            }
                        }
                    }
                }
                is BbNode.Table -> {
                    FlushInline()
                    val columnWidths = remember(node) {
                        val count = node.rows.maxOfOrNull { it.size } ?: 0
                        List(count) { column ->
                            val longest = node.rows.maxOfOrNull { row ->
                                row.getOrNull(column)?.sumOf { it.plainText().length } ?: 0
                            } ?: 0
                            (longest.coerceIn(7, 22) * 12 + 20).dp
                        }
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    ) {
                        node.rows.forEachIndexed { rowIndex, row ->
                            Row {
                                row.forEachIndexed { columnIndex, cell ->
                                    Box(
                                        modifier = Modifier.width(columnWidths.getOrElse(columnIndex) { 128.dp })
                                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                            .background(
                                                if (rowIndex == 0) MaterialTheme.colorScheme.surfaceVariant
                                                else Color.Transparent,
                                            )
                                            .padding(horizontal = 9.dp, vertical = 7.dp),
                                    ) {
                                        RenderNodes(cell, contentColor, mediaMode)
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    val visibleNode = if (trimLeadingInlineWhitespace && node is BbNode.Text) {
                        node.copy(value = node.value.trimStart())
                    } else node
                    if (visibleNode !is BbNode.Text || visibleNode.value.isNotEmpty()) {
                        inline += visibleNode
                        trimLeadingInlineWhitespace = false
                    }
                }
            }
        }
        FlushInline()
    }
}

private data class QuoteParts(val header: List<BbNode>, val body: List<BbNode>)

private fun splitQuote(nodes: List<BbNode>): QuoteParts {
    val headerEnd = nodes.indexOfFirst { node -> node.plainText().contains("Post by", ignoreCase = true) }
    if (headerEnd < 0) return QuoteParts(emptyList(), trimNodeWhitespace(nodes))
    return QuoteParts(
        header = trimNodeWhitespace(nodes.take(headerEnd + 1)),
        body = trimNodeWhitespace(nodes.drop(headerEnd + 1)),
    )
}

private fun trimNodeWhitespace(nodes: List<BbNode>): List<BbNode> {
    val result = nodes.toMutableList()
    while ((result.firstOrNull() as? BbNode.Text)?.value?.isBlank() == true) result.removeAt(0)
    while ((result.lastOrNull() as? BbNode.Text)?.value?.isBlank() == true) result.removeAt(result.lastIndex)
    (result.firstOrNull() as? BbNode.Text)?.let { result[0] = it.copy(value = it.value.trimStart()) }
    (result.lastOrNull() as? BbNode.Text)?.let { result[result.lastIndex] = it.copy(value = it.value.trimEnd()) }
    return result
}

private fun BbNode.plainText(): String = when (this) {
    is BbNode.Text -> value
    is BbNode.Bold -> children.joinToString("") { it.plainText() }
    is BbNode.Italic -> children.joinToString("") { it.plainText() }
    is BbNode.Underline -> children.joinToString("") { it.plainText() }
    is BbNode.Strikethrough -> children.joinToString("") { it.plainText() }
    is BbNode.Color -> children.joinToString("") { it.plainText() }
    is BbNode.Span -> children.joinToString("") { it.plainText() }
    is BbNode.Quote -> children.joinToString("") { it.plainText() }
    is BbNode.Collapse -> children.joinToString("") { it.plainText() }
    is BbNode.Warning -> children.joinToString("") { it.plainText() }
    is BbNode.Album -> children.joinToString("") { it.plainText() }
    is BbNode.Heading -> children.joinToString("") { it.plainText() }
    is BbNode.Paragraph -> children.joinToString("") { it.plainText() }
    is BbNode.Align -> children.joinToString("") { it.plainText() }
    is BbNode.Subscript -> children.joinToString("") { it.plainText() }
    is BbNode.Superscript -> children.joinToString("") { it.plainText() }
    BbNode.HorizontalRule -> ""
    is BbNode.Link -> children.joinToString("") { it.plainText() }
    is BbNode.Attachment -> name
    is BbNode.Code -> value
    is BbNode.BulletList -> items.joinToString("\n") { row -> row.joinToString("") { it.plainText() } }
    is BbNode.Table -> rows.joinToString("\n") { row ->
        row.joinToString("\t") { cell -> cell.joinToString("") { it.plainText() } }
    }
    is BbNode.Image -> "[图片]"
    is BbNode.Video -> "[视频]"
    is BbNode.Emotion -> "[表情]"
}

@Composable
private fun PostInlineImage(url: String) {
    val automaticImages = LocalAutomaticImageLoading.current
    val gallery = LocalPostImageGallery.current
    var manualAllowed by remember(url) { mutableStateOf(false) }
    var retryToken by remember(url) { mutableIntStateOf(0) }
    if (!automaticImages && !manualAllowed) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(72.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            onClick = { manualAllowed = true },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("点击加载图片", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        return
    }
    SubcomposeAsyncImage(
        model = rememberNgaImageRequest(url, "post-inline", retryToken),
        contentDescription = "帖子图片",
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxWidth(),
        loading = {
            Box(
                Modifier.fillMaxWidth().height(88.dp).clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("正在加载图片…", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        error = {
            val throwable = (painter.state as? AsyncImagePainter.State.Error)?.result?.throwable
            ImageFailureCard(
                url = url,
                error = throwable,
                retryToken = retryToken,
                onRetry = { retryToken++ },
            )
        },
        success = {
            SubcomposeAsyncImageContent(
                modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)
                    .clip(RoundedCornerShape(8.dp)).clickable { gallery?.onOpen?.invoke(url) },
            )
        },
    )
}

@Composable
private fun ImageFailureCard(
    url: String,
    error: Throwable?,
    retryToken: Int,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val diagnosticPurpose = "post-inline ${LocalImageDiagnosticContext.current}"
    var downloading by remember(url) { mutableStateOf(false) }
    LaunchedEffect(url, retryToken) {
        ImageDiagnosticStore.probe(context.applicationContext, url, diagnosticPurpose)
    }
    fun copyDiagnostic() {
        val report = ImageDiagnosticStore.report(context, url)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("LNGA 图片加载与查看诊断", report))
        Toast.makeText(context, "该图片的脱敏诊断已复制", Toast.LENGTH_SHORT).show()
    }
    fun startDownload() {
        if (downloading) return
        downloading = true
        scope.launch {
            ImageDownloader.download(context, url)
                .onSuccess { Toast.makeText(context, "原图已保存到图片/LNGA", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, "下载失败：${it.message ?: "未知错误"}", Toast.LENGTH_LONG).show() }
            downloading = false
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startDownload()
        else Toast.makeText(context, "需要存储权限才能保存图片", Toast.LENGTH_SHORT).show()
    }
    fun downloadWithPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        ) startDownload() else permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.58f),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text("图片加载失败", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onErrorContainer)
            Text(
                imageFailureMessage(error),
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onRetry) { Text("重试") }
                TextButton(onClick = ::downloadWithPermission, enabled = !downloading) {
                    Text(if (downloading) "下载中" else "下载原图")
                }
                TextButton(onClick = ::copyDiagnostic) { Text("复制诊断") }
            }
        }
    }
}

@Composable
private fun CompactQuotedImage(url: String) {
    val automaticImages = LocalAutomaticImageLoading.current
    var previewing by remember(url) { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(7.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        onClick = { previewing = true },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (automaticImages) {
                AsyncImage(
                    model = rememberNgaImageRequest(url, "quote-thumbnail"),
                    contentDescription = "引用中的图片",
                    modifier = Modifier.width(72.dp).fillMaxHeight(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(Modifier.width(72.dp).fillMaxHeight(), contentAlignment = Alignment.Center) { Text("图") }
            }
            Column(Modifier.padding(horizontal = 9.dp).weight(1f)) {
                Text("图片 / GIF", style = MaterialTheme.typography.labelMedium)
                Text(
                    "点击查看原图",
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (previewing) ZoomableImageDialog(url, onDismiss = { previewing = false })
}

@Composable
private fun CompactQuotedVideo(url: String) {
    var expanded by remember(url) { mutableStateOf(false) }
    if (expanded) {
        InlineVideoPlayer(url)
    } else {
        Surface(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(7.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            onClick = { expanded = true },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Text("▶", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                Column(Modifier.weight(1f)) {
                    Text("视频", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "点击展开并播放",
                        maxLines = 1,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun InlineVideoPlayer(url: String) {
    val context = LocalContext.current
    val activity = context.findActivity()
    var fullscreen by remember(url) { mutableStateOf(false) }
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    if (!fullscreen) {
        VideoPlayerView(
            player = player,
            onFullscreen = { fullscreen = true },
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp)).background(Color.Black),
        )
    } else {
        Dialog(
            onDismissRequest = { fullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            BackHandler { fullscreen = false }
            DisposableEffect(activity) {
                val previousOrientation = activity?.requestedOrientation
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                activity?.window?.let { window ->
                    WindowCompat.getInsetsController(window, window.decorView)
                        .hide(WindowInsetsCompat.Type.systemBars())
                }
                onDispose {
                    activity?.window?.let { window ->
                        WindowCompat.getInsetsController(window, window.decorView)
                            .show(WindowInsetsCompat.Type.systemBars())
                    }
                    activity?.requestedOrientation = previousOrientation ?: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }
            VideoPlayerView(
                player = player,
                onFullscreen = { fullscreen = false },
                modifier = Modifier.fillMaxSize().background(Color.Black),
            )
        }
    }
}

@Composable
private fun BilibiliLinkCard(url: String, compact: Boolean = false) {
    val context = LocalContext.current
    val openLink = LocalLinkOpener.current
    val loader = remember(context) { BilibiliPreviewLoader(context.applicationContext) }
    val onLoadingChanged = LocalBilibiliLoadingChanged.current
    var preview by remember(url) { mutableStateOf<BilibiliPreview?>(null) }
    LaunchedEffect(url, loader, onLoadingChanged) {
        onLoadingChanged(true)
        try {
            preview = loader.load(url)
        } finally {
            onLoadingChanged(false)
        }
    }
    val target = preview?.url ?: url
    if (compact) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(7.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            onClick = { openLink(target) },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val cover = preview?.coverUrl.orEmpty()
                if (cover.isNotBlank()) {
                    AsyncImage(
                        model = cover,
                        contentDescription = "Bilibili 视频封面",
                        modifier = Modifier.width(72.dp).fillMaxHeight(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        Modifier.width(48.dp).fillMaxHeight().background(Color(0xFFFB7299).copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("B", color = Color(0xFFFB7299), fontWeight = FontWeight.Bold)
                    }
                }
                Column(Modifier.padding(horizontal = 9.dp).weight(1f)) {
                    Text("哔哩哔哩", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFB7299))
                    Text(
                        preview?.title ?: "Bilibili 视频链接",
                        maxLines = 1,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
        return
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = { openLink(target) },
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            val cover = preview?.coverUrl.orEmpty()
            if (cover.isNotBlank()) {
                Box {
                    AsyncImage(
                        model = cover,
                        contentDescription = "Bilibili 视频封面",
                        modifier = Modifier.width(120.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(7.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    preview?.durationSeconds?.takeIf { it > 0 }?.let { seconds ->
                        Text(
                            formatVideoDuration(seconds),
                            modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
                                .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("哔哩哔哩", style = MaterialTheme.typography.labelMedium, color = Color(0xFFFB7299))
                Text(
                    preview?.title ?: "Bilibili 视频链接",
                    maxLines = 2,
                    style = MaterialTheme.typography.bodyMedium,
                )
                preview?.uploader?.takeIf(String::isNotBlank)?.let {
                    Text(it, maxLines = 1, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun WeiboLinkCard(url: String, compact: Boolean = false) {
    val context = LocalContext.current
    val openLink = LocalLinkOpener.current
    val loader = remember(context) { WeiboPreviewLoader(context.applicationContext) }
    var preview by remember(url) { mutableStateOf<WeiboPreview?>(null) }
    var loaded by remember(url) { mutableStateOf(false) }
    LaunchedEffect(url, loader) {
        preview = loader.load(url)
        loaded = true
    }
    val target = preview?.url ?: url
    val cover = preview?.coverUrl.orEmpty()
    if (compact) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(7.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            onClick = { openLink(target) },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (cover.isNotBlank()) {
                    AsyncImage(
                        model = cover,
                        contentDescription = "微博配图",
                        modifier = Modifier.width(72.dp).fillMaxHeight(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    WeiboMark(Modifier.width(48.dp).fillMaxHeight())
                }
                Column(Modifier.padding(horizontal = 9.dp).weight(1f)) {
                    Text(
                        preview?.author?.takeIf(String::isNotBlank)?.let { "微博 · $it" } ?: "微博博文",
                        maxLines = 1,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE6162D),
                    )
                    Text(
                        preview?.text?.takeIf(String::isNotBlank)
                            ?: if (loaded) "点击打开微博" else "正在读取摘要…",
                        maxLines = 1,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
        return
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = { openLink(target) },
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (cover.isNotBlank()) {
                AsyncImage(
                    model = cover,
                    contentDescription = "微博配图",
                    modifier = Modifier.width(112.dp).height(84.dp).clip(RoundedCornerShape(7.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.width(10.dp))
            } else {
                WeiboMark(Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)))
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("微博", style = MaterialTheme.typography.labelMedium, color = Color(0xFFE6162D))
                preview?.author?.takeIf(String::isNotBlank)?.let { author ->
                    Text(author, maxLines = 1, style = MaterialTheme.typography.labelMedium)
                }
                Text(
                    preview?.text?.takeIf(String::isNotBlank)
                        ?: if (loaded) "微博博文 · 点击查看原文" else "正在读取微博摘要…",
                    maxLines = 3,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (preview == null) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun WeiboMark(modifier: Modifier = Modifier) {
    Box(
        modifier.background(Color(0xFFE6162D).copy(alpha = 0.13f)),
        contentAlignment = Alignment.Center,
    ) {
        Text("微", color = Color(0xFFE6162D), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun XiaohongshuLinkCard(url: String, compact: Boolean = false) {
    val context = LocalContext.current
    val openLink = LocalLinkOpener.current
    val loader = remember(context) { XiaohongshuPreviewLoader(context.applicationContext) }
    var preview by remember(url) { mutableStateOf<XiaohongshuPreview?>(null) }
    var loaded by remember(url) { mutableStateOf(false) }
    LaunchedEffect(url, loader) {
        preview = loader.load(url)
        loaded = true
    }
    val target = preview?.url ?: url
    val cover = preview?.coverUrl.orEmpty()
    val label = when (preview?.type) {
        "video" -> "小红书 · 视频"
        "normal" -> "小红书 · 图文"
        else -> "小红书笔记"
    }
    val summary = preview?.title?.takeIf(String::isNotBlank)
        ?: preview?.description?.takeIf(String::isNotBlank)
        ?: if (loaded) "点击查看小红书原文" else "正在读取摘要…"
    if (compact) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(7.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            onClick = { openLink(target) },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (cover.isNotBlank()) {
                    AsyncImage(
                        model = cover,
                        contentDescription = "小红书封面",
                        modifier = Modifier.width(72.dp).fillMaxHeight(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    XiaohongshuMark(Modifier.width(48.dp).fillMaxHeight())
                }
                Column(Modifier.padding(horizontal = 9.dp).weight(1f)) {
                    Text(label, maxLines = 1, style = MaterialTheme.typography.labelSmall, color = XHS_RED)
                    Text(summary, maxLines = 1, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        return
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = { openLink(target) },
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (cover.isNotBlank()) {
                AsyncImage(
                    model = cover,
                    contentDescription = "小红书封面",
                    modifier = Modifier.width(112.dp).height(84.dp).clip(RoundedCornerShape(7.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                XiaohongshuMark(Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = XHS_RED)
                preview?.author?.takeIf(String::isNotBlank)?.let { author ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (preview?.avatarUrl?.isNotBlank() == true) {
                            AsyncImage(
                                model = preview?.avatarUrl,
                                contentDescription = "小红书作者头像",
                                modifier = Modifier.size(20.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                            Spacer(Modifier.width(5.dp))
                        }
                        Text(author, maxLines = 1, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Text(
                    summary,
                    maxLines = 3,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (preview == null) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private val XHS_RED = Color(0xFFFF2442)

@Composable
private fun XiaohongshuMark(modifier: Modifier = Modifier) {
    Box(modifier.background(XHS_RED.copy(alpha = 0.13f)), contentAlignment = Alignment.Center) {
        Text("薯", color = XHS_RED, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DouyinLinkCard(url: String, compact: Boolean = false) {
    val context = LocalContext.current
    val openLink = LocalLinkOpener.current
    val loader = remember(context) { DouyinPreviewLoader(context.applicationContext) }
    var preview by remember(url) { mutableStateOf<DouyinPreview?>(null) }
    var loaded by remember(url) { mutableStateOf(false) }
    LaunchedEffect(url, loader) {
        preview = loader.load(url)
        loaded = true
    }
    val target = preview?.url ?: url
    val cover = preview?.coverUrl.orEmpty()
    val summary = preview?.title?.takeIf(String::isNotBlank)
        ?: if (loaded) "点击打开抖音视频" else "正在读取视频摘要…"
    if (compact) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(7.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            onClick = { openLink(target) },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (cover.isNotBlank()) {
                    AsyncImage(
                        model = cover,
                        contentDescription = "抖音视频封面",
                        modifier = Modifier.width(72.dp).fillMaxHeight(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    DouyinMark(Modifier.width(48.dp).fillMaxHeight())
                }
                Column(Modifier.padding(horizontal = 9.dp).weight(1f)) {
                    Text("抖音视频", maxLines = 1, style = MaterialTheme.typography.labelSmall, color = DOUYIN_PINK)
                    Text(summary, maxLines = 1, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        return
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = { openLink(target) },
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (cover.isNotBlank()) {
                Box {
                    AsyncImage(
                        model = cover,
                        contentDescription = "抖音视频封面",
                        modifier = Modifier.width(120.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(7.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    preview?.durationSeconds?.takeIf { it > 0 }?.let { seconds ->
                        Text(
                            formatVideoDuration(seconds),
                            modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
                                .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            } else {
                DouyinMark(Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("抖音视频", style = MaterialTheme.typography.labelMedium, color = DOUYIN_PINK)
                preview?.author?.takeIf(String::isNotBlank)?.let { author ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (preview?.avatarUrl?.isNotBlank() == true) {
                            AsyncImage(
                                model = preview?.avatarUrl,
                                contentDescription = "抖音作者头像",
                                modifier = Modifier.size(20.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                            Spacer(Modifier.width(5.dp))
                        }
                        Text(author, maxLines = 1, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Text(
                    summary,
                    maxLines = 3,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (preview == null) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private val DOUYIN_PINK = Color(0xFFFE2C55)

@Composable
private fun DouyinMark(modifier: Modifier = Modifier) {
    Box(modifier.background(DOUYIN_PINK.copy(alpha = 0.13f)), contentAlignment = Alignment.Center) {
        Text("抖", color = DOUYIN_PINK, fontWeight = FontWeight.Bold)
    }
}

private fun formatVideoDuration(seconds: Int): String =
    if (seconds >= 3600) "%d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60)
    else "%d:%02d".format(seconds / 60, seconds % 60)

@Composable
@SuppressLint("UnsafeOptInUsageError")
private fun VideoPlayerView(player: ExoPlayer, onFullscreen: () -> Unit, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { playerContext ->
            PlayerView(playerContext).apply {
                this.player = player
                useController = true
                controllerShowTimeoutMs = 2800
                controllerHideOnTouch = true
                controllerAutoShow = true
                setFullscreenButtonClickListener { onFullscreen() }
            }
        },
        update = { view ->
            view.player = player
            view.setFullscreenButtonClickListener { onFullscreen() }
        },
        modifier = modifier,
    )
}

internal fun shouldOpenInInternalBrowser(url: String, enabled: Boolean): Boolean {
    if (!enabled || isBilibiliUrl(url) || isWeiboUrl(url) || isXiaohongshuUrl(url) || isDouyinUrl(url)) return false
    val scheme = runCatching { java.net.URI(url).scheme.orEmpty() }.getOrDefault("")
    return scheme.equals("http", ignoreCase = true) || scheme.equals("https", ignoreCase = true)
}

@Composable
private fun rememberWifiConnection(): Boolean {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
    fun current(): Boolean = manager.getNetworkCapabilities(manager.activeNetwork)
        ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    var connected by remember(manager) { mutableStateOf(current()) }
    DisposableEffect(manager) {
        val mainHandler = Handler(Looper.getMainLooper())
        fun publish(value: Boolean) {
            if (Looper.myLooper() == Looper.getMainLooper()) connected = value
            else mainHandler.post { connected = value }
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                publish(
                    manager.getNetworkCapabilities(network)
                        ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true,
                )
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                if (network == manager.activeNetwork) {
                    publish(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
                }
            }

            override fun onLost(network: Network) {
                publish(current())
            }
        }
        runCatching { manager.registerDefaultNetworkCallback(callback) }
        onDispose {
            runCatching { manager.unregisterNetworkCallback(callback) }
            mainHandler.removeCallbacksAndMessages(null)
        }
    }
    return connected
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun ExpandableBbCodeBlock(
    title: String,
    initiallyExpanded: Boolean,
    content: @Composable () -> Unit,
) {
    var expanded by remember(title) { mutableStateOf(initiallyExpanded) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                )
            }
            if (expanded) Box(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 10.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun QuoteBlock(node: BbNode.Quote) {
    val parts = remember(node) { splitQuote(node.children) }
    val bodyText = remember(parts.body) { parts.body.joinToString("") { it.plainText() } }
    val isLong = remember(bodyText) {
        bodyText.lines().size > 5 || bodyText.lines().sumOf { (it.length + 21) / 22 } > 5
    }
    var expanded by remember(node) { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f))
            .clickable(enabled = isLong) { expanded = !expanded }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (parts.header.isNotEmpty()) {
            RichText(
                parts.header,
                textStyle = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
        if (parts.body.isNotEmpty()) {
            val quotedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.56f)
            Box(
                Modifier.fillMaxWidth().then(
                    if (isLong && !expanded) Modifier.heightIn(max = 112.dp).clipToBounds()
                    else Modifier.wrapContentHeight(),
                ),
            ) {
                RenderNodes(parts.body, quotedTextColor, MediaRenderMode.CompactQuote)
            }
        }
        if (isLong) {
            Text(
                if (expanded) "收起引用" else "展开引用",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

@Composable
private fun ZoomableImageDialog(url: String, onDismiss: () -> Unit) {
    ZoomableImageGalleryDialog(listOf(url), initialIndex = 0, onDismiss = onDismiss)
}

@Composable
private fun ZoomableImageGalleryDialog(urls: List<String>, initialIndex: Int, onDismiss: () -> Unit) {
    if (urls.isEmpty()) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(urls.indices),
        pageCount = urls::size,
    )
    var downloadingUrl by remember { mutableStateOf<String?>(null) }
    var pendingDownloadUrl by remember { mutableStateOf<String?>(null) }
    fun startDownload(url: String) {
        if (downloadingUrl != null) return
        downloadingUrl = url
        scope.launch {
            ImageDownloader.download(context, url)
                .onSuccess { Toast.makeText(context, "已保存到图片/LNGA", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, "下载失败：${it.message ?: "未知错误"}", Toast.LENGTH_LONG).show() }
            downloadingUrl = null
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val target = pendingDownloadUrl
        pendingDownloadUrl = null
        if (granted && target != null) startDownload(target)
        else Toast.makeText(context, "需要存储权限才能保存图片", Toast.LENGTH_SHORT).show()
    }
    fun downloadWithPermission(url: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        ) startDownload(url) else {
            pendingDownloadUrl = url
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                    key = { page -> "${page}:${urls[page]}" },
                ) { page ->
                    ZoomableImagePage(
                        url = urls[page],
                        isCurrentPage = pagerState.currentPage == page,
                    )
                }
                if (urls.size > 1) {
                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp),
                        color = Color.Black.copy(alpha = 0.58f),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text(
                            "${pagerState.currentPage + 1} / ${urls.size}",
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                Row(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                    val currentUrl = urls[pagerState.currentPage]
                    IconButton(
                        onClick = { downloadWithPermission(currentUrl) },
                        enabled = downloadingUrl == null,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.55f), CircleShape),
                    ) {
                        if (downloadingUrl != null) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Icon(Icons.Default.Download, contentDescription = "下载原图", tint = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.55f), CircleShape),
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "关闭大图", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomableImagePage(url: String, isCurrentPage: Boolean) {
    val context = LocalContext.current
    var imageRatio by remember(url) { mutableStateOf<Float?>(null) }
    var scale by remember(url) { mutableStateOf(1f) }
    var offsetX by remember(url) { mutableStateOf(0f) }
    var offsetY by remember(url) { mutableStateOf(0f) }
    var lastLayoutSignature by remember(url) { mutableStateOf("") }
    var lastBoundary by remember(url) { mutableStateOf("") }
    var lastDecodedSize by remember(url) { mutableStateOf("") }
    LaunchedEffect(isCurrentPage, imageRatio) {
        if (isCurrentPage) {
            scale = 1f
            offsetX = 0f
            offsetY = 0f
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val ratio = imageRatio
        val longRatio = ratio?.takeIf { it > 0f && it < (1f / 3f) }
        val isLongImage = longRatio != null
        val viewportWidth = maxWidth
        val verticalPadding = maxHeight / 4
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        LaunchedEffect(url, isCurrentPage, widthPx, heightPx, longRatio) {
            if (isCurrentPage) {
                ImageDiagnosticStore.record(
                    context = context,
                    stage = "viewer-viewport",
                    purpose = "post-fullscreen",
                    url = url,
                    detail = "safeViewport=${widthPx.toInt()}x${heightPx.toInt()} ratio=${ratio ?: -1f} " +
                        "long=$isLongImage padding=${(heightPx / 4f).toInt()}",
                )
            }
        }
        val transformState = rememberTransformableState { zoomChange, panChange, _ ->
            val nextScale = (scale * zoomChange).coerceIn(1f, 6f)
            val horizontalBound = ((widthPx * nextScale - widthPx) / 2f).coerceAtLeast(0f)
            scale = nextScale
            offsetX = (offsetX + panChange.x).coerceIn(-horizontalBound, horizontalBound)
            offsetY = if (longRatio != null) {
                val bounds = longImageVerticalPanBounds(widthPx, longRatio, heightPx, nextScale)
                val requested = offsetY + panChange.y
                val clamped = requested.coerceIn(bounds.minimum, bounds.maximum)
                val boundary = when {
                    requested > bounds.maximum -> "top"
                    requested < bounds.minimum -> "bottom"
                    else -> ""
                }
                if (boundary != lastBoundary) {
                    lastBoundary = boundary
                    if (boundary.isNotBlank()) {
                        ImageDiagnosticStore.record(
                            context = context,
                            stage = "viewer-boundary",
                            purpose = "post-fullscreen",
                            url = url,
                            detail = "edge=$boundary requested=$requested clamped=$clamped " +
                                "bounds=${bounds.minimum}..${bounds.maximum} scale=$nextScale",
                        )
                    }
                }
                clamped
            } else {
                val verticalBound = ((heightPx * nextScale - heightPx) / 2f).coerceAtLeast(0f)
                (offsetY + panChange.y).coerceIn(-verticalBound, verticalBound)
            }
        }
        val transformModifier = Modifier
            .pointerInput(url) {
                detectTapGestures(onDoubleTap = {
                    scale = 1f
                    offsetX = 0f
                    offsetY = 0f
                })
            }
            .transformable(
                state = transformState,
                canPan = { delta -> scale > 1f || isLongImage && abs(delta.y) > abs(delta.x) },
            )
        Box(
            modifier = Modifier.fillMaxSize().then(transformModifier).clipToBounds(),
            contentAlignment = if (isLongImage) Alignment.TopCenter else Alignment.Center,
        ) {
            val imageModifier = if (longRatio != null) {
                Modifier.wrapContentSize(Alignment.TopCenter, unbounded = true)
                    .requiredWidth(viewportWidth)
                    .requiredHeight(viewportWidth / longRatio + verticalPadding * 2)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    }
                    .padding(vertical = verticalPadding)
                    .onGloballyPositioned { coordinates ->
                        if (isCurrentPage) {
                            val bounds = coordinates.boundsInWindow()
                            val signature = "${coordinates.size.width}x${coordinates.size.height}:" +
                                "${bounds.left.toInt()},${bounds.top.toInt()},${bounds.right.toInt()},${bounds.bottom.toInt()}"
                            if (signature != lastLayoutSignature) {
                                lastLayoutSignature = signature
                                ImageDiagnosticStore.record(
                                    context = context,
                                    stage = "viewer-layout",
                                    purpose = "post-fullscreen",
                                    url = url,
                                    detail = "measured=${coordinates.size.width}x${coordinates.size.height} " +
                                        "windowBounds=${bounds.left},${bounds.top},${bounds.right},${bounds.bottom} " +
                                        "scale=$scale offsetY=$offsetY",
                                )
                            }
                        }
                    }
            } else {
                Modifier.fillMaxSize().graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                }
            }
            AsyncImage(
                model = rememberNgaImageRequest(url, "post-fullscreen"),
                contentDescription = "图片大图",
                contentScale = ContentScale.Fit,
                modifier = imageModifier,
                onSuccess = { state ->
                    val width = state.result.drawable.intrinsicWidth
                    val height = state.result.drawable.intrinsicHeight
                    if (width > 0 && height > 0) {
                        val decodedSize = "${width}x$height"
                        if (decodedSize != lastDecodedSize) {
                            lastDecodedSize = decodedSize
                            ImageDiagnosticStore.record(
                                context = context,
                                stage = "viewer-decode",
                                purpose = "post-fullscreen",
                                url = url,
                                detail = "intrinsic=$decodedSize ratio=${width.toFloat() / height.toFloat()} " +
                                    "long=${height > width * 3}",
                            )
                        }
                        imageRatio = width.toFloat() / height.toFloat()
                    }
                },
            )
        }
    }
}

internal fun longImageContentHeightPx(
    viewportWidthPx: Float,
    imageWidthToHeightRatio: Float,
    viewportHeightPx: Float,
    scale: Float,
): Float = (viewportWidthPx / imageWidthToHeightRatio + viewportHeightPx / 2f) * scale

internal data class LongImageVerticalPanBounds(
    val minimum: Float,
    val maximum: Float,
)

internal fun longImageVerticalPanBounds(
    viewportWidthPx: Float,
    imageWidthToHeightRatio: Float,
    viewportHeightPx: Float,
    scale: Float,
): LongImageVerticalPanBounds {
    val contentHeight = longImageContentHeightPx(
        viewportWidthPx = viewportWidthPx,
        imageWidthToHeightRatio = imageWidthToHeightRatio,
        viewportHeightPx = viewportHeightPx,
        scale = scale,
    )
    return LongImageVerticalPanBounds(
        minimum = (viewportHeightPx - contentHeight).coerceAtMost(0f),
        maximum = 0f,
    )
}

@Composable
private fun RichText(
    nodes: List<BbNode>,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
) {
    val openLink = LocalLinkOpener.current
    val emotions = remember(nodes) { collectEmotionContent(nodes) }
    val inlineContent = emotions.mapValues { (_, emotion) ->
        InlineTextContent(
            placeholder = Placeholder(
                width = 32.sp,
                height = 32.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
            ),
        ) {
            Image(
                painter = painterResource(emotion.drawableId),
                contentDescription = emotion.description,
                modifier = Modifier.fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .then(
                        if (NgaEmotionResources.needsLightBackground(emotion.category)) {
                            Modifier.background(Color.White)
                        } else Modifier
                    ),
                contentScale = ContentScale.Fit,
            )
        }
    }
    val text = remember(nodes, emotions.keys) { buildRichText(nodes, emotions.keys) }
    var layoutResult by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    TrackedSelectionContainer {
        BasicText(
            text = text,
            inlineContent = inlineContent,
            modifier = Modifier.pointerInput(text) {
                detectTapGestures { position ->
                    val offset = layoutResult?.getOffsetForPosition(position) ?: return@detectTapGestures
                    text.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()?.item?.let(openLink)
                }
            },
            style = textStyle,
            onTextLayout = { layoutResult = it },
        )
    }
}

@Composable
private fun TrackedSelectionContainer(content: @Composable () -> Unit) {
    val generation = LocalSelectionGeneration.current
    val onActivated = LocalSelectionActivated.current
    val viewConfiguration = LocalViewConfiguration.current
    Box(
        Modifier.pointerInput(onActivated, viewConfiguration.longPressTimeoutMillis) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
                var moved = false
                val releasedBeforeTimeout = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                    var pressed = true
                    while (pressed) {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        val change = event.changes.firstOrNull { it.id == down.id }
                        pressed = change?.pressed == true
                        if (change != null && (change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                            moved = true
                            return@withTimeoutOrNull true
                        }
                    }
                    true
                }
                if (releasedBeforeTimeout == null && !moved) {
                    var pressed = true
                    while (pressed) {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        pressed = event.changes.any { it.id == down.id && it.pressed }
                    }
                    onActivated()
                }
            }
        },
    ) {
        key(generation) { SelectionContainer(content = content) }
    }
}

private data class EmotionContent(
    val category: String,
    val description: String,
    val drawableId: Int,
)

private fun emotionKey(category: String, code: String): String = "emotion:$category:$code"

private fun collectEmotionContent(nodes: List<BbNode>): Map<String, EmotionContent> = buildMap {
    fun collect(children: List<BbNode>) {
        children.forEach { node ->
            when (node) {
                is BbNode.Emotion -> NgaEmotionResources.resolve(node.category, node.code)?.let { drawableId ->
                    put(
                        emotionKey(node.category, node.code),
                        EmotionContent(node.category, "NGA 表情 ${node.code}", drawableId),
                    )
                }
                is BbNode.Bold -> collect(node.children)
                is BbNode.Italic -> collect(node.children)
                is BbNode.Underline -> collect(node.children)
                is BbNode.Strikethrough -> collect(node.children)
                is BbNode.Color -> collect(node.children)
                is BbNode.Span -> collect(node.children)
                is BbNode.Quote -> collect(node.children)
                is BbNode.Collapse -> collect(node.children)
                is BbNode.Warning -> collect(node.children)
                is BbNode.Album -> collect(node.children)
                is BbNode.Heading -> collect(node.children)
                is BbNode.Paragraph -> collect(node.children)
                is BbNode.Align -> collect(node.children)
                is BbNode.Subscript -> collect(node.children)
                is BbNode.Superscript -> collect(node.children)
                is BbNode.Link -> collect(node.children)
                is BbNode.BulletList -> node.items.forEach(::collect)
                is BbNode.Table -> node.rows.forEach { row -> row.forEach(::collect) }
                is BbNode.Text, is BbNode.Code, is BbNode.Image, is BbNode.Video, is BbNode.Attachment,
                BbNode.HorizontalRule -> Unit
            }
        }
    }
    collect(nodes)
}

private fun buildRichText(nodes: List<BbNode>, supportedEmotions: Set<String> = emptySet()) = buildAnnotatedString {
    fun appendNodes(children: List<BbNode>) {
        children.forEach { node ->
            when (node) {
                is BbNode.Text -> append(node.value)
                is BbNode.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { appendNodes(node.children) }
                is BbNode.Italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { appendNodes(node.children) }
                is BbNode.Underline -> withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { appendNodes(node.children) }
                is BbNode.Strikethrough -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { appendNodes(node.children) }
                is BbNode.Color -> withStyle(SpanStyle(color = parsePostColor(node.value))) { appendNodes(node.children) }
                is BbNode.Span -> appendNodes(node.children)
                is BbNode.Subscript -> withStyle(
                    SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 0.75.em),
                ) { appendNodes(node.children) }
                is BbNode.Superscript -> withStyle(
                    SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 0.75.em),
                ) { appendNodes(node.children) }
                is BbNode.Link -> {
                    pushStringAnnotation(tag = "URL", annotation = node.href)
                    withStyle(
                        SpanStyle(color = Color(0xFF3F51B5), textDecoration = TextDecoration.Underline),
                    ) { appendNodes(node.children) }
                    pop()
                }
                is BbNode.Emotion -> {
                    val key = emotionKey(node.category, node.code)
                    if (key in supportedEmotions) appendInlineContent(key, "[s:${node.category}:${node.code}]")
                    else append("[s:${node.category}:${node.code}]")
                }
                is BbNode.Image -> append(node.url)
                is BbNode.Video -> append(node.url)
                is BbNode.Attachment -> append(node.name)
                is BbNode.Quote -> appendNodes(node.children)
                is BbNode.Collapse -> appendNodes(node.children)
                is BbNode.Warning -> appendNodes(node.children)
                is BbNode.Album -> appendNodes(node.children)
                is BbNode.Heading -> appendNodes(node.children)
                is BbNode.Paragraph -> appendNodes(node.children)
                is BbNode.Align -> appendNodes(node.children)
                BbNode.HorizontalRule -> append('\n')
                is BbNode.Code -> append(node.value)
                is BbNode.BulletList -> node.items.forEach { item ->
                    append("• ")
                    appendNodes(item)
                    append('\n')
                }
                is BbNode.Table -> node.rows.forEach { row ->
                    row.forEachIndexed { index, cell ->
                        if (index > 0) append('\t')
                        appendNodes(cell)
                    }
                    append('\n')
                }
            }
        }
    }
    appendNodes(nodes)
}

private fun parsePostColor(value: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(value.trim()))
}.getOrDefault(Color.Unspecified)

private fun formatDate(seconds: Long): String {
    if (seconds <= 0) return "时间未知"
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(seconds * 1000))
}

internal fun formatCompactPostDate(seconds: Long): String {
    if (seconds <= 0) return "时间未知"
    return SimpleDateFormat("M月d日 HH:mm", Locale.getDefault()).format(Date(seconds * 1000))
}

internal fun formatTopicUpdateTime(seconds: Long, nowMillis: Long = System.currentTimeMillis()): String {
    if (seconds <= 0) return "时间未知"
    val elapsedSeconds = nowMillis / 1000 - seconds
    return when {
        elapsedSeconds in 0..59 -> "刚刚"
        elapsedSeconds in 60..3599 -> "${elapsedSeconds / 60}分钟前"
        elapsedSeconds in 3600..86399 -> "${elapsedSeconds / 3600}小时前"
        else -> SimpleDateFormat("M月d日", Locale.getDefault()).format(Date(seconds * 1000))
    }
}

internal fun replyCountColor(replies: Int, neutral: Color, accent: Color): Color {
    if (replies < 5) return neutral
    val start = lerp(neutral, accent, 0.38f)
    val progress = ((replies.coerceAtMost(60) - 5) / 55f).coerceIn(0f, 1f)
    return lerp(start, accent, progress)
}

internal fun formatPostTime(seconds: Long, nowMillis: Long = System.currentTimeMillis()): String {
    if (seconds <= 0) return "时间未知"
    val elapsedSeconds = nowMillis / 1000 - seconds
    return when {
        elapsedSeconds in 0..59 -> "刚刚"
        elapsedSeconds in 60..3599 -> "${elapsedSeconds / 60}分钟前"
        else -> formatDate(seconds)
    }
}
