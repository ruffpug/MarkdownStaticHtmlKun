package net.ruffpug.markdownstatichtmlkun

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import co.touchlab.kermit.Logger
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.rememberLibraries
import java.awt.Desktop
import java.net.URI

private const val LOG_TAG: String = "LicenseDialog"

/**
 * ライセンスダイアログ
 */
@Composable
internal fun LicenseDialog(onDismissRequest: () -> Unit, onLibraryClick: (Library) -> Unit = ::handleLibraryClicked) {
    val libraries: Libs? by rememberLibraries {
        useResource("aboutlibraries.json") { res -> res.bufferedReader().readText() }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        LibrariesContainer(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            libraries = libraries,
            onLibraryClick = onLibraryClick,
        )
    }
}

//  ライブラリの押下をハンドリングする。
private fun handleLibraryClicked(library: Library) {
    try {
        Logger.d(LOG_TAG) { "ライブラリブラウザ表示 開始: $library" }
        val url: String? = library.website
        if (url != null) Desktop.getDesktop().browse(URI(url))
        Logger.d(LOG_TAG) { "ライブラリブラウザ表示 終了: $library" }
    } catch (e: Exception) {
        Logger.d(LOG_TAG, e) { "ライブラリブラウザ表示 例外: $library" }
    }
}
