package com.aki.lnga.data

import com.aki.lnga.model.NgaSession
import com.aki.lnga.model.ForumCategory
import com.aki.lnga.model.ForumBoard
import com.aki.lnga.model.Pagination
import com.aki.lnga.model.Post
import com.aki.lnga.model.ThreadPage
import com.aki.lnga.model.Topic
import com.aki.lnga.model.TopicPage
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject

internal class NgaRepository(private val client: NgaClient) {
    suspend fun loadForumCategories(session: NgaSession): List<ForumCategory> {
        val root = NgaJson.parse(
            client.get(
                path = "/app_api.php",
                query = mapOf("__lib" to "home", "__act" to "category"),
                session = session,
            ),
        )
        root.errorMessage()?.let { throw NgaApiException(it) }
        return NgaForumParser.parse(root).ifEmpty { throw NgaApiException("NGA 未返回可用版面") }
    }

    suspend fun searchForums(session: NgaSession, query: String): List<ForumBoard> {
        val key = query.trim()
        if (key.isBlank()) return emptyList()
        val root = NgaJson.parseElement(
            client.get(
                path = "/forum.php",
                query = mapOf("__output" to "8", "key" to key),
                session = session,
                diagnosticPurpose = "forum-search",
            ),
        )
        (root as? JsonObject)?.errorMessage()?.let { throw NgaApiException(it) }
        return NgaForumParser.parseSearch(root)
    }

    suspend fun verifySession(session: NgaSession): Unit {
        val root = NgaJson.parse(
            client.get(
                path = "/nuke.php",
                query = mapOf(
                    "__lib" to "ucp",
                    "__act" to "get",
                    "lite" to "js",
                    "noprefix" to "",
                    "uid" to session.uid,
                ),
                session = session,
            ),
        )
        root.errorMessage()?.let { message ->
            if (message.contains("未登录")) {
                throw NgaAuthenticationException(
                    "NGA 未识别这组登录凭据，请重新获取当前会话的 ngaPassportUid 和 ngaPassportCid",
                )
            }
            throw NgaApiException(message)
        }
        if (root.objectOrNull("data") == null) throw NgaApiException("NGA 未返回有效账号信息")
    }

    suspend fun loadUserIpLocation(session: NgaSession, uid: Long): String {
        if (uid <= 0) return ""
        val root = NgaJson.parse(
            client.get(
                path = "/nuke.php",
                query = mapOf(
                    "__lib" to "ucp",
                    "__act" to "get",
                    "lite" to "js",
                    "noprefix" to "",
                    "uid" to uid.toString(),
                ),
                session = session,
            ),
        )
        root.errorMessage()?.let { throw NgaApiException(it) }
        val data = root.objectOrNull("data") ?: return ""
        val profile = data.objectOrNull("0") ?: data
        return profile["ipLoc"].text()
    }

    suspend fun loadUserProfile(session: NgaSession, uid: Long): UserProfile {
        if (uid <= 0) throw NgaApiException("匿名用户没有可查看的公开资料")
        val root = NgaJson.parse(
            client.get(
                path = "/nuke.php",
                query = mapOf(
                    "__lib" to "ucp", "__act" to "get", "lite" to "js", "noprefix" to "",
                    "uid" to uid.toString(),
                ),
                session = session,
            ),
        )
        root.errorMessage()?.let { throw NgaApiException(it) }
        val data = root.objectOrNull("data") ?: throw NgaApiException("NGA 未返回用户资料")
        val profile = data.objectOrNull("0") ?: data
        return UserProfile(
            uid = profile["uid"].long(uid),
            userName = decodeAnonymousName(profile["username"].text()).ifBlank { "用户 $uid" },
            avatarUrl = normalizeAvatarUrl(profile["avatar"].text()),
            group = profile["group"].text(),
            title = profile["title"].text(),
            honor = profile["honor"].text(),
            reputation = profile["yz"].int(),
            posts = profile["posts"].text(),
            registeredAtSeconds = profile["regdate"].long(),
            signature = sanitizeUserReply(profile["sign"].text()),
            ipLocation = profile["ipLoc"].text(),
            followingCount = profile["follow_num"].text(),
            followerCount = profile["follow_by_num"].text(),
        )
    }

