package com.aki.lnga.ui

import androidx.compose.ui.graphics.Color

private val avatarColors = listOf(
    Color(0xFF8D4A3C),
    Color(0xFF8A5D17),
    Color(0xFF527A3A),
    Color(0xFF28776F),
    Color(0xFF3D6B9B),
    Color(0xFF6556A3),
    Color(0xFF8B4C7B),
    Color(0xFF6D5A50),
)

internal fun avatarColorIndex(authorId: Long, author: String): Int {
    val hash = if (authorId != 0L) {
        (authorId xor (authorId ushr 32)).toInt()
    } else {
        author.hashCode()
    }
    return Math.floorMod(hash, avatarColors.size)
}

internal fun defaultAvatarColor(authorId: Long, author: String): Color =
    avatarColors[avatarColorIndex(authorId, author)]
