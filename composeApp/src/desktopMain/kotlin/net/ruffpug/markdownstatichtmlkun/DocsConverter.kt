package net.ruffpug.markdownstatichtmlkun

/**
 * ドキュメントの変換ロジック
 */
internal interface DocsConverter {

    /**
     * Markdownドキュメントを静的HTMLのドキュメントに変換する。
     */
    suspend fun convert(directoryPath: String): DocsConversionResult
}
