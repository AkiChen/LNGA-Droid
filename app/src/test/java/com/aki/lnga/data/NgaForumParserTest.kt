package com.aki.lnga.data

import org.junit.Assert.assertEquals
import org.junit.Test

class NgaForumParserTest {
    @Test
    fun parsesCategoryGroupAndBoardTree() {
        val root = NgaJson.parse(
            """{"result":{"1":{"_id":"1","name":"综合讨论","groups":[{"name":"热门", "forums":[{"fid":-7,"id":0,"name":"大漩涡","info":"综合版面"},{"fid":0,"id":123,"name":"专题版","info":""}]}]}}}""",
        )

        val categories = NgaForumParser.parse(root)

        assertEquals(1, categories.size)
        assertEquals("综合讨论", categories.single().name)
        assertEquals("热门", categories.single().groups.single().name)
        assertEquals(-7, categories.single().groups.single().boards[0].fid)
        assertEquals(123, categories.single().groups.single().boards[1].stid)
    }

    @Test
    fun parsesRealApiArrayShapeAndStringFid() {
        val root = NgaJson.parse(
            """{"code":0,"result":[{"id":0,"_id":"other","name":"网事杂谈","groups":[{"name":"网事杂谈","forums":[{"fid":"-7955747","id":"-7955747","name":"晴风村","info":"情感生活"}]}]}]}""",
        )

        val board = NgaForumParser.parse(root).single().groups.single().boards.single()

        assertEquals(-7955747, board.fid)
        assertEquals("晴风村", board.name)
        assertEquals(
            "https://img4.nga.cn/ngabbs/nga_classic/f/app/-7955747.png",
            board.iconUrl,
        )
    }

    @Test
    fun parsesDataWrappedNumericKeyObjects() {
        val root = NgaJson.parse(
            """{"data":{"result":{"0":{"_id":"other","name":"分类","groups":{"0":{"name":"分组","forums":{"0":{"fid":1,"id":1,"name":"版面"}}}}}}}}""",
        )

        assertEquals(1, NgaForumParser.parse(root).single().groups.single().boards.single().fid)
    }

    @Test
    fun ignoresEmptyAndInvalidBoards() {
        val root = NgaJson.parse(
            """{"result":{"1":{"name":"空分类","groups":[{"name":"空组","forums":[{"fid":0,"id":0,"name":"无效"}]}]}}}""",
        )

        assertEquals(emptyList<Any>(), NgaForumParser.parse(root))
    }

    @Test
    fun removesDuplicateBoardsWithinTheSameGroup() {
        val root = NgaJson.parse(
            """{"result":[{"_id":"","name":"分类","groups":[{"name":"分组","forums":[{"fid":1,"id":1,"name":"版面 A"},{"fid":1,"id":1,"name":"版面 A 重复"},{"fid":0,"id":2,"name":"专题版"}]}]}]}""",
        )

        val boards = NgaForumParser.parse(root).single().groups.single().boards

        assertEquals(listOf("版面 A", "专题版"), boards.map { it.name })
    }

    @Test
    fun parsesBareServerSearchArrayIncludingHiddenBoard() {
        val root = NgaJson.parseElement(
            """[{"fid":"-152678","stid":0,"name":"[股市]技术分析","info":"股票技术讨论"}]""",
        )

        val board = NgaForumParser.parseSearch(root).single()

        assertEquals(-152678, board.fid)
        assertEquals("[股市]技术分析", board.name)
        assertEquals("股票技术讨论", board.info)
    }

    @Test
    fun parsesWrappedServerSearchAndRemovesDuplicates() {
        val root = NgaJson.parseElement(
            """{"data":[{"fid":1,"name":"版面"},{"fid":1,"name":"重复版面"}]}""",
        )

        assertEquals(listOf("版面"), NgaForumParser.parseSearch(root).map { it.name })
    }

    @Test
    fun parsesLegacyNumericKeySearchObject() {
        val root = NgaJson.parseElement(
            """{"data":{"0":{"fid":-152678,"name":"[股市]技术分析"},"1":{"fid":-7,"name":"大漩涡"}}}""",
        )

        assertEquals(
            listOf("[股市]技术分析", "大漩涡"),
            NgaForumParser.parseSearch(root).map { it.name },
        )
    }
}
