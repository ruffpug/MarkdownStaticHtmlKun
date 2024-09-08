package net.ruffpug.markdownstatichtmlkun

import java.io.IOException

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
    sealed interface Failure : DocsConversionResult {

        /**
         * 無効な対象ディレクトリパスが指定された場合
         *
         * * 指定パスがディレクトリではない場合
         * * 指定パスが存在しない場合
         */
        data object InvalidTargetDirectoryPathSpecified : Failure

        /**
         * CSSファイルが見つからない場合
         * (本アプリの事前準備が正しく行えていない場合が該当する。)
         */
        data object CssFileNotFound : Failure

        /**
         * 無効な出力先ディレクトリパスが指定された場合
         *
         * * 指定パスがディレクトリではない場合
         * * 指定パスが存在しない場合
         */
        data object InvalidOutputDirectoryPathSpecified : Failure

        /**
         * HTMLファイルの作成に失敗した場合
         *
         * * 既に同名の .html ファイルが存在する場合
         */
        data class FailedToCreateHtmlFile(val fileName: String) : Failure

        /**
         * IOExceptionが発生した場合
         */
        data class IOExceptionOccurred(val exception: IOException) : Failure

        /**
         * SecurityExceptionが発生した場合
         */
        data class SecurityExceptionOccurred(val exception: SecurityException) : Failure
    }
}
