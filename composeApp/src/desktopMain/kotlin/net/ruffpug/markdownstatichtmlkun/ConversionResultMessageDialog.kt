package net.ruffpug.markdownstatichtmlkun

import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties
import markdownstatichtmlkun.composeapp.generated.resources.Res
import markdownstatichtmlkun.composeapp.generated.resources.conversion_result_message_dialog_button_failure
import markdownstatichtmlkun.composeapp.generated.resources.conversion_result_message_dialog_button_success
import markdownstatichtmlkun.composeapp.generated.resources.conversion_result_message_dialog_message_failure_failed_to_create_html_file
import markdownstatichtmlkun.composeapp.generated.resources.conversion_result_message_dialog_message_failure_invalid_directory_path_specified
import markdownstatichtmlkun.composeapp.generated.resources.conversion_result_message_dialog_message_failure_io_exception_occurred
import markdownstatichtmlkun.composeapp.generated.resources.conversion_result_message_dialog_message_failure_security_exception_occurred
import markdownstatichtmlkun.composeapp.generated.resources.conversion_result_message_dialog_message_success
import markdownstatichtmlkun.composeapp.generated.resources.conversion_result_message_dialog_title_failure
import markdownstatichtmlkun.composeapp.generated.resources.conversion_result_message_dialog_title_success
import org.jetbrains.compose.resources.stringResource

/**
 * 変換結果メッセージダイアログ
 */
@Composable
internal fun ConversionResultMessageDialog(
    result: DocsConversionResult,
    onDismissRequest: () -> Unit,
    onConfirmButtonClicked: () -> Unit,
) {
    val title: String = when (result) {
        is DocsConversionResult.Success -> stringResource(Res.string.conversion_result_message_dialog_title_success)
        is DocsConversionResult.Failure -> stringResource(Res.string.conversion_result_message_dialog_title_failure)
    }
    val message: String = when (result) {
        is DocsConversionResult.Success -> stringResource(
            Res.string.conversion_result_message_dialog_message_success,
            result.outputDirectoryPath,
        )

        is DocsConversionResult.Failure.InvalidDirectoryPathSpecified ->
            stringResource(Res.string.conversion_result_message_dialog_message_failure_invalid_directory_path_specified)

        is DocsConversionResult.Failure.FailedToCreateHtmlFile -> stringResource(
            Res.string.conversion_result_message_dialog_message_failure_failed_to_create_html_file,
            result.fileName,
        )

        is DocsConversionResult.Failure.IOExceptionOccurred ->
            stringResource(Res.string.conversion_result_message_dialog_message_failure_io_exception_occurred)

        is DocsConversionResult.Failure.SecurityExceptionOccurred ->
            stringResource(Res.string.conversion_result_message_dialog_message_failure_security_exception_occurred)
    }
    val button: String = when (result) {
        is DocsConversionResult.Success -> stringResource(Res.string.conversion_result_message_dialog_button_success)
        is DocsConversionResult.Failure -> stringResource(Res.string.conversion_result_message_dialog_button_failure)
    }

    AlertDialog(
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirmButtonClicked) {
                Text(button)
            }
        },
    )
}

/**
 * 変換結果メッセージダイアログの表示状態
 */
internal sealed interface ConversionResultMessageDialogState {

    /**
     * 表示状態
     */
    data class Shown(

        /**
         * 変換結果
         */
        val result: DocsConversionResult,
    ) : ConversionResultMessageDialogState

    /**
     * 非表示状態
     */
    data object NotShown : ConversionResultMessageDialogState
}
