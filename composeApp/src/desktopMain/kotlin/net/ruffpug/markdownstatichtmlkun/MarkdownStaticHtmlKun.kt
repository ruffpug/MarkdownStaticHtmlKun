package net.ruffpug.markdownstatichtmlkun

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import markdownstatichtmlkun.composeapp.generated.resources.Res
import markdownstatichtmlkun.composeapp.generated.resources.title
import org.jetbrains.compose.resources.stringResource

private object MarkdownStaticHtmlKun {

    @JvmStatic
    fun main(args: Array<String>) {
        application {
            Window(
                title = stringResource(Res.string.title),
                onCloseRequest = ::exitApplication,
            ) {
                App()
            }
        }
    }
}
