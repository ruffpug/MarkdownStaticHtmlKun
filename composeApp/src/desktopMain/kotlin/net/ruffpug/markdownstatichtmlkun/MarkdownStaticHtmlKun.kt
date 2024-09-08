package net.ruffpug.markdownstatichtmlkun

import androidx.compose.ui.window.application
import co.touchlab.kermit.Logger
import co.touchlab.kermit.SystemWriter

private object MarkdownStaticHtmlKun {

    @JvmStatic
    fun main(args: Array<String>) {
        Logger.setLogWriters(SystemWriter())
        Logger.i("アプリ起動")

        val docsConverter = DocsConverterImpl()
        val viewModel = MainWindowViewModel(docsConverter = docsConverter)

        application {
            MainWindow(viewModel = viewModel, onCloseRequest = ::exitApplication)
        }
    }
}
