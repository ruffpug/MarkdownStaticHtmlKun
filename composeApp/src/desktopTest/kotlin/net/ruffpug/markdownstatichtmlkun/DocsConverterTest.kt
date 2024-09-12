package net.ruffpug.markdownstatichtmlkun

import java.io.File
import java.net.URLDecoder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

internal class DocsConverterTest {

    @Rule
    @JvmField
    val tempFolder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `変換に成功するケースのテスト`() = runBlocking {
        //  変換前ドキュメントAを利用する。
        val sourceDocsA = File(javaClass.classLoader.getResource("source_docs_a")!!.file)
        val stylesheetCss = File(javaClass.classLoader.getResource("stylesheet.css")!!.file)
        val destDir: File = tempFolder.newFolder("dest_parent")

        //  変換を実行する。
        val converter = DocsConverterImpl()
        val result: DocsConversionResult = converter.convert(
            targetDirPath = sourceDocsA.absolutePath,
            cssFilePath = stylesheetCss.absolutePath,
            outputDirPath = destDir.absolutePath,
        )

        //  変換に成功しているはず。
        assertIs<DocsConversionResult.Success>(result)

        //  出力先のファイル一覧が以下の内容であるはず。
        //  (順不同で、出力ディレクトリからの相対パスで比較する。)
        val expectedOutputFiles = setOf(
            "README.html", // ← "README.md",
            "document1.html", // ← "document1.md",
            "ドキュメント2.html", // ← "ドキュメント2.md",
            "text1.txt",
            "テキスト2.txt",
            "test.html",
            "sub/document3.html", // ← "sub/document3.md",
            "sub/書類4.html", // ← "sub/書類4.md",
            "sub/text3.txt",
            "sub/平文4.txt",
            "res/example_image.png",
        )
        val outputDir = File(result.outputDirectoryPath)
        val outputDirPath: Path = outputDir.toPath()
        val outputFileList: List<String> =
            Files.walk(outputDirPath)
                .filter { path: Path -> path.isRegularFile() }
                .map { path: Path -> path.toFile().toRelativeString(outputDir) }
                .toList()
        assertContentEquals(
            expected = expectedOutputFiles.sorted(),
            actual = outputFileList.sorted(),
        )

        //  README.mdの中のファイルのリンクが以下のものであるはず。
        val readmeHtml: Document = Jsoup.parse(File(result.outputDirectoryPath, "README.html"))
        assert(
            expectedLinks = listOf(
                "目次" to "README.html", // ← README.md
                "document1" to "document1.html", // ← document1.md
                "ドキュメント2" to "ドキュメント2.html", // ← ドキュメント2.md
                "document3" to "sub/document3.html", // ← sub/document3.md
                "書類4" to "sub/書類4.html", // ← sub/書類4.md
                "text1" to "text1.txt",
                "テキスト2" to "テキスト2.txt",
                "text3" to "sub/text3.txt",
                "平文4" to "sub/平文4.txt",
                "https://example.com" to "https://example.com",
                "https://example.com/test.txt" to "https://example.com/text.txt",
                "example.com/test.txt" to "example.com/text.txt",
                "目次 (カレントディレクトリ指定)" to "./README.html", // ← ./README.md
                "document1 (カレントディレクトリ指定)" to "./document1.html", // ← ./document1.html
                "ドキュメント2 (カレントディレクトリ指定)" to "./ドキュメント2.html", // ← ./ドキュメント2.html
                "document3 (カレントディレクトリ指定)" to "./sub/document3.html", // ← ./sub/document3.md
                "書類4 (カレントディレクトリ指定)" to "./sub/書類4.html", // ← ./sub/書類4.md
                "text1 (カレントディレクトリ指定)" to "./text1.txt",
                "テキスト2 (カレントディレクトリ指定)" to "./テキスト2.txt",
                "text3 (カレントディレクトリ指定)" to "./sub/text3.txt",
                "平文4 (カレントディレクトリ指定)" to "./sub/平文4.txt",
                "存在しないファイル.md" to "存在しないファイル.md",
                "sub/存在しないファイル.md" to "sub/存在しないファイル.md",
            ),
            actualATags = readmeHtml.getElementsByTag("a"),
        )

        //  sub/書類4.mdの中のファイルのリンクが以下のものであるはず。
        val doc4Html: Document = Jsoup.parse(File(result.outputDirectoryPath, "sub/書類4.html"))
        assert(
            expectedLinks = listOf(
                "目次" to "../README.html", // ← ../README.md
                "document1" to "../document1.html", // ← ../document1.md
                "ドキュメント2" to "../ドキュメント2.html", // ← ../ドキュメント2.md
                "document3" to "document3.html", // ← document3.md
                "書類4" to "書類4.html", // ← 書類4.md
                "text1" to "../text1.txt",
                "テキスト2" to "../テキスト2.txt",
                "text3" to "text3.txt",
                "平文4" to "平文4.txt",
                "https://example.com" to "https://example.com",
                "https://example.com/test.txt" to "https://example.com/text.txt",
                "example.com/test.txt" to "example.com/text.txt",
                "目次 (カレントディレクトリ指定)" to "./../README.html", // ← ./../README.md
                "document1 (カレントディレクトリ指定)" to "./../document1.html", // ← ./../document1.html
                "ドキュメント2 (カレントディレクトリ指定)" to "./../ドキュメント2.html", // ← ./../ドキュメント2.html
                "document3 (カレントディレクトリ指定)" to "./document3.html", // ← ./document3.md
                "書類4 (カレントディレクトリ指定)" to "./書類4.html", // ← ./書類4.md
                "text1 (カレントディレクトリ指定)" to "./../text1.txt",
                "テキスト2 (カレントディレクトリ指定)" to "./../テキスト2.txt",
                "text3 (カレントディレクトリ指定)" to "./text3.txt",
                "平文4 (カレントディレクトリ指定)" to "./平文4.txt",
                "存在しないファイル.md" to "../存在しないファイル.md",
                "sub/存在しないファイル.md" to "存在しないファイル.md",
            ),
            actualATags = doc4Html.getElementsByTag("a"),
        )
    }

