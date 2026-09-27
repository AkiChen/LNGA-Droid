package com.aki.lnga.data

import com.aki.lnga.model.ForumBoard
import com.aki.lnga.model.ForumCategory
import com.aki.lnga.model.ForumGroup
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

internal object NgaForumParser {
    fun parseSearch(root: JsonElement): List<ForumBoard> {
        val payload = when (root) {
            is JsonArray -> root
            is JsonObject -> root["data"] ?: root["result"]
            else -> null
        }
        val values = elements(payload)
        return values.mapNotNull { value ->
            val board = value as? JsonObject ?: return@mapNotNull null
            val fid = board["fid"].int()
            val stid = board["stid"].int(board["id"].int())
            val name = board["name"].text().trim()
            if (name.isBlank() || fid == 0 && stid == 0) return@mapNotNull null
            val boardId = fid.takeIf { it != 0 } ?: stid
            ForumBoard(
                fid = fid,
                stid = stid,
                name = name,
                info = board["info"].text().trim(),
                iconUrl = board["icon"].text().ifBlank {
                    "https://img4.nga.cn/ngabbs/nga_classic/f/app/$boardId.png"
                },
            )
        }.distinctBy(ForumBoard::preferenceKey)
    }

    fun parse(root: JsonObject): List<ForumCategory> {
        val data = root.objectOrNull("data")
        val result = root["result"] ?: data?.get("result") ?: return emptyList()
        return elements(result).mapNotNull { categoryValue ->
            val category = categoryValue as? JsonObject ?: return@mapNotNull null
            val groups = elements(category["groups"]).mapNotNull { groupValue ->
                val group = groupValue as? JsonObject ?: return@mapNotNull null
                val boards = elements(group["forums"]).mapNotNull { boardValue ->
                    val board = boardValue as? JsonObject ?: return@mapNotNull null
                    val fid = board["fid"].int()
                    val stid = board["id"].int()
                    val name = board["name"].text().trim()
                    if (name.isBlank() || (fid == 0 && stid == 0)) return@mapNotNull null
                    val boardId = fid.takeIf { it != 0 } ?: stid
                    ForumBoard(
                        fid = fid,
                        stid = stid,
                        name = name,
                        info = board["info"].text().trim(),
                        iconUrl = board["icon"].text().ifBlank {
                            "https://img4.nga.cn/ngabbs/nga_classic/f/app/$boardId.png"
                        },
                    )
                }.distinctBy(ForumBoard::preferenceKey)
                if (boards.isEmpty()) null else ForumGroup(group["name"].text().trim(), boards)
            }
            if (groups.isEmpty()) null else ForumCategory(
                id = category["_id"].text(category["id"].text()),
                name = category["name"].text("其他版面").trim(),
                groups = groups,
            )
        }
    }

    private fun elements(value: JsonElement?): Collection<JsonElement> = when (value) {
        is JsonArray -> value
        is JsonObject -> value.values
        else -> emptyList()
    }
}
