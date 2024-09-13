package net.ruffpug.markdownstatichtmlkun

import co.touchlab.kermit.Logger
import java.io.File
import java.io.IOException
import java.net.URLDecoder
import java.nio.file.Files
import java.nio.file.Path
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

        //  Viewport設定を行うmetaタグのHTML文字列
        private const val VIEWPORT_META_TAG: String =
            """<meta name="viewport" content="width=device-width, initial-scale=1">"""

        //  bodyタグに付与するクラス名
        private const val BODY_CLASS_NAME: String = "markdown-body"
    }

    override suspend fun convert(
        targetDirPath: String,
        cssFilePath: String,
        outputDirPath: String,
    ): DocsConversionResult {
        Logger.i(LOG_TAG) { "変換処理 開始: $targetDirPath, $cssFilePath, $outputDirPath" }

        //  ファイル操作を行うため、UIをフリーズさせないように作業用スレッドで処理を実行する。
        val result: DocsConversionResult = withContext(this.workingThreadDispatcher) {
            this@DocsConverterImpl.convert(
                targetDir = File(targetDirPath),
                cssFile = File(cssFilePath),
                outputDir = File(outputDirPath),
            )
        }

        Logger.i(LOG_TAG) { "変換処理 終了: $targetDirPath, $cssFilePath, $outputDirPath, $result" }
        return result
    }

    //  変換処理を実行する。
    private fun convert(targetDir: File, cssFile: File, outputDir: File): DocsConversionResult {
        try {
            //  組み込むCSSファイルを読み込む。
            Logger.d(LOG_TAG) { "CSSファイル読み込み 開始: $cssFile" }

            //  CSSファイルが存在しない場合
            if (!cssFile.exists()) {
                Logger.d(LOG_TAG) { "CSSファイル読み込み ファイル不明: $cssFile" }
                return DocsConversionResult.Failure.CssFileNotFound
            }

            //  CSSファイルが存在する場合、その値を読み込む。
            val cssValue: String = cssFile.readText(charset = Charsets.UTF_8)
            Logger.d(LOG_TAG) { "CSSファイル読み込み 終了: $cssFile" }

            //  指定された対象ディレクトリが有効 (ディレクトリであり、かつ、存在している) かどうかを判定する。
            Logger.d(LOG_TAG) { "対象ディレクトリ有効判定: $targetDir" }
            val isValidTargetDirSpecified: Boolean = targetDir.isDirectory && targetDir.exists()
            Logger.d(LOG_TAG) { "対象ディレクトリ有効判定 結果: $isValidTargetDirSpecified" }

            //  指定された対象ディレクトリが無効である場合
            if (!isValidTargetDirSpecified) {
                Logger.d(LOG_TAG) { "対象ディレクトリ有効判定 無効" }
                return DocsConversionResult.Failure.InvalidTargetDirectoryPathSpecified
            }

            //  指定された出力先ディレクトリが有効 (ディレクトリであり、かつ、存在している) かどうかを判定する。
            Logger.d(LOG_TAG) { "出力先ディレクトリ有効判定: $outputDir" }
            val isOutputDirCreated: Boolean = outputDir.mkdirs()
            val isValidOutputDirSpecified: Boolean = outputDir.isDirectory && outputDir.exists()
            Logger.d(LOG_TAG) { "出力先ディレクトリ有効判定: $outputDir, $isOutputDirCreated, $isValidOutputDirSpecified" }

            //  指定された出力先ディレクトリが無効である場合
            if (!isValidOutputDirSpecified) {
                Logger.d(LOG_TAG) { "出力先ディレクトリ有効判定 無効" }
                return DocsConversionResult.Failure.InvalidOutputDirectoryPathSpecified
            }

            //  出力先ディレクトリ内に一時フォルダを作成する。
            Logger.d(LOG_TAG) { "一時フォルダ作成" }
            val tempDir: File = Files.createTempDirectory(outputDir.toPath(), "docs_").toFile()
            Logger.d(LOG_TAG) { "一時フォルダ作成 作成: $tempDir" }

            //  指定されたディレクトリの中身を一時フォルダに再帰的にコピーする。
            Logger.d(LOG_TAG) { "フォルダコピー 開始" }
            val isSucceeded: Boolean = targetDir.copyRecursively(target = tempDir)
            Logger.d(LOG_TAG) { "フォルダコピー 結果: $isSucceeded" }

            //  変換対象のMarkdownファイル一覧を洗い出す。
            Logger.d(LOG_TAG) { "変換対象Markdown洗い出し 開始" }
            val targetMarkdownFiles = LinkedList<File>()
            Files.walk(tempDir.toPath()).use { stream: Stream<Path> ->
                for (path: Path in stream) {
                    val file: File = path.toFile().canonicalFile
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

                    //  href属性が含まれている場合、そのリンクの相対リンクを解決する。
                    //  (URLエンコードされている可能性も考慮して、URLデコードを掛けた場合のものも求めておく。)
                    val hrefAttr: Attribute = aTag.attribute("href")
                    val link: String = hrefAttr.value
                    val decodedLink: String = URLDecoder.decode(link, Charsets.UTF_8)
                    val resolved1: File = markdownFile.parentFile.resolve(link).canonicalFile
                    val resolved2: File = markdownFile.parentFile.resolve(decodedLink).canonicalFile
                    Logger.d(LOG_TAG) { "Markdown変換 aタグ補正 リンク解決結果: $markdownFile, $link, $resolved1, $resolved2" }

                    //  解決されたリンクに該当するMarkdownファイルを探索する。
                    //  NOTE: そのままのリンク・URLデコードされたリンクのどちらか片方にでもヒットした場合、そのリンクはそのファイルを指しているとみなす。
                    val linkedMarkdownFile: File? =
                        targetMarkdownFiles.firstOrNull { file: File -> file == resolved1 || file == resolved2 }
                    Logger.d(LOG_TAG) { "Markdown変換 aタグ補正 探索結果: $markdownFile, $linkedMarkdownFile" }

                    //  リンクされているMarkdownファイルが見つからなかった場合、手を加えない。
                    if (linkedMarkdownFile == null) {
                        Logger.d(LOG_TAG) { "Markdown変換 aタグ補正 終了 (探索ヒットなし): $markdownFile, $link" }
                        continue
                    }

                    //  リンクされているMarkdownファイルが見つかった場合、そのリンクの拡張子 .md を .html に置換してhref属性を更新する。
                    val correctedLink: String = link.dropLast(n = ".md".length) + ".html"
                    hrefAttr.setValue(correctedLink)
                    Logger.d(LOG_TAG) { "Markdown変換 aタグ補正 終了 (探索ヒットあり): $markdownFile, $link, $correctedLink" }
                }

                //  bodyタグにクラス名を付与する。
                document.body().addClass(BODY_CLASS_NAME)

                //  Viewportの設定を追加する。
                document.head().append(VIEWPORT_META_TAG)

                //  スタイルシートを設定する。
                document.head().appendElement("style").text(cssValue)

                //  補正後のHTML文字列をHTMLファイルとして保存する。
                val markdownHtmlFile = File(markdownFile.parentFile, "${markdownFile.nameWithoutExtension}.html")
                Logger.d(LOG_TAG) { "Markdown変換 補正後HTML作成 開始: $markdownFile, $markdownHtmlFile" }
                val isNewlyCreated: Boolean = markdownHtmlFile.createNewFile()
                Logger.d(LOG_TAG) { "Markdown変換 補正後HTML作成 終了: $markdownFile, $isNewlyCreated" }

                //  HTMLファイルの新規作成に失敗した場合
                if (!isNewlyCreated) {
                    Logger.d(LOG_TAG) { "Markdown変換 補正後HTML作成 新規作成失敗: $markdownFile" }
                    return DocsConversionResult.Failure.FailedToCreateHtmlFile(
                        fileName = markdownFile.toRelativeString(tempDir.canonicalFile),
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

            //  レポートファイルを一時ファイルと同一階層に作成する。
            Logger.d(LOG_TAG) { "レポートファイル作成 開始" }
            val reportFile = File(outputDir, "${tempDir.name}.txt")
            val isReportFileCreated: Boolean = reportFile.createNewFile()
            Logger.d(LOG_TAG) { "レポートファイル作成 終了: $reportFile, $isReportFileCreated" }

            //  レポートファイルに変換対象を出力していく。
            for (markdownFile: File in targetMarkdownFiles) {
                Logger.d(LOG_TAG) { "レポートファイル出力 開始: $markdownFile" }

                //  変換前のMarkdownファイルの相対パスと変換後のHTMLファイルの相対パスを出力する。
                val mdRelativePath: String = markdownFile.toRelativeString(tempDir)
                val htmlFile = File(markdownFile.parentFile, "${markdownFile.nameWithoutExtension}.html")
                val htmlRelativePath: String = htmlFile.toRelativeString(tempDir)
                val line = "$mdRelativePath → $htmlRelativePath${System.lineSeparator()}"
                reportFile.appendText(text = line, charset = Charsets.UTF_8)

                Logger.d(LOG_TAG) { "レポートファイル出力 出力: $markdownFile" }
            }

            //  成功結果を返す。
            return DocsConversionResult.Success(outputDirectoryPath = tempDir.absolutePath)
        }

        //  IOExceptionが発生した場合
        catch (e: IOException) {
            Logger.d(LOG_TAG, e) { "IOException発生: $targetDir" }
            return DocsConversionResult.Failure.IOExceptionOccurred(exception = e)
        }

        //  SecurityExceptionが発生した場合
        catch (e: SecurityException) {
            Logger.d(LOG_TAG, e) { "SecurityException発生: $targetDir" }
            return DocsConversionResult.Failure.SecurityExceptionOccurred(exception = e)
        }
    }
}
