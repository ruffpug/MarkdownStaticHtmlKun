package net.ruffpug.markdownstatichtmlkun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import co.touchlab.kermit.Logger
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerType
import java.awt.Desktop
import java.io.File
import java.net.URI
import markdownstatichtmlkun.composeapp.generated.resources.Res
import markdownstatichtmlkun.composeapp.generated.resources.css_picker_title
import markdownstatichtmlkun.composeapp.generated.resources.main_window_convert_button
import markdownstatichtmlkun.composeapp.generated.resources.main_window_css_file_path_label
import markdownstatichtmlkun.composeapp.generated.resources.main_window_css_file_path_select_button
import markdownstatichtmlkun.composeapp.generated.resources.main_window_github_button
import markdownstatichtmlkun.composeapp.generated.resources.main_window_license_button
import markdownstatichtmlkun.composeapp.generated.resources.main_window_output_directory_path_label
import markdownstatichtmlkun.composeapp.generated.resources.main_window_output_directory_path_select_button
import markdownstatichtmlkun.composeapp.generated.resources.main_window_target_directory_path_label
import markdownstatichtmlkun.composeapp.generated.resources.main_window_target_directory_select_button
import markdownstatichtmlkun.composeapp.generated.resources.output_directory_picker_title
import markdownstatichtmlkun.composeapp.generated.resources.target_directory_picker_title
import markdownstatichtmlkun.composeapp.generated.resources.title
import org.jetbrains.compose.resources.stringResource

private const val LOG_TAG: String = "MainWindow"

/**
 * メイン画面
 */
