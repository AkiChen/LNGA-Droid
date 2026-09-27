package com.aki.lnga.data

import android.content.Context
import com.aki.lnga.model.ForumBoard
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

internal const val DEFAULT_DOUBLE_SWIPE_INTERVAL_MILLIS = 850
internal const val MIN_DOUBLE_SWIPE_INTERVAL_MILLIS = 400
internal const val MAX_DOUBLE_SWIPE_INTERVAL_MILLIS = 1500
internal const val DEFAULT_DOUBLE_SWIPE_DISTANCE_DP = 44
internal const val MIN_DOUBLE_SWIPE_DISTANCE_DP = 24
internal const val MAX_DOUBLE_SWIPE_DISTANCE_DP = 96

internal enum class PageControlPosition {
    Left, Right;

    companion object {
        fun fromStored(value: String?): PageControlPosition =
            entries.firstOrNull { it.name == value } ?: Right
    }
}

internal enum class FontScaleOption(val factor: Float, val displayName: String) {
    Small(0.9f, "较小"),
    Standard(1f, "标准"),
    Large(1.15f, "较大"),
    ExtraLarge(1.3f, "特大");

    companion object {
        fun fromStored(value: String?): FontScaleOption =
            entries.firstOrNull { it.name == value } ?: Standard
    }
}

internal enum class ImageLoadingMode(val displayName: String) {
    Always("始终自动加载"),
    Manual("手动点击加载"),
    WifiOnly("仅 Wi-Fi 自动加载");

    companion object {
        fun fromStored(value: String?): ImageLoadingMode =
            entries.firstOrNull { it.name == value } ?: Always
    }
}

internal class BoardPreferenceStore(context: Context) {
    private val preferences = context.getSharedPreferences("board_preferences", Context.MODE_PRIVATE)

    fun favoriteKeys(): List<String> = preferences.getString(KEY_FAVORITES, "")
        .orEmpty()
        .split(SEPARATOR)
        .filter(String::isNotBlank)
        .distinct()

    fun favoriteBoards(): List<ForumBoard> {
        val stored = storedFavoriteBoards().associateBy(ForumBoard::preferenceKey)
        return favoriteKeys().map { key -> stored[key] ?: favoriteBoardFallback(key) }.filterNotNull()
    }

    fun missingFavoriteKeys(): List<String> {
        val storedKeys = storedFavoriteBoards().mapTo(mutableSetOf(), ForumBoard::preferenceKey)
        return favoriteKeys().filterNot(storedKeys::contains)
    }

    fun toggleFavorite(board: ForumBoard): List<String> {
        val key = board.preferenceKey()
        val current = favoriteKeys().toMutableList()
        val snapshots = storedFavoriteBoards().associateBy(ForumBoard::preferenceKey).toMutableMap()
        if (current.remove(key)) snapshots.remove(key) else {
            current += key
            snapshots[key] = board
        }
        save(current)
        saveFavoriteBoards(current.mapNotNull(snapshots::get))
        return current
    }

    fun rememberFavoriteBoards(boards: List<ForumBoard>): List<ForumBoard> {
        val favoriteKeys = favoriteKeys()
        val favoriteSet = favoriteKeys.toSet()
        val snapshots = storedFavoriteBoards().associateBy(ForumBoard::preferenceKey).toMutableMap()
        boards.filter { it.preferenceKey() in favoriteSet }.forEach { snapshots[it.preferenceKey()] = it }
        val ordered = favoriteKeys.mapNotNull(snapshots::get)
        saveFavoriteBoards(ordered)
        return ordered
    }

    fun moveFavorite(key: String, offset: Int): List<String> {
        val reordered = moveItem(favoriteKeys(), key, offset)
        save(reordered)
        return reordered
    }

    fun boardIconSelections(): Map<String, String> = preferences.all.asSequence()
        .filter { (key, value) -> key.startsWith(KEY_BOARD_ICON_PREFIX) && value is String }
        .associate { (key, value) ->
            key.removePrefix(KEY_BOARD_ICON_PREFIX) to value.toString()
        }

    fun setBoardIconSelection(board: ForumBoard, selection: String?): Map<String, String> {
        val key = "$KEY_BOARD_ICON_PREFIX${board.preferenceKey()}"
        preferences.edit().run {
            if (selection.isNullOrBlank()) remove(key) else putString(key, selection)
        }.apply()
        return boardIconSelections()
    }