    @Test
    fun `指定された変換対象ディレクトリが無効であることにより変換に失敗するケースのテスト`() = runBlocking<Unit> {
        val sourceDocsA = File(javaClass.classLoader.getResource("source_docs_a")!!.file)
        val stylesheetCss = File(javaClass.classLoader.getResource("stylesheet.css")!!.file)
        val destDir: File = tempFolder.newFolder("dest_parent")
        val converter = DocsConverterImpl()

        //  存在しない変換対象ディレクトリを設定する。
        val badSourceDir = File(sourceDocsA.parentFile, "bad_source_docs/")
        assertEquals(expected = false, actual = badSourceDir.exists())

        //  ドキュメントAの変換は失敗するはず。
        val resultA1: DocsConversionResult = converter.convert(
            targetDirPath = badSourceDir.absolutePath,
            cssFilePath = stylesheetCss.absolutePath,
            outputDirPath = destDir.absolutePath,
        )
        assertIs<DocsConversionResult.Failure.InvalidTargetDirectoryPathSpecified>(resultA1)

        //  変換対象ディレクトリにテキストファイルを設定する。
        val badSourceDirFile = tempFolder.newFile("bad_source_docs.txt")
        badSourceDirFile.createNewFile()

        //  ドキュメントAの変換は失敗するはず。
        val resultA2: DocsConversionResult = converter.convert(
            targetDirPath = badSourceDir.absolutePath,
            cssFilePath = stylesheetCss.absolutePath,
            outputDirPath = destDir.absolutePath,
        )
        assertIs<DocsConversionResult.Failure.InvalidTargetDirectoryPathSpecified>(resultA2)
    }

    @Test
    fun `指定されたCSSファイルが無効であることにより変換に失敗するケースのテスト`() = runBlocking<Unit> {
        //  変換前ドキュメントAを利用する。
        val sourceDocsA = File(javaClass.classLoader.getResource("source_docs_a")!!.file)
        val stylesheetCss = File(javaClass.classLoader.getResource("stylesheet.css")!!.file)
        val destDir: File = tempFolder.newFolder("dest_parent")
        val converter = DocsConverterImpl()

        //  存在しないCSSファイルを指定する。
        val badStylesheetCss = File(stylesheetCss.parentFile, "bad_stylesheet.css")
        assertEquals(expected = false, actual = badStylesheetCss.exists())

        //  ドキュメントAの変換は失敗するはず。
        val resultA: DocsConversionResult = converter.convert(
            targetDirPath = sourceDocsA.absolutePath,
            cssFilePath = badStylesheetCss.absolutePath,
            outputDirPath = destDir.absolutePath,
        )
        assertIs<DocsConversionResult.Failure.CssFileNotFound>(resultA)
    }

