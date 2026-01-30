package io.github.wifi_password_manager.utils

import android.content.Context
import android.net.wifi.WifiManager
import io.github.wifi_password_manager.domain.repository.FileRepository
import io.github.wifi_password_manager.domain.repository.WifiRepository

sealed interface ExportNetworksResult {
    data object Success : ExportNetworksResult

    data object NoNetworks : ExportNetworksResult
}

sealed interface ExportCurrentNetworkResult {
    data object Success : ExportCurrentNetworkResult

    data object NoCurrentNetwork : ExportCurrentNetworkResult
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

suspend fun exportCurrentNetwork(
    context: Context,
    wifiRepository: WifiRepository,
    fileRepository: FileRepository,
    writeJson: suspend (String) -> Unit,
): ExportCurrentNetworkResult {
    val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return ExportCurrentNetworkResult.NoCurrentNetwork
    val wifiInfo = wifiManager.connectionInfo ?: return ExportCurrentNetworkResult.NoCurrentNetwork
    if (wifiInfo.networkId == -1 || wifiInfo.ssid == WifiManager.UNKNOWN_SSID) {
        return ExportCurrentNetworkResult.NoCurrentNetwork
    }

    val normalizedSsid = wifiInfo.ssid.removePrefix("\"").removeSuffix("\"")
    val networks = wifiRepository.getAllNetworksList()
    val currentNetwork =
        networks.firstOrNull { it.networkId == wifiInfo.networkId }
            ?: networks.firstOrNull { it.ssid == normalizedSsid }
            ?: return ExportCurrentNetworkResult.NoCurrentNetwork
    val json = fileRepository.networksToJson(listOf(currentNetwork))
    writeJson(json)
    return ExportCurrentNetworkResult.Success
}