    fun topicSubBoardIcons(): Boolean = preferences.getBoolean(KEY_TOPIC_SUB_BOARD_ICONS, false)

    fun setTopicSubBoardIcons(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_TOPIC_SUB_BOARD_ICONS, enabled).apply()
    }

    fun hideTradingTopics(): Boolean = preferences.getBoolean(KEY_HIDE_TRADING_TOPICS, true)

    fun setHideTradingTopics(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_HIDE_TRADING_TOPICS, enabled).apply()
    }

    fun autoNextPage(): Boolean = preferences.getBoolean(KEY_AUTO_NEXT_PAGE, false)

    fun setAutoNextPage(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTO_NEXT_PAGE, enabled).apply()
    }

    fun doubleSwipeNextPage(): Boolean = preferences.getBoolean(KEY_DOUBLE_SWIPE_NEXT_PAGE, false)

    fun setDoubleSwipeNextPage(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_DOUBLE_SWIPE_NEXT_PAGE, enabled).apply()
    }

    fun doubleSwipeIntervalMillis(): Int = preferences.getInt(
        KEY_DOUBLE_SWIPE_INTERVAL_MILLIS,
        DEFAULT_DOUBLE_SWIPE_INTERVAL_MILLIS,
    ).coerceIn(MIN_DOUBLE_SWIPE_INTERVAL_MILLIS, MAX_DOUBLE_SWIPE_INTERVAL_MILLIS)

    fun setDoubleSwipeIntervalMillis(value: Int) {
        preferences.edit().putInt(
            KEY_DOUBLE_SWIPE_INTERVAL_MILLIS,
            value.coerceIn(MIN_DOUBLE_SWIPE_INTERVAL_MILLIS, MAX_DOUBLE_SWIPE_INTERVAL_MILLIS),
        ).apply()
    }

    fun doubleSwipeDistanceDp(): Int = preferences.getInt(
        KEY_DOUBLE_SWIPE_DISTANCE_DP,
        DEFAULT_DOUBLE_SWIPE_DISTANCE_DP,
    ).coerceIn(MIN_DOUBLE_SWIPE_DISTANCE_DP, MAX_DOUBLE_SWIPE_DISTANCE_DP)

    fun setDoubleSwipeDistanceDp(value: Int) {
        preferences.edit().putInt(
            KEY_DOUBLE_SWIPE_DISTANCE_DP,
            value.coerceIn(MIN_DOUBLE_SWIPE_DISTANCE_DP, MAX_DOUBLE_SWIPE_DISTANCE_DP),
        ).apply()
    }

    fun resumeLastRead(): Boolean = preferences.getBoolean(KEY_RESUME_LAST_READ, true)

    fun setResumeLastRead(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_RESUME_LAST_READ, enabled).apply()
    }

    fun pageControlPosition(): PageControlPosition =
        PageControlPosition.fromStored(preferences.getString(KEY_PAGE_CONTROL_POSITION, null))

    fun setPageControlPosition(position: PageControlPosition) {
        preferences.edit().putString(KEY_PAGE_CONTROL_POSITION, position.name).apply()
    }

    fun fontScale(): FontScaleOption = FontScaleOption.fromStored(preferences.getString(KEY_FONT_SCALE, null))

    fun setFontScale(option: FontScaleOption) {
        preferences.edit().putString(KEY_FONT_SCALE, option.name).apply()
    }

    fun imageLoadingMode(): ImageLoadingMode =
        ImageLoadingMode.fromStored(preferences.getString(KEY_IMAGE_LOADING_MODE, null))

    fun setImageLoadingMode(mode: ImageLoadingMode) {
        preferences.edit().putString(KEY_IMAGE_LOADING_MODE, mode.name).apply()
    }

    fun internalBrowserEnabled(): Boolean = preferences.getBoolean(KEY_INTERNAL_BROWSER, true)

    fun setInternalBrowserEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_INTERNAL_BROWSER, enabled).apply()
    }

    fun webLoginResetRequired(): Boolean = preferences.getBoolean(KEY_WEB_LOGIN_RESET_REQUIRED, false)

    fun setWebLoginResetRequired(required: Boolean) {
        preferences.edit().putBoolean(KEY_WEB_LOGIN_RESET_REQUIRED, required).apply()
    }

    private fun save(keys: List<String>) {
        preferences.edit().putString(KEY_FAVORITES, keys.joinToString(SEPARATOR)).apply()
    }

    private fun saveFavoriteBoards(boards: List<ForumBoard>) {
        preferences.edit().putString(KEY_FAVORITE_BOARDS, encodeFavoriteBoards(boards)).apply()
    }

    private fun storedFavoriteBoards(): List<ForumBoard> =
        decodeFavoriteBoards(preferences.getString(KEY_FAVORITE_BOARDS, null))

    private companion object {
        const val KEY_FAVORITES = "favorite_board_keys"
        const val KEY_FAVORITE_BOARDS = "favorite_board_snapshots_v1"
        const val KEY_HIDE_TRADING_TOPICS = "hide_trading_topics"
        const val KEY_AUTO_NEXT_PAGE = "auto_next_page"
        const val KEY_DOUBLE_SWIPE_NEXT_PAGE = "double_swipe_next_page"
        const val KEY_DOUBLE_SWIPE_INTERVAL_MILLIS = "double_swipe_interval_millis"
        const val KEY_DOUBLE_SWIPE_DISTANCE_DP = "double_swipe_distance_dp"
        const val KEY_RESUME_LAST_READ = "resume_last_read"
        const val KEY_PAGE_CONTROL_POSITION = "page_control_position"
        const val KEY_FONT_SCALE = "font_scale"
        const val KEY_IMAGE_LOADING_MODE = "image_loading_mode"
        const val KEY_INTERNAL_BROWSER = "internal_browser_enabled"
        const val KEY_WEB_LOGIN_RESET_REQUIRED = "web_login_reset_required"
        const val KEY_BOARD_ICON_PREFIX = "board_icon_"
        const val KEY_TOPIC_SUB_BOARD_ICONS = "topic_sub_board_icons"
        const val SEPARATOR = "|"
    }
}

