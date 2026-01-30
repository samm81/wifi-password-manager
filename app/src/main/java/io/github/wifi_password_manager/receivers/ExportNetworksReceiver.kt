package io.github.wifi_password_manager.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.wifi_password_manager.domain.repository.FileRepository
import io.github.wifi_password_manager.domain.repository.WifiRepository
import io.github.wifi_password_manager.utils.ExportCurrentNetworkResult
import io.github.wifi_password_manager.utils.ExportNetworksResult
import io.github.wifi_password_manager.utils.exportCurrentNetwork
import io.github.wifi_password_manager.utils.exportNetworks
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ExportNetworksReceiver : BroadcastReceiver(), KoinComponent {
    private val wifiRepository by inject<WifiRepository>()
    private val fileRepository by inject<FileRepository>()

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_EXPORT_NETWORKS -> exportAllNetworks(context, intent)
            ACTION_EXPORT_CURRENT_NETWORK -> exportCurrentNetwork(context, intent)
            else -> return
        }
    }

    private fun exportAllNetworks(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val resultIntent = Intent(ACTION_EXPORT_NETWORKS_RESULT)
            runCatching {
                    val baseDir =
                        context.getExternalFilesDir(null)
                            ?: error("External files directory unavailable")
                    val exportDir = File(baseDir, EXPORT_DIR)
                    if (!exportDir.exists()) {
                        check(exportDir.mkdirs()) { "Unable to create export directory" }
                    }
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
                    val timestamp = LocalDateTime.now(ZoneId.systemDefault()).format(formatter)
                    val exportFile = File(exportDir, "wifi_export_$timestamp.json")
                    val result =
                        exportNetworks(
                            wifiRepository = wifiRepository,
                            fileRepository = fileRepository,
                        ) { json ->
                            exportFile.writeText(json)
                        }
                    when (result) {
                        ExportNetworksResult.Success -> {
                            resultIntent.putExtra(EXTRA_SUCCESS, true)
                            resultIntent.putExtra(EXTRA_PATH, exportFile.absolutePath)
                        }
                        ExportNetworksResult.NoNetworks -> {
                            resultIntent.putExtra(EXTRA_SUCCESS, false)
                            resultIntent.putExtra(EXTRA_ERROR, ERROR_NO_NETWORKS)
                        }
                    }
                }
                .onFailure { throwable ->
                    resultIntent.putExtra(EXTRA_SUCCESS, false)
                    resultIntent.putExtra(EXTRA_ERROR, throwable.message ?: ERROR_UNKNOWN)
                }

            context.sendBroadcast(resultIntent)
            pendingResult.finish()
        }
    }

    private fun exportCurrentNetwork(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val resultIntent = Intent(ACTION_EXPORT_CURRENT_NETWORK_RESULT)
            runCatching {
                    val baseDir =
                        context.getExternalFilesDir(null)
                            ?: error("External files directory unavailable")
                    val exportDir = File(baseDir, EXPORT_DIR)
                    if (!exportDir.exists()) {
                        check(exportDir.mkdirs()) { "Unable to create export directory" }
                    }
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
                    val timestamp = LocalDateTime.now(ZoneId.systemDefault()).format(formatter)
                    val exportFile = File(exportDir, "wifi_current_$timestamp.json")
                    val result =
                        exportCurrentNetwork(
                            context = context,
                            wifiRepository = wifiRepository,
                            fileRepository = fileRepository,
                        ) { json ->
                            exportFile.writeText(json)
                        }
                    when (result) {
                        ExportCurrentNetworkResult.Success -> {
                            resultIntent.putExtra(EXTRA_SUCCESS, true)
                            resultIntent.putExtra(EXTRA_PATH, exportFile.absolutePath)
                        }
                        ExportCurrentNetworkResult.NoCurrentNetwork -> {
                            resultIntent.putExtra(EXTRA_SUCCESS, false)
                            resultIntent.putExtra(EXTRA_ERROR, ERROR_NO_CURRENT_NETWORK)
                        }
                    }
                }
                .onFailure { throwable ->
                    resultIntent.putExtra(EXTRA_SUCCESS, false)
                    resultIntent.putExtra(EXTRA_ERROR, throwable.message ?: ERROR_UNKNOWN)
                }

            context.sendBroadcast(resultIntent)
            pendingResult.finish()
        }
    }

    companion object {
        const val ACTION_EXPORT_NETWORKS =
            "io.github.wifi_password_manager.action.EXPORT_NETWORKS"
        const val ACTION_EXPORT_NETWORKS_RESULT =
            "io.github.wifi_password_manager.action.EXPORT_NETWORKS_RESULT"
        const val ACTION_EXPORT_CURRENT_NETWORK =
            "io.github.wifi_password_manager.action.EXPORT_CURRENT_NETWORK"
        const val ACTION_EXPORT_CURRENT_NETWORK_RESULT =
            "io.github.wifi_password_manager.action.EXPORT_CURRENT_NETWORK_RESULT"

        const val EXTRA_SUCCESS = "extra_success"
        const val EXTRA_PATH = "extra_path"
        const val EXTRA_ERROR = "extra_error"

        const val ERROR_NO_NETWORKS = "no_networks"
        const val ERROR_NO_CURRENT_NETWORK = "no_current_network"
        const val ERROR_UNKNOWN = "unknown"

        private const val EXPORT_DIR = "exports"
    }
}
