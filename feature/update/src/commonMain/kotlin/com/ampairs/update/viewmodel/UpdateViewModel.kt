package com.ampairs.update.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.AppScope
import com.ampairs.update.domain.UpdateDownloadState
import com.ampairs.update.domain.UpdateInfo
import com.ampairs.update.domain.UpdateInstallState
import com.ampairs.update.service.UpdateDownloader
import com.ampairs.update.service.UpdateInstaller
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UpdateDialogUiState(
    val updateInfo: UpdateInfo,
    val downloadState: UpdateDownloadState = UpdateDownloadState.Idle,
    val installState: UpdateInstallState = UpdateInstallState.Idle,
    val downloadedFilePath: String? = null,
)

@AssistedInject
class UpdateViewModel(
    @Assisted val updateInfo: UpdateInfo,
    private val downloader: UpdateDownloader,
    private val installer: UpdateInstaller,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(updateInfo: UpdateInfo): UpdateViewModel
    }

    private val _state = MutableStateFlow(UpdateDialogUiState(updateInfo))
    val state: StateFlow<UpdateDialogUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            downloader.downloadState.collect { ds ->
                val path = if (ds is UpdateDownloadState.Downloaded) ds.filePath else _state.value.downloadedFilePath
                _state.update { it.copy(downloadState = ds, downloadedFilePath = path) }
            }
        }
        viewModelScope.launch {
            installer.installState.collect { installState ->
                _state.update { it.copy(installState = installState) }
            }
        }
    }

    fun downloadUpdate() {
        viewModelScope.launch { downloader.downloadUpdate(updateInfo) }
    }

    fun cancelDownload() {
        downloader.cancelDownload()
    }

    fun installUpdate() {
        val path = _state.value.downloadedFilePath ?: return
        viewModelScope.launch { installer.installUpdate(path, updateInfo) }
    }

    fun reset() {
        downloader.resetState()
        installer.resetState()
        _state.value = UpdateDialogUiState(updateInfo)
    }
}
