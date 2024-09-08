package net.ruffpug.markdownstatichtmlkun

import co.touchlab.kermit.Logger
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.stream.Stream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import org.jsoup.Jsoup
import org.jsoup.nodes.Attribute
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

/**
 * ドキュメントの変換ロジックの具象実装
 */
internal class DocsConverterImpl(
    private val workingThreadDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DocsConverter {
    companion object {
        private const val LOG_TAG: String = "DocsConverterImpl"
    }

    override suspend fun convert(directoryPath: String): DocsConversionResult {
        Logger.i(LOG_TAG) { "変換処理 開始: $directoryPath" }

        //  ファイル操作を行うため、UIをフリーズさせないように作業用スレッドで処理を実行する。
        val result: DocsConversionResult = withContext(this.workingThreadDispatcher) {
            this@DocsConverterImpl.convert(directory = File(directoryPath))
        }

        Logger.i(LOG_TAG) { "変換処理 終了: $directoryPath, $result" }
        return result
    }

    //  変換処理を実行する。
    private fun convert(directory: File): DocsConversionResult {
        try {
            //  指定されたディレクトリが有効 (ディレクトリであり、かつ、存在している) かどうかを判定する。
            Logger.d(LOG_TAG) { "指定ディレクトリ有効判定: $directory" }
            val isValidDirectorySpecified: Boolean = directory.isDirectory && directory.exists()
            Logger.d(LOG_TAG) { "指定ディレクトリ有効判定 結果: $isValidDirectorySpecified" }

            //  指定されたディレクトリが無効である場合
            if (!isValidDirectorySpecified) {
                Logger.d(LOG_TAG) { "指定ディレクトリ有効判定 無効" }
                return DocsConversionResult.Failure.InvalidDirectoryPathSpecified
            }

            //  一時フォルダを作成する。
            Logger.d(LOG_TAG) { "一時フォルダ作成" }
            val currentDirectoryPath: Path = Paths.get("").toAbsolutePath()
            Logger.d(LOG_TAG) { "一時フォルダ作成 現在ディレクトリ: $currentDirectoryPath" }
            val tempDir: File = Files.createTempDirectory(currentDirectoryPath, "docs_").toFile()
            Logger.d(LOG_TAG) { "一時フォルダ作成 作成: $tempDir" }

            //  指定されたディレクトリの中身を一時フォルダに再帰的にコピーする。
            Logger.d(LOG_TAG) { "フォルダコピー 開始" }
            val isSucceeded: Boolean = directory.copyRecursively(target = tempDir)
            Logger.d(LOG_TAG) { "フォルダコピー 結果: $isSucceeded" }

            //  変換対象のMarkdownファイル一覧を洗い出す。
            Logger.d(LOG_TAG) { "変換対象Markdown洗い出し 開始" }
            val targetMarkdownFiles = LinkedList<File>()
            Files.walk(tempDir.toPath()).use { stream: Stream<Path> ->
                for (path: Path in stream) {
                    val file: File = path.toFile()
                    val isMarkdownFile: Boolean = file.isFile && file.extension.equals("md", ignoreCase = true)

                    //  Markdownファイルである場合
                    if (isMarkdownFile) {
                        Logger.d(LOG_TAG) { "変換対象Markdown洗い出し 対象: $file" }
                        targetMarkdownFiles.add(file)
                    }

                    //  Markdownファイルではない場合
                    else {
                        Logger.d(LOG_TAG) { "変換対象Markdown洗い出し 対象外: $file" }
                    }
                }
            }
            Logger.d(LOG_TAG) { "変換対象Markdown洗い出し 終了: ${targetMarkdownFiles.size}" }

            //  変換対象の各MarkdownファイルをHTMLに変換する。
            val flavour = GFMFlavourDescriptor()
            val parser = MarkdownParser(flavour)
            for (markdownFile: File in targetMarkdownFiles) {
                Logger.d(LOG_TAG) { "Markdown変換 開始: $markdownFile" }

                //  Markdownファイルのテキストを読み出す。
                Logger.d(LOG_TAG) { "Markdown変換 テキスト読み出し 開始: $markdownFile" }
                val markdownStr: String = markdownFile.readText(charset = Charsets.UTF_8)
                Logger.d(LOG_TAG) { "Markdown変換 テキスト読み出し 終了: $markdownFile" }

                //  Markdown文字列をHTML文字列に変換する。
                Logger.d(LOG_TAG) { "Markdown変換 HTML変換 開始: $markdownFile" }
                val parsedTree: ASTNode = parser.buildMarkdownTreeFromString(markdownStr)
                val generator = HtmlGenerator(markdownStr, parsedTree, flavour)
                val html: String = generator.generateHtml()
                Logger.d(LOG_TAG) { "Markdown変換 HTML変換 終了: $markdownFile" }

                //  HTML文字列を再びパースし、aタグを洗い出す。
                Logger.d(LOG_TAG) { "Markdown変換 HTML再パース 開始: $markdownFile" }
                val document: Document = Jsoup.parse(html)
                val aTags: Elements = document.getElementsByTag("a")
                Logger.d(LOG_TAG) { "Markdown変換 HTML再パース 終了: $markdownFile, ${aTags.size}" }

                //  各aタグに対してMarkdownファイルへのリンクを補正する。
                for (aTag: Element in aTags) {
                    Logger.d(LOG_TAG) { "Markdown変換 aタグ補正 開始: $markdownFile, $aTag" }

                    //  href属性が含まれない場合
                    if (!aTag.hasAttr("href")) {
                        Logger.d(LOG_TAG) { "Markdown変換 aタグ補正 終了 (hrefなし): $markdownFile, $aTag" }
                        continue
                    }

                    //  href属性が含まれている場合、そのリンク (相対位置) を絶対パスに変換する。
                    val hrefAttr: Attribute = aTag.attribute("href")
                    val link: String = hrefAttr.value
                    val resolvedLink: File = markdownFile.parentFile.resolve(link).canonicalFile
                    Logger.d(LOG_TAG) { "Markdown変換 aタグ補正 リンク解決結果: $markdownFile, $link, $resolvedLink" }

                    //  変換されたリンクに該当するMarkdownファイルを探索する。
                    val linkedMarkdownFile: File? =
                        targetMarkdownFiles.firstOrNull { f -> f.absolutePath == resolvedLink.absolutePath }
                    Logger.d(LOG_TAG) { "Markdown変換 aタグ補正 探索結果: $markdownFile, $linkedMarkdownFile" }

                    //  リンクされているMarkdownファイルが見つからなかった場合
                    if (linkedMarkdownFile == null) {
                        Logger.d(LOG_TAG) { "Markdown変換 aタグ補正 終了 (探索ヒットなし): $markdownFile, $link" }
                        continue
                    }

                    //  リンクされているMarkdownファイルが見つかった場合、拡張子をhtmlに更新した相対パスをhref属性に設定する。
                    val linkedMarkdownHtmlFile =
                        File(linkedMarkdownFile.parentFile, "${linkedMarkdownFile.nameWithoutExtension}.html")
                    val correctedRelativePath: String = linkedMarkdownHtmlFile.toRelativeString(markdownFile.parentFile)
                    hrefAttr.setValue(correctedRelativePath)
                    Logger.d(LOG_TAG) { "Markdown変換 aタグ補正 終了 (探索ヒットあり): $markdownFile, $correctedRelativePath" }
                }

                //  補正後のHTML文字列をHTMLファイルとして保存する。
                val markdownHtmlFile = File(markdownFile.parentFile, "${markdownFile.nameWithoutExtension}.html")
                Logger.d(LOG_TAG) { "Markdown変換 補正後HTML作成 開始: $markdownFile, $markdownHtmlFile" }
                val isNewlyCreated: Boolean = markdownHtmlFile.createNewFile()
                Logger.d(LOG_TAG) { "Markdown変換 補正後HTML作成 終了: $markdownFile, $isNewlyCreated" }

                //  HTMLファイルの新規作成に失敗した場合
                if (!isNewlyCreated) {
                    Logger.d(LOG_TAG) { "Markdown変換 補正後HTML作成 新規作成失敗: $markdownFile" }
                    return DocsConversionResult.Failure.FailedToCreateHtmlFile(
                        fileName = markdownFile.toRelativeString(tempDir),
                    )
                }

                //  HTMLファイルの新規作成に成功した場合、そのファイルにHTML文字列を書き込む。
                Logger.d(LOG_TAG) { "Markdown変換 補正後HTML保存 開始: $markdownFile" }
                markdownHtmlFile.writeText(text = document.html(), charset = Charsets.UTF_8)
                Logger.d(LOG_TAG) { "Markdown変換 補正後HTML保存 終了: $markdownFile" }

                //  不要となったMarkdownファイルは削除する。
                Logger.d(LOG_TAG) { "Markdown変換 Markdownファイル削除 開始: $markdownFile" }
                val isSuccessfullyDeleted: Boolean = markdownFile.delete()
                Logger.d(LOG_TAG) { "Markdown変換 Markdownファイル削除 終了: $markdownFile, $isSuccessfullyDeleted" }

                Logger.d(LOG_TAG) { "Markdown変換 終了: $markdownFile" }
            }

            //  成功結果を返す。
            return DocsConversionResult.Success(outputDirectoryPath = tempDir.absolutePath)
        }

        //  IOExceptionが発生した場合
        catch (e: IOException) {
            Logger.d(LOG_TAG, e) { "IOException発生: $directory" }
            return DocsConversionResult.Failure.IOExceptionOccurred(exception = e)
        }

        //  SecurityExceptionが発生した場合
        catch (e: SecurityException) {
            Logger.d(LOG_TAG, e) { "SecurityException発生: $directory" }
            return DocsConversionResult.Failure.SecurityExceptionOccurred(exception = e)
        }
    }
}