internal fun encodeFavoriteBoards(boards: List<ForumBoard>): String = buildJsonArray {
    boards.forEach { board ->
        add(buildJsonObject {
            put("fid", JsonPrimitive(board.fid))
            put("stid", JsonPrimitive(board.stid))
            put("name", JsonPrimitive(board.name))
            put("info", JsonPrimitive(board.info))
            put("iconUrl", JsonPrimitive(board.iconUrl))
        })
    }
}.toString()

internal fun decodeFavoriteBoards(raw: String?): List<ForumBoard> {
    val values = runCatching { Json.parseToJsonElement(raw.orEmpty()) as? JsonArray }.getOrNull() ?: return emptyList()
    return values.mapNotNull { value ->
        val board = value as? JsonObject ?: return@mapNotNull null
        val fid = (board["fid"] as? JsonPrimitive)?.intOrNull ?: 0
        val stid = (board["stid"] as? JsonPrimitive)?.intOrNull ?: 0
        val name = (board["name"] as? JsonPrimitive)?.contentOrNull.orEmpty()
        if (name.isBlank() || fid == 0 && stid == 0) return@mapNotNull null
        ForumBoard(
            fid = fid,
            stid = stid,
            name = name,
            info = (board["info"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
            iconUrl = (board["iconUrl"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
        )
    }.distinctBy(ForumBoard::preferenceKey)
}

internal fun favoriteBoardFallback(key: String): ForumBoard? {
    val id = key.substringAfter(':', "").toIntOrNull() ?: return null
    return when {
        key.startsWith("fid:") && id != 0 -> ForumBoard(fid = id, stid = 0, name = "版面 $id")
        key.startsWith("stid:") && id != 0 -> ForumBoard(fid = 0, stid = id, name = "专题版面 $id")
        else -> null
    }
}

internal fun ForumBoard.preferenceKey(): String =
    if (fid != 0) "fid:$fid" else "stid:$stid"

internal fun moveItem(values: List<String>, value: String, offset: Int): List<String> {
    val from = values.indexOf(value)
    if (from < 0) return values
    val to = (from + offset).coerceIn(values.indices)
    if (from == to) return values
    return values.toMutableList().apply {
        removeAt(from)
        add(to, value)
    }
}
