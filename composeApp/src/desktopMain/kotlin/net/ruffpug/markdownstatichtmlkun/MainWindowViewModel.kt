package net.ruffpug.markdownstatichtmlkun

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import io.github.vinceglb.filekit.core.PlatformDirectory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * メイン画面のViewModel
 */
internal class MainWindowViewModel(private val docsConverter: DocsConverter) : ViewModel() {
    companion object {
        private const val LOG_TAG: String = "MainWindowViewModel"
    }

    //  処理中かどうか
    private val _isProcessing = MutableStateFlow(value = false)

    //  変換対象のディレクトリパス
    private val _targetDirectoryPath = MutableStateFlow(value = "")

    //  変換結果メッセージの表示要求を通知するイベント
    private val _displayingConversionResultMessageRequested =
        Channel<DocsConversionResult>(capacity = Channel.UNLIMITED)

    /**
     * 処理中かどうか
     */
    val isProcessing: StateFlow<Boolean> = this._isProcessing.asStateFlow()

    /**
     * 変換ボタンが有効かどうか
     */
    val isConvertButtonEnabled: StateFlow<Boolean> = combine(
        this._isProcessing,
        this._targetDirectoryPath,
        this::toIsConvertButtonEnabled,
    ).stateIn(this.viewModelScope, SharingStarted.Eagerly, this.toIsConvertButtonEnabled())

    /**
     * 変換対象のディレクトリパス
     */
    val targetDirectoryPath: StateFlow<String> = this._targetDirectoryPath.asStateFlow()

    /**
     * 変換結果メッセージの表示要求を通知するイベント
     */
    val displayingConversionResultMessageRequested: Flow<DocsConversionResult> =
        this._displayingConversionResultMessageRequested.receiveAsFlow()

    init {
        Logger.d(LOG_TAG) { "init" }
    }

    override fun onCleared() {
        Logger.d(LOG_TAG) { "onCleared()" }
        this._displayingConversionResultMessageRequested.close()
    }

    /**
     * 変換対象のディレクトリパスの入力が変化したとき。
     */
    fun onTargetDirectoryPathInputChanged(value: String) {
        Logger.d(LOG_TAG) { "ディレクトリパス入力変化: $value" }
        this._targetDirectoryPath.value = value
    }

    /**
     * ディレクトリが選択されたとき。
     */
    fun onDirectorySelected(directory: PlatformDirectory?) {
        Logger.i(LOG_TAG) { "ディレクトリ選択: $directory" }
        if (directory == null) return

        //  選択結果のパスを設定する。
        this._targetDirectoryPath.value = directory.file.absolutePath
    }

    /**
     * 変換ボタンがクリックされたとき。
     */
    fun onConvertButtonClicked() {
        Logger.i(LOG_TAG) { "変換ボタン押下" }

        //  変換処理を開始する。
        this.viewModelScope.launch { this@MainWindowViewModel.convert() }
    }

    //  変換処理を実行する。
    private suspend fun convert() {
        Logger.i(LOG_TAG) { "変換処理 開始" }

        //  処理中状態を設定する。
        this._isProcessing.value = true

        //  変換処理を実行する。
        val directoryPath: String = this._targetDirectoryPath.value
        val result: DocsConversionResult = this.docsConverter.convert(directoryPath = directoryPath)
        currentCoroutineContext().ensureActive()

        //  変換結果メッセージを表示させる。
        this._displayingConversionResultMessageRequested.trySend(result)

        //  処理中状態を解除する。
        this._isProcessing.value = false

        Logger.i(LOG_TAG) { "変換処理 終了" }
    }

    //  変換ボタンが有効かどうかに変換する。
    private fun toIsConvertButtonEnabled(
        isProcessing: Boolean = this._isProcessing.value,
        targetDirectoryPath: String = this._targetDirectoryPath.value,
    ): Boolean {
        return !isProcessing && targetDirectoryPath.isNotEmpty()
    }
}
