package com.aki.lnga.data

import com.aki.lnga.model.Topic

private const val CRASH_REPORT_PREFIX = "iOS客户端闪退"
private val tradingBoardPrefixes = listOf("优惠信息", "二手交易")

internal fun Topic.isAlwaysHidden(): Boolean = subject.trimStart().startsWith(CRASH_REPORT_PREFIX)

internal fun Topic.isTradingBoardTopic(): Boolean {
    val normalized = boardName.trim().replace(Regex("\\s+"), " ")
    return tradingBoardPrefixes.any(normalized::startsWith)
}
