package com.aki.lnga.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Engineering
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.SportsBasketball
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.TabletAndroid
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

internal const val BOARD_ICON_OFFICIAL = "official"
internal const val BOARD_ICON_PRESET_PREFIX = "preset:"

internal enum class BoardIconCategory(val displayName: String) {
    Technology("科技数码"),
    Knowledge("论坛知识"),
    Entertainment("影音娱乐"),
    Life("体育生活"),
    Shopping("购物出行"),
    Development("开发技术"),
}

internal enum class BoardIconPreset(
    val displayName: String,
    val category: BoardIconCategory,
) {
    Computer("电脑", BoardIconCategory.Technology),
    Memory("芯片", BoardIconCategory.Technology),
    Smartphone("手机", BoardIconCategory.Technology),
    Tablet("平板", BoardIconCategory.Technology),
    Storage("存储", BoardIconCategory.Technology),
    Router("路由器", BoardIconCategory.Technology),
    Wifi("网络", BoardIconCategory.Technology),
    Headphones("耳机", BoardIconCategory.Technology),

    Forum("论坛", BoardIconCategory.Knowledge),
    Chat("聊天", BoardIconCategory.Knowledge),
    Groups("社群", BoardIconCategory.Knowledge),
    School("教育", BoardIconCategory.Knowledge),
    History("历史", BoardIconCategory.Knowledge),
    Psychology("心理", BoardIconCategory.Knowledge),
    Science("科学", BoardIconCategory.Knowledge),
    Newspaper("新闻", BoardIconCategory.Knowledge),

    Gaming("游戏", BoardIconCategory.Entertainment),
    Movie("电影", BoardIconCategory.Entertainment),
    Music("音乐", BoardIconCategory.Entertainment),
    Camera("摄影", BoardIconCategory.Entertainment),
    Animation("动漫", BoardIconCategory.Entertainment),
    Sparkle("创意", BoardIconCategory.Entertainment),
    Casino("棋牌", BoardIconCategory.Entertainment),
    Trophy("赛事", BoardIconCategory.Entertainment),

    Soccer("足球", BoardIconCategory.Life),
    Basketball("篮球", BoardIconCategory.Life),
    Fitness("健身", BoardIconCategory.Life),
    Restaurant("美食", BoardIconCategory.Life),
    Cafe("饮品", BoardIconCategory.Life),
    Favorite("情感", BoardIconCategory.Life),
    Pets("宠物", BoardIconCategory.Life),
    Health("健康", BoardIconCategory.Life),

    Shopping("购物", BoardIconCategory.Shopping),
    Offer("优惠", BoardIconCategory.Shopping),
    Sell("交易", BoardIconCategory.Shopping),
    Paid("金融", BoardIconCategory.Shopping),
    Car("汽车", BoardIconCategory.Shopping),
    Motorcycle("摩托", BoardIconCategory.Shopping),
    Flight("航空", BoardIconCategory.Shopping),
    Travel("旅行", BoardIconCategory.Shopping),

    Code("代码", BoardIconCategory.Development),
    Terminal("终端", BoardIconCategory.Development),
    Build("工具", BoardIconCategory.Development),
    Engineering("工程", BoardIconCategory.Development),
    Security("安全", BoardIconCategory.Development),
    Bug("调试", BoardIconCategory.Development),
    Public("全球", BoardIconCategory.Development),
    Label("标签", BoardIconCategory.Development),
}