@Composable
internal fun MainWindow(
    viewModel: MainWindowViewModel,
    onCloseRequest: () -> Unit,
) {
    Window(
        title = stringResource(Res.string.title),
        onCloseRequest = onCloseRequest,
    ) {
        MaterialTheme {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                val isProcessing: Boolean by viewModel.isProcessing.collectAsState()
                val isConvertButtonEnabled: Boolean by viewModel.isConvertButtonEnabled.collectAsState()
                val targetDirectoryPath: String by viewModel.targetDirectoryPath.collectAsState()
                val cssFilePath: String by viewModel.cssFilePath.collectAsState()
                val outputDirectoryPath: String by viewModel.outputDirectoryPath.collectAsState()
                var conversionResultMessageDialogState: ConversionResultMessageDialogState by remember {
                    mutableStateOf(ConversionResultMessageDialogState.NotShown)
                }
                var isLicenseDialogShown: Boolean by remember { mutableStateOf(false) }

                //  対象ディレクトリ選択ダイアログのLauncher
                val targetDirPickerLauncher = rememberDirectoryPickerLauncher(
                    title = stringResource(Res.string.target_directory_picker_title),
                    initialDirectory = null,
                    onResult = viewModel::onTargetDirectorySelected,
                )

                //  CSS選択ダイアログのLauncher
                val cssPickerLauncher = rememberFilePickerLauncher(
                    type = PickerType.File(listOf("css")),
                    title = stringResource(Res.string.css_picker_title),
                    initialDirectory = null,
                    onResult = viewModel::onCssFileSelected,
                )

                //  出力先ディレクトリ選択ダイアログのLauncher
                val outputDirPickerLauncher = rememberDirectoryPickerLauncher(
                    title = stringResource(Res.string.output_directory_picker_title),
                    initialDirectory = null,
                    onResult = viewModel::onOutputDirectorySelected,
                )

                //  変換結果メッセージの表示要求を購読する。
                LaunchedEffect(Unit) {
                    viewModel.displayingConversionResultMessageRequested.collect { result: DocsConversionResult ->
                        //  変換結果メッセージダイアログを表示する。
                        conversionResultMessageDialogState = ConversionResultMessageDialogState.Shown(result = result)
                    }
                }

                //  上部ボタン群
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    //  GitHubボタン
                    TextButton(
                        modifier = Modifier.padding(8.dp, 8.dp, 4.dp, 8.dp),
                        onClick = ::openGitHubPageInBrowser,
                    ) {
                        Text(stringResource(Res.string.main_window_github_button))
                    }

                    //  ライセンスボタン
                    TextButton(
                        modifier = Modifier.padding(4.dp, 8.dp, 8.dp, 8.dp),
                        onClick = { isLicenseDialogShown = true },
                    ) {
                        Text(stringResource(Res.string.main_window_license_button))
                    }
                }

                //  対象ディレクトリパスラベル
                Text(
                    text = stringResource(Res.string.main_window_target_directory_path_label),
                    modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp),
                )

                //  対象ディレクトリパス入力欄
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    //  入力フィールド
                    OutlinedTextField(
                        value = targetDirectoryPath,
                        onValueChange = viewModel::onTargetDirectoryPathInputChanged,
                        enabled = !isProcessing,
                        singleLine = true,
                        modifier = Modifier.padding(16.dp, 8.dp, 8.dp, 16.dp).weight(1f),
                    )

                    //  選択ボタン
                    Button(
                        onClick = { targetDirPickerLauncher.launch() },
                        enabled = !isProcessing,
                        modifier = Modifier.padding(8.dp, 8.dp, 16.dp, 8.dp),
                    ) {
                        Text(text = stringResource(Res.string.main_window_target_directory_select_button))
                    }
                }

                //  CSSファイルパスラベル
                Text(
                    text = stringResource(Res.string.main_window_css_file_path_label),
                    modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp),
                )

                //  CSSファイルパス入力欄
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    //  入力フィールド
                    OutlinedTextField(
                        value = cssFilePath,
                        onValueChange = viewModel::onCssFilePathInputChanged,
                        enabled = !isProcessing,
                        singleLine = true,
                        modifier = Modifier.padding(16.dp, 8.dp, 8.dp, 16.dp).weight(1f),
                    )

                    //  選択ボタン
                    Button(
                        onClick = { cssPickerLauncher.launch() },
                        enabled = !isProcessing,
                        modifier = Modifier.padding(8.dp, 8.dp, 16.dp, 8.dp),
                    ) {
                        Text(text = stringResource(Res.string.main_window_css_file_path_select_button))
                    }
                }

                //  出力先ディレクトリパスラベル
                Text(
                    text = stringResource(Res.string.main_window_output_directory_path_label),
                    modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp),
                )

                //  出力先ディレクトリパス入力欄
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    //  入力フィールド
                    OutlinedTextField(
                        value = outputDirectoryPath,
                        onValueChange = viewModel::onOutputDirectoryPathInputChanged,
                        enabled = !isProcessing,
                        singleLine = true,
                        modifier = Modifier.padding(16.dp, 8.dp, 8.dp, 16.dp).weight(1f),
                    )

                    //  選択ボタン
                    Button(
                        onClick = { outputDirPickerLauncher.launch() },
                        enabled = !isProcessing,
                        modifier = Modifier.padding(8.dp, 8.dp, 16.dp, 8.dp),
                    ) {
                        Text(text = stringResource(Res.string.main_window_output_directory_path_select_button))
                    }
                }

                //  変換ボタン
                Button(
                    onClick = viewModel::onConvertButtonClicked,
                    enabled = isConvertButtonEnabled,
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(stringResource(Res.string.main_window_convert_button))
                }

                //  プログレスインジケータ
                if (isProcessing) CircularProgressIndicator(modifier = Modifier.padding(16.dp))

                //  変換結果メッセージダイアログ
                val dialogState: ConversionResultMessageDialogState = conversionResultMessageDialogState
                if (dialogState is ConversionResultMessageDialogState.Shown) {
                    val result: DocsConversionResult = dialogState.result

                    ConversionResultMessageDialog(
                        result = result,
                        onDismissRequest = {
                            Logger.i(LOG_TAG) { "変換結果メッセージダイアログ終了: $result" }

                            //  ダイアログを非表示状態にする。
                            conversionResultMessageDialogState = ConversionResultMessageDialogState.NotShown
                        },
                        onConfirmButtonClicked = {
                            Logger.i(LOG_TAG) { "変換結果メッセージダイアログ確認ボタン押下: $result" }

                            //  ダイアログを非表示状態にする。
                            conversionResultMessageDialogState = ConversionResultMessageDialogState.NotShown

                            //  変換成功時は出力先をExplorer/Finderで表示する。
                            if (result is DocsConversionResult.Success) {
                                openOutputDirectory(outputDirectoryPath = result.outputDirectoryPath)
                            }
                        },
                    )
                }

                //  ライセンスダイアログ
                if (isLicenseDialogShown) {
                    LicenseDialog(onDismissRequest = { isLicenseDialogShown = false })
                }
            }
        }
    }
}

//  出力先ディレクトリをExplorer/Finderで表示する。
private fun openOutputDirectory(outputDirectoryPath: String) {
    try {
        Logger.d(LOG_TAG) { "出力先表示 開始: $outputDirectoryPath" }
        val file = File(outputDirectoryPath)
        Desktop.getDesktop().open(file)
        Logger.d(LOG_TAG) { "出力先表示 終了: $outputDirectoryPath" }
    } catch (e: Exception) {
        Logger.d(LOG_TAG, e) { "出力先表示 例外: $outputDirectoryPath" }
    }
}

//  GitHubページをブラウザで表示する。
private fun openGitHubPageInBrowser() {
    try {
        Logger.d(LOG_TAG) { "GitHubページ表示 開始" }
        val url = "https://github.com/ruffpug/MarkdownStaticHtmlKun"
        Desktop.getDesktop().browse(URI(url))
        Logger.d(LOG_TAG) { "GitHubページ表示 終了" }
    } catch (e: Exception) {
        Logger.d(LOG_TAG, e) { "GitHubページ表示 例外" }
    }
}
