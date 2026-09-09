package com.mustafanabeel.accpqbank2026

import com.mustafanabeel.accpqbank2026.data.model.QBankJsonRoot
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.zip.GZIPInputStream

class QuestionBankIntegrityTest {
    @Test
    fun `bundled question bank is complete and canonical`() {
        val candidates = listOf(
            File("src/main/assets/questions.json.gz"),
            File("app/src/main/assets/questions.json.gz")
        )
        val asset = candidates.firstOrNull { it.isFile }
            ?: error("questions.json.gz was not found")
        val bytes = GZIPInputStream(FileInputStream(asset)).use { it.readBytes() }

        val hash = MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        assertEquals(EXPECTED_SHA256, hash)

        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val root = requireNotNull(
            moshi.adapter(QBankJsonRoot::class.java)
                .fromJson(bytes.toString(Charsets.UTF_8))
        )

        assertEquals(22, root.chapters.size)
        assertEquals(533, root.questions.size)
        assertEquals(533, root.questions.map { it.id }.distinct().size)
        assertEquals(531, root.questions.count { it.ocrStatus != "source_missing" })
        assertEquals(258, root.questions.count { it.type == "assessment" })
        assertEquals(275, root.questions.count { it.type == "case" })

        val chapterIds = root.chapters.map { it.id }.toSet()
        assertTrue(root.questions.all { it.chapterId in chapterIds })
        chapterIds.forEach { chapterId ->
            assertTrue(root.questions.any { it.chapterId == chapterId && it.type == "assessment" })
            assertTrue(root.questions.any { it.chapterId == chapterId && it.type == "case" })
        }
    }

    private companion object {
        const val EXPECTED_SHA256 =
            "d285391e4ec2debe39a1ca6be969cf265c53d41c0cdf70003fb3631fa35cfb75"
    }
}