internal fun BoardIconPreset.vector(): ImageVector = when (this) {
    BoardIconPreset.Computer -> Icons.Rounded.Computer
    BoardIconPreset.Memory -> Icons.Rounded.Memory
    BoardIconPreset.Smartphone -> Icons.Rounded.Smartphone
    BoardIconPreset.Tablet -> Icons.Rounded.TabletAndroid
    BoardIconPreset.Storage -> Icons.Rounded.Storage
    BoardIconPreset.Router -> Icons.Rounded.Router
    BoardIconPreset.Wifi -> Icons.Rounded.Wifi
    BoardIconPreset.Headphones -> Icons.Rounded.Headphones
    BoardIconPreset.Forum -> Icons.Rounded.Forum
    BoardIconPreset.Chat -> Icons.Rounded.Chat
    BoardIconPreset.Groups -> Icons.Rounded.Groups
    BoardIconPreset.School -> Icons.Rounded.School
    BoardIconPreset.History -> Icons.Rounded.AccountBalance
    BoardIconPreset.Psychology -> Icons.Rounded.Psychology
    BoardIconPreset.Science -> Icons.Rounded.Science
    BoardIconPreset.Newspaper -> Icons.Rounded.Newspaper
    BoardIconPreset.Gaming -> Icons.Rounded.SportsEsports
    BoardIconPreset.Movie -> Icons.Rounded.Movie
    BoardIconPreset.Music -> Icons.Rounded.MusicNote
    BoardIconPreset.Camera -> Icons.Rounded.PhotoCamera
    BoardIconPreset.Animation -> Icons.Rounded.Animation
    BoardIconPreset.Sparkle -> Icons.Rounded.AutoAwesome
    BoardIconPreset.Casino -> Icons.Rounded.Casino
    BoardIconPreset.Trophy -> Icons.Rounded.EmojiEvents
    BoardIconPreset.Soccer -> Icons.Rounded.SportsSoccer
    BoardIconPreset.Basketball -> Icons.Rounded.SportsBasketball
    BoardIconPreset.Fitness -> Icons.Rounded.FitnessCenter
    BoardIconPreset.Restaurant -> Icons.Rounded.Restaurant
    BoardIconPreset.Cafe -> Icons.Rounded.LocalCafe
    BoardIconPreset.Favorite -> Icons.Rounded.Favorite
    BoardIconPreset.Pets -> Icons.Rounded.Pets
    BoardIconPreset.Health -> Icons.Rounded.HealthAndSafety
    BoardIconPreset.Shopping -> Icons.Rounded.ShoppingCart
    BoardIconPreset.Offer -> Icons.Rounded.LocalOffer
    BoardIconPreset.Sell -> Icons.Rounded.Sell
    BoardIconPreset.Paid -> Icons.Rounded.Paid
    BoardIconPreset.Car -> Icons.Rounded.DirectionsCar
    BoardIconPreset.Motorcycle -> Icons.Rounded.TwoWheeler
    BoardIconPreset.Flight -> Icons.Rounded.Flight
    BoardIconPreset.Travel -> Icons.Rounded.TravelExplore
    BoardIconPreset.Code -> Icons.Rounded.Code
    BoardIconPreset.Terminal -> Icons.Rounded.Terminal
    BoardIconPreset.Build -> Icons.Rounded.Build
    BoardIconPreset.Engineering -> Icons.Rounded.Engineering
    BoardIconPreset.Security -> Icons.Rounded.Security
    BoardIconPreset.Bug -> Icons.Rounded.BugReport
    BoardIconPreset.Public -> Icons.Rounded.Public
    BoardIconPreset.Label -> Icons.Rounded.Label
}

internal fun storedBoardIconPreset(value: String?): BoardIconPreset? = value
    ?.takeIf { it.startsWith(BOARD_ICON_PRESET_PREFIX) }
    ?.removePrefix(BOARD_ICON_PRESET_PREFIX)
    ?.let { name -> BoardIconPreset.entries.firstOrNull { it.name == name } }

internal fun BoardIconPreset.storedValue(): String = "$BOARD_ICON_PRESET_PREFIX$name"

internal fun automaticBoardIconPreset(name: String): BoardIconPreset {
    val normalized = name.lowercase()
    val mappings = listOf(
        listOf("pc软硬件", "硬件", "装机", "显卡", "电脑") to BoardIconPreset.Computer,
        listOf("手机", "数码", "消费电子") to BoardIconPreset.Smartphone,
        listOf("网络", "路由", "宽带") to BoardIconPreset.Router,
        listOf("耳机", "音响", "音频") to BoardIconPreset.Headphones,
        listOf("it杂谈", "杂谈", "论坛") to BoardIconPreset.Forum,
        listOf("历史") to BoardIconPreset.History,
        listOf("新闻", "时事") to BoardIconPreset.Newspaper,
        listOf("影音", "电影", "电视") to BoardIconPreset.Movie,
        listOf("音乐") to BoardIconPreset.Music,
        listOf("摄影", "相机") to BoardIconPreset.Camera,
        listOf("动漫", "二次元") to BoardIconPreset.Animation,
        listOf("生命之杯", "足球") to BoardIconPreset.Soccer,
        listOf("篮球") to BoardIconPreset.Basketball,
        listOf("体育", "赛事") to BoardIconPreset.Trophy,
        listOf("游戏", "主机", "手游") to BoardIconPreset.Gaming,
        listOf("晴风村", "情感", "生活") to BoardIconPreset.Favorite,
        listOf("美食", "烹饪") to BoardIconPreset.Restaurant,
        listOf("宠物") to BoardIconPreset.Pets,
        listOf("健康", "医疗") to BoardIconPreset.Health,
        listOf("优惠", "购物指南") to BoardIconPreset.Offer,
        listOf("二手", "交易") to BoardIconPreset.Sell,
        listOf("汽车") to BoardIconPreset.Car,
        listOf("摩托") to BoardIconPreset.Motorcycle,
        listOf("旅行", "旅游", "出行") to BoardIconPreset.Travel,
        listOf("编程", "开发", "代码") to BoardIconPreset.Code,
        listOf("安全") to BoardIconPreset.Security,
    )
    return mappings.firstOrNull { (keywords, _) -> keywords.any(normalized::contains) }?.second
        ?: BoardIconPreset.Label
}
