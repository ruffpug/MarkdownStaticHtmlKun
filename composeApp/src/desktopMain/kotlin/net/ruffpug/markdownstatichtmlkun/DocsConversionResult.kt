package net.ruffpug.markdownstatichtmlkun

/**
 * ドキュメントの変換結果
 */
internal sealed interface DocsConversionResult {

    /**
     * 変換成功
     */
    data class Success(

        /**
         * 出力先のディレクトリのパス
         */
        val outputDirectoryPath: String,
    ) : DocsConversionResult

    /**
     * 変換失敗
     */
    data class Failure(

        /**
         * TODO: エラー原因の定義
         */
        val todoCause: Any,
    ) : DocsConversionResult
}
