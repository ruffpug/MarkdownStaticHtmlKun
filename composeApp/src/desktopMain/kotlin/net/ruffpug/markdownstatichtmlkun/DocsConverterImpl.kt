package net.ruffpug.markdownstatichtmlkun

import co.touchlab.kermit.Logger
import java.nio.file.Path
import kotlin.io.path.pathString
import kotlin.random.Random
import kotlinx.coroutines.delay

/**
 * ドキュメントの変換ロジックの具象実装
 */
internal class DocsConverterImpl : DocsConverter {
    companion object {
        private const val LOG_TAG: String = "DocsConverterImpl"
    }

    override suspend fun convert(directoryPath: String): DocsConversionResult {
        Logger.i(LOG_TAG) { "変換処理 開始: $directoryPath" }

        //  TODO: 仮
        delay(5000L)
        val result: DocsConversionResult = when (Random.Default.nextBoolean()) {
            true -> DocsConversionResult.Success(outputDirectoryPath = Path.of("").toAbsolutePath().pathString)
            false -> DocsConversionResult.Failure(todoCause = "TODO: 今後実装予定")
        }

        Logger.i(LOG_TAG) { "変換処理 開始: $directoryPath, $result" }
        return result
    }
}
