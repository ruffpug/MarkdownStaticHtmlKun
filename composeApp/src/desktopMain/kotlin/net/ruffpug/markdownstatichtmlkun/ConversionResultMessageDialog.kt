package net.ruffpug.markdownstatichtmlkun

import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties
import markdownstatichtmlkun.composeapp.generated.resources.Res
import markdownstatichtmlkun.composeapp.generated.resources.conversion_result_message_dialog_button_failure
import markdownstatichtmlkun.composeapp.generated.resources.conversion_result_message_dialog_button_success
import markdownstatichtmlkun.composeapp.generated.resources.conversion_result_message_dialog_message_failure
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
        is DocsConversionResult.Success -> stringResource(Res.string.conversion_result_message_dialog_message_success)
        is DocsConversionResult.Failure -> stringResource(Res.string.conversion_result_message_dialog_message_failure)
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
