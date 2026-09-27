package com.aki.lnga.data

import kotlinx.serialization.json.JsonObject

internal data class NgaUserPresentation(
    val name: String,
    val avatarUrl: String,
    val isAnonymous: Boolean,
)

internal fun resolveNgaUser(row: JsonObject, users: JsonObject?): NgaUserPresentation {
    val authorId = row["authorid"].long()
    val user = users?.get(authorId.toString()) as? JsonObject
    val encodedName = row["author"].text().ifBlank { user?.get("username").text() }
    val isAnonymous = row["isanonymous"].bool() || encodedName.startsWith("#anony_")
    val avatar = row["js_escap_avatar"].text().ifBlank { user?.get("avatar").text() }
    return NgaUserPresentation(
        name = decodeAnonymousName(encodedName).ifBlank { "未知用户" },
        avatarUrl = normalizeAvatarUrl(avatar),
        isAnonymous = isAnonymous,
    )
}

internal fun decodeAnonymousName(encoded: String): String {
    if (!encoded.startsWith("#anony_")) return encoded
    var offset = 6
    return buildString {
        repeat(6) { index ->
            val position = if (index == 0 || index == 3) {
                encoded.getOrNull(offset + 1)?.digitToIntOrNull(16)
            } else {
                encoded.substringOrNull(offset, offset + 2)?.toIntOrNull(16)
            }
            val alphabet = if (index == 0 || index == 3) PRIMARY_CHARS else SECONDARY_CHARS
            position?.takeIf { it in alphabet.indices }?.let { append(alphabet[it]) }
            offset += 2
        }
    }.ifBlank { "匿名" }
}

internal fun normalizeAvatarUrl(raw: String): String {
    val value = raw.trim().replace("\\/", "/")
    if (value.isBlank()) return ""
    val embedded = Regex("https?://[^\\\"'\\s}]+").find(value)?.value
    if (embedded != null) return embedded
    return when {
        value.startsWith("//") -> "https:$value"
        value.startsWith('/') -> "https://img.nga.cn$value"
        else -> "https://img4.nga.cn/ngabbs/nga_classic/avatar/$value"
    }
}

private fun String.substringOrNull(start: Int, end: Int): String? =
    takeIf { start >= 0 && end <= length }?.substring(start, end)

private const val PRIMARY_CHARS = "甲乙丙丁戊己庚辛壬癸子丑寅卯辰巳午未申酉戌亥"
private const val SECONDARY_CHARS =
    "王李张刘陈杨黄吴赵周徐孙马朱胡林郭何高罗郑梁谢宋唐许邓冯韩曹曾彭萧蔡潘田董袁于余叶蒋杜苏魏程吕丁沈任姚卢傅钟姜崔谭廖范汪陆金石戴贾韦夏邱方侯邹熊孟秦白江阎薛尹段雷黎史龙陶贺顾毛郝龚邵万钱严赖覃洪武莫孔汤向常温康施文牛樊葛邢安齐易乔伍庞颜倪庄聂章鲁岳翟殷詹申欧耿关兰焦俞左柳甘祝包宁尚符舒阮柯纪梅童凌毕单季裴霍涂成苗谷盛曲翁冉骆蓝路游辛靳管柴蒙鲍华喻祁蒲房滕屈饶解牟艾尤阳时穆农司卓古吉缪简车项连芦麦褚娄窦戚岑景党宫费卜冷晏席卫米柏宗瞿桂全佟应臧闵苟邬边卞姬师和仇栾隋商刁沙荣巫寇桑郎甄丛仲虞敖巩明佘池查麻苑迟邝"