    @Test
    fun `指定された出力先が無効であることにより変換に失敗するケースのテスト`() = runBlocking<Unit> {
        //  変換前ドキュメントAを利用する。
        val sourceDocsA = File(javaClass.classLoader.getResource("source_docs_a")!!.file)
        val stylesheetCss = File(javaClass.classLoader.getResource("stylesheet.css")!!.file)
        val destDir: File = tempFolder.newFolder("dest_parent")
        val converter = DocsConverterImpl()

        //  出力先ディレクトリをテキストファイルとする。
        val invalidDestDir = File(destDir, "bad_dir.txt")
        invalidDestDir.createNewFile()

        //  ドキュメントAの変換は失敗するはず。
        val resultA: DocsConversionResult = converter.convert(
            targetDirPath = sourceDocsA.absolutePath,
            cssFilePath = stylesheetCss.absolutePath,
            outputDirPath = invalidDestDir.absolutePath,
        )
        assertIs<DocsConversionResult.Failure.InvalidOutputDirectoryPathSpecified>(resultA)
    }

    @Test
    fun `同名のHTMLが既に存在することにより変換に失敗するケースのテスト`() = runBlocking {
        //  変換前ドキュメントB・Cを利用する。
        val sourceDocsB = File(javaClass.classLoader.getResource("source_docs_b")!!.file)
        val sourceDocsC = File(javaClass.classLoader.getResource("source_docs_c")!!.file)
        val stylesheetCss = File(javaClass.classLoader.getResource("stylesheet.css")!!.file)
        val destDir: File = tempFolder.newFolder("dest_parent")
        val converter = DocsConverterImpl()

        //  ドキュメントBの変換は失敗するはず。
        val resultB: DocsConversionResult = converter.convert(
            targetDirPath = sourceDocsB.absolutePath,
            cssFilePath = stylesheetCss.absolutePath,
            outputDirPath = destDir.absolutePath,
        )
        assertIs<DocsConversionResult.Failure.FailedToCreateHtmlFile>(resultB)
        assertEquals(expected = "README.md", actual = resultB.fileName)

        //  ドキュメントCの変換は失敗するはず。
        val resultC: DocsConversionResult = converter.convert(
            targetDirPath = sourceDocsC.absolutePath,
            cssFilePath = stylesheetCss.absolutePath,
            outputDirPath = destDir.absolutePath,
        )
        assertIs<DocsConversionResult.Failure.FailedToCreateHtmlFile>(resultC)
        assertEquals(expected = "sub/sub_doc.md", actual = resultC.fileName)
    }

    //  リンク集が指定したaタグ一覧と一致しているかどうかを検証する。
    private fun assert(expectedLinks: List<Pair<String, String>>, actualATags: Elements) {
        //  要素数を検証する。
        assertEquals(expected = expectedLinks.size, actual = actualATags.size)

        for (i: Int in expectedLinks.indices) {
            val (expectedText: String, expectedLink: String) = expectedLinks[i]
            val actualText: String = actualATags[i].text()
            val actualLink: String = actualATags[i].attr("href")

            //  テキスト部分の一致を検証する。
            assertEquals(expected = expectedText, actual = actualText)

            //  リンク部分の一致を検証する。
            assertTrue {
                //  そのままのリンク、もしくは、デコードされたリンクと一致しているかどうかを判定する。
                //  (補正対象のリンクは未エンコードで、補正対象外のリンクはエンコードされた、hrefに設定されるため。)
                val decodedActualLink: String = URLDecoder.decode(actualLink, Charsets.UTF_8)
                expectedLink == actualLink || expectedLink == decodedActualLink
            }
        }
    }
}
