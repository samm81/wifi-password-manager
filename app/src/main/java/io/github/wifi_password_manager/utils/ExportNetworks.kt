package io.github.wifi_password_manager.utils

import io.github.wifi_password_manager.domain.repository.FileRepository
import io.github.wifi_password_manager.domain.repository.WifiRepository

sealed interface ExportNetworksResult {
    data object Success : ExportNetworksResult

    data object NoNetworks : ExportNetworksResult
}

suspend fun exportNetworks(
    wifiRepository: WifiRepository,
    fileRepository: FileRepository,
    writeJson: suspend (String) -> Unit,
): ExportNetworksResult {
    val count = wifiRepository.getNetworkCount()
    if (count == 0) {
        return ExportNetworksResult.NoNetworks
    }

    val networks = wifiRepository.getAllNetworksList()
    val json = fileRepository.networksToJson(networks)
    writeJson(json)
    return ExportNetworksResult.Success
}