    suspend fun loadUserActivityAnalysis(
        session: NgaSession,
        uid: Long,
        maxPagesPerType: Int = 3,
        maxRecordsPerType: Int = 100,
    ): UserActivityAnalysis {
        if (uid <= 0) throw NgaApiException("匿名用户没有可分析的公开记录")
        val topics = loadUserActivityRecords(session, uid, replies = false, maxPagesPerType, maxRecordsPerType)
        val replies = loadUserActivityRecords(session, uid, replies = true, maxPagesPerType, maxRecordsPerType)
        return aggregateUserActivity(
            topics = topics.records,
            replies = replies.records,
            loadIssues = listOfNotNull(topics.issue, replies.issue),
        )
    }

    private data class ActivityRecordsResult(
        val records: List<UserActivityRecord>,
        val issue: UserActivityLoadIssue? = null,
    )

    private suspend fun loadUserActivityRecords(
        session: NgaSession,
        uid: Long,
        replies: Boolean,
        maxPages: Int,
        maxRecords: Int,
    ): ActivityRecordsResult {
        val records = mutableListOf<UserActivityRecord>()
        var page = 1
        var totalPages = 1
        while (page <= totalPages && page <= maxPages && records.size < maxRecords) {
            val query = mutableMapOf(
                "page" to page.toString(), "lite" to "js", "noprefix" to "", "authorid" to uid.toString(),
            )
            if (replies) query["searchpost"] = "1"
            val section = if (replies) "回复" else "主题"
            val response = try {
                client.get(
                    path = "/thread.php",
                    query = query,
                    session = session,
                    diagnosticPurpose = "user-activity-${if (replies) "replies" else "topics"}-page-$page",
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                return ActivityRecordsResult(
                    records = distinctActivityRecords(records, replies, maxRecords),
                    issue = UserActivityLoadIssue(
                        section = section,
                        page = page,
                        message = userActivityFailureMessage(error, section, page, records.isNotEmpty()),
                    ),
                )
            }
            val root = try {
                NgaJson.parse(response)
            } catch (error: Throwable) {
                return ActivityRecordsResult(
                    records = distinctActivityRecords(records, replies, maxRecords),
                    issue = UserActivityLoadIssue(
                        section = section,
                        page = page,
                        message = "$section${if (records.isNotEmpty()) "第 ${page} 页" else "记录"}返回格式异常",
                    ),
                )
            }
            root.errorMessage()?.let { message ->
                return ActivityRecordsResult(
                    records = distinctActivityRecords(records, replies, maxRecords),
                    issue = UserActivityLoadIssue(
                        section = section,
                        page = page,
                        message = when {
                            message.contains("未登录") -> "${section}记录加载失败：登录状态已失效"
                            else -> "${section}记录当前不可用，请稍后重试"
                        },
                    ),
                )
            }
            val data = root.objectOrNull("data") ?: break
            val rows = data.objectOrNull("__T").orEmpty().values.mapNotNull { value ->
                val item = value as? JsonObject ?: return@mapNotNull null
                val tid = item["tid"].long()
                val subject = item["subject"].text()
                if (tid <= 0 || item["authorid"].long() == 0L || subject.contains("权限不足") ||
                    subject.contains("帖子发布或回复时间超过限制")) return@mapNotNull null
                val post = item.objectOrNull("__P")
                val timestamp = if (replies) post?.get("postdate").long(item["postdate"].long())
                    else item["postdate"].long()
                UserActivityRecord(
                    tid = tid,
                    subject = subject,
                    forumName = item.objectOrNull("parent")?.get("2").text(),
                    timestampSeconds = timestamp,
                    replySnippet = if (replies) sanitizeUserReply(post?.get("content").text()) else "",
                )
            }
            records += rows
            val totalRows = data["__ROWS"].int(records.size)
            val rowsPerPage = data["__T__ROWS_PAGE"].int(35).coerceAtLeast(1)
            totalPages = ((totalRows + rowsPerPage - 1) / rowsPerPage).coerceAtLeast(1)
            if (rows.isEmpty()) break
            page++
        }
        return ActivityRecordsResult(distinctActivityRecords(records, replies, maxRecords))
    }

    private fun distinctActivityRecords(
        records: List<UserActivityRecord>,
        replies: Boolean,
        maxRecords: Int,
    ): List<UserActivityRecord> = records
        .distinctBy { record ->
            if (replies) "${record.tid}:${record.timestampSeconds}:${record.replySnippet}" else record.tid.toString()
        }
        .take(maxRecords)

    private fun userActivityFailureMessage(
        error: Throwable,
        section: String,
        page: Int,
        hasPartialRecords: Boolean,
    ): String {
        val target = if (hasPartialRecords) "${section}第 ${page} 页" else "${section}记录"
        return when (error) {
            is NgaTemporaryUnavailableException -> "$target 暂时不可用（HTTP ${error.statusCode}）"
            is NgaAuthenticationException -> "$target 加载失败：登录状态已失效"
            is NgaContentUnavailableException -> "$target 当前不可见或没有访问权限"
            else -> "$target 加载失败，请稍后重试"
        }
    }

    suspend fun loadTopics(
        session: NgaSession,
        fid: String,
        stid: String,
        page: Int,
        orderBy: String = "",
    ): TopicPage {
        val boardQuery = if (stid.isNotBlank()) mapOf("stid" to stid) else mapOf("fid" to fid)
        val sortQuery = orderBy.takeIf(String::isNotBlank)?.let { mapOf("order_by" to it) }.orEmpty()
        val root = NgaJson.parse(
            client.get(
                path = "/thread.php",
                query = boardQuery + sortQuery +
                    mapOf("page" to page.toString(), "lite" to "js", "noprefix" to ""),
                session = session,
                diagnosticPurpose = "topic-list-page-$page${orderBy.takeIf(String::isNotBlank)?.let { "-$it" }.orEmpty()}",
            ),
        )
        root.errorMessage()?.let { throw NgaApiException(it) }
        val data = root.objectOrNull("data") ?: throw NgaApiException("主题列表缺少 data")
        val topicMap = data.objectOrNull("__T").orEmpty()
        val topics = topicMap.values.mapNotNull { value ->
            val item = value as? JsonObject ?: return@mapNotNull null
            val tid = item["tid"].long()
            if (tid <= 0) return@mapNotNull null
            Topic(
                tid = tid,
                fid = item["fid"].long(),
                subject = item["subject"].text("（无标题）"),
                author = decodeAnonymousName(item["author"].text()).ifBlank { "未知用户" },
                authorId = item["authorid"].long(),
                replies = item["replies"].int(),
                lastPoster = item["lastposter"].text(),
                postDateSeconds = item["postdate"].long(),
                lastPostDateSeconds = item["lastpost"].long(item["postdate"].long()),
                titleFont = item["titlefont"].int(),
                type = item["type"].int(),
                boardName = item.objectOrNull("parent")?.get("2").text(),
                previewImages = TopicPreviewParser.parse(item),
            ).takeUnless(Topic::isAlwaysHidden)
        }
        val rows = data["__ROWS"].int(topics.size)
        val rowsPerPage = data["__T__ROWS_PAGE"].int(35).coerceAtLeast(1)
        return TopicPage(
            forumName = data.objectOrNull("__F")?.get("name").text("版面 ${stid.ifBlank { fid }}"),
            topics = topics,
            pagination = Pagination(page, ((rows + rowsPerPage - 1) / rowsPerPage).coerceAtLeast(1), rows),
            serverTimeSeconds = root["time"].long(),
        )
    }

    suspend fun loadThread(session: NgaSession, tid: Long, page: Int): ThreadPage {
        try {
            val root = NgaJson.parse(
                client.get(
                    path = "/read.php",
                    query = mapOf(
                        "tid" to tid.toString(),
                        "page" to page.toString(),
                        "__output" to "8",
                        "noprefix" to "",
                        "v2" to "",
                    ),
                    session = session,
                ),
            )
            return parseThreadJson(root, tid, page).withInjectedQuotes()
        } catch (error: NgaAuthenticationException) {
            throw error
        } catch (error: NgaContentUnavailableException) {
            throw error
        } catch (_: Exception) {
            val html = client.getHtml(
                path = "/read.php",
                query = mapOf(
                    "tid" to tid.toString(),
                    "page" to page.toString(),
                ),
                session = session,
            )
            return NgaHtmlThreadParser.parse(html, tid, page).withInjectedQuotes()
        }
    }

    suspend fun loadTopicPreview(session: NgaSession, tid: Long): List<String> {
        val root = NgaJson.parse(
            client.get(
                path = "/read.php",
                query = mapOf(
                    "tid" to tid.toString(),
                    "page" to "1",
                    "__output" to "8",
                    "noprefix" to "",
                    "v2" to "",
                ),
                session = session,
            ),
        )
        root.errorMessage()?.let { throw NgaApiException(it) }
        val rows = root.objectOrNull("data")?.objectOrNull("__R").orEmpty()
        val firstPost = rows.values.asSequence()
            .mapNotNull { it as? JsonObject }
            .minByOrNull { it["lou"].int(Int.MAX_VALUE) }
            ?: return emptyList()
        return TopicPreviewParser.parse(firstPost)
    }

    private fun parseThreadJson(root: JsonObject, tid: Long, page: Int): ThreadPage {
        root.errorMessage()?.let { throw NgaApiException(it) }
        val data = root.objectOrNull("data") ?: throw NgaApiException("帖子详情缺少 data")
        val rowMap = data.objectOrNull("__R").orEmpty()
        val users = data.objectOrNull("__U")
        val posts = rowMap.values.mapNotNull { value ->
            (value as? JsonObject)?.let { mapPost(it, users, tid) }
        }
        val rows = data["__ROWS"].int(posts.size)
        val rowsPerPage = data["__R__ROWS_PAGE"].int(20).coerceAtLeast(20)
        val first = posts.firstOrNull()
        val topic = data.objectOrNull("__T")
        return ThreadPage(
            title = topic?.get("subject").text().ifBlank {
                first?.subject?.ifBlank { "主题 $tid" } ?: "主题 $tid"
            },
            forumName = data.objectOrNull("__F")?.get("name").text(),
            posts = posts,
            pagination = Pagination(page, ((rows + rowsPerPage - 1) / rowsPerPage).coerceAtLeast(1), rows),
        )
    }

    private fun mapPost(item: JsonObject, users: JsonObject?, fallbackTid: Long): Post? {
        val pid = item["pid"].long()
        val floor = item["lou"].int(-1)
        if (pid < 0 || (pid == 0L && floor != 0) ||
            (item["comment_to_id"] != null && item["content"] == null)
        ) return null
        val user = resolveNgaUser(item, users)
        val comments = item.objectOrNull("comment").orEmpty().values.mapNotNull { value ->
            (value as? JsonObject)?.let { mapPost(it, users, fallbackTid) }
        }
        return Post(
            tid = item["tid"].long(fallbackTid),
            fid = item["fid"].long(),
            pid = pid,
            floor = floor,
            author = user.name,
            authorId = item["authorid"].long(),
            subject = item["subject"].text(),
            content = item["content"].text(),
            postDateSeconds = item["postdatetimestamp"].long(item["postdate"].long()),
            avatarUrl = user.avatarUrl,
            isAnonymous = user.isAnonymous,
            score = item["score"].int(),
            scoreDown = item["score_2"].int(),
            attachments = NgaAttachmentParser.parse(item["attachs"]),
            comments = comments,
        )
    }

    private fun ThreadPage.withInjectedQuotes(): ThreadPage = copy(posts = NgaQuoteInjector.inject(posts))
}

private fun JsonObject.errorMessage(): String? {
    val error = this["error"] ?: return null
    if (error is JsonObject) return error.values.firstOrNull().text("NGA 返回业务错误")
    return error.text().ifBlank { null }
}

internal class NgaApiException(message: String) : Exception(message)
