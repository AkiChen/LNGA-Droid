package com.aki.lnga.model

data class NgaSession(
    val uid: String,
    val cid: String,
)

data class ForumBoard(
    val fid: Int,
    val stid: Int,
    val name: String,
    val info: String = "",
    val iconUrl: String = "",
)

data class ForumGroup(
    val name: String,
    val boards: List<ForumBoard>,
)

data class ForumCategory(
    val id: String,
    val name: String,
    val groups: List<ForumGroup>,
)

data class Pagination(
    val page: Int,
    val totalPages: Int,
    val totalRows: Int,
)

data class Topic(
    val tid: Long,
    val fid: Long,
    val subject: String,
    val author: String,
    val authorId: Long,
    val replies: Int,
    val lastPoster: String,
    val postDateSeconds: Long,
    val lastPostDateSeconds: Long = postDateSeconds,
    val titleFont: Int = 0,
    val type: Int = 0,
    val boardName: String = "",
    val previewImages: List<String> = emptyList(),
)

data class TopicPage(
    val forumName: String,
    val topics: List<Topic>,
    val pagination: Pagination,
    val serverTimeSeconds: Long = 0,
)

data class Post(
    val tid: Long,
    val fid: Long,
    val pid: Long,
    val floor: Int,
    val author: String,
    val authorId: Long,
    val subject: String,
    val content: String,
    val postDateSeconds: Long,
    val avatarUrl: String = "",
    val isAnonymous: Boolean = false,
    val score: Int = 0,
    val scoreDown: Int = 0,
    val attachments: List<PostAttachment> = emptyList(),
    val comments: List<Post> = emptyList(),
)

data class PostAttachment(
    val url: String,
    val name: String,
    val sizeLabel: String = "",
)

data class ThreadPage(
    val title: String,
    val forumName: String,
    val posts: List<Post>,
    val pagination: Pagination,
)

sealed interface BbNode {
    data class Text(val value: String) : BbNode
    data class Bold(val children: List<BbNode>) : BbNode
    data class Italic(val children: List<BbNode>) : BbNode
    data class Underline(val children: List<BbNode>) : BbNode
    data class Strikethrough(val children: List<BbNode>) : BbNode
    data class Color(val value: String, val children: List<BbNode>) : BbNode
    data class Span(val children: List<BbNode>) : BbNode
    data class Quote(val children: List<BbNode>) : BbNode
    data class Collapse(val title: String, val children: List<BbNode>) : BbNode
    data class Warning(val children: List<BbNode>) : BbNode
    data class Album(val title: String, val children: List<BbNode>) : BbNode
    data class Heading(val children: List<BbNode>) : BbNode
    data class Paragraph(val children: List<BbNode>) : BbNode
    data class Align(val value: String, val children: List<BbNode>) : BbNode
    data class Subscript(val children: List<BbNode>) : BbNode
    data class Superscript(val children: List<BbNode>) : BbNode
    data object HorizontalRule : BbNode
    data class Code(val value: String) : BbNode
    data class BulletList(val items: List<List<BbNode>>) : BbNode
    data class Table(val rows: List<List<List<BbNode>>>) : BbNode
    data class Link(val href: String, val children: List<BbNode>) : BbNode
    data class Attachment(val url: String, val name: String) : BbNode
    data class Image(val url: String) : BbNode
    data class Video(val url: String) : BbNode
    data class Emotion(val category: String, val code: String) : BbNode
}
