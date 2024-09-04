package net.ruffpug.markdownstatichtmlkun

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "MarkdownStaticHtmlKun",
    ) {
        App()
    }
}