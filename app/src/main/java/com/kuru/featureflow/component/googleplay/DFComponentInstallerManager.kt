package com.kuru.featureflow.component.googleplay

import android.util.Log
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.kuru.featureflow.component.state.InstallationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DFComponentInstallerManager(
    private val manager: SplitInstallManager,
    private val scope: CoroutineScope,
    private val mutex: Mutex,
    private val installStates: MutableSharedFlow<Pair<String, InstallationState>>
) : DFComponentInstaller {

    override fun installComponent(componentName: String): Flow<InstallationState> {
        return flowForInstallation(componentName, isRetry = false)
    }

    override fun retryComponentInstall(componentName: String): Flow<InstallationState> {
        return flowForInstallation(componentName, isRetry = true)
    }

    private fun flowForInstallation(
        componentName: String,
        isRetry: Boolean
    ): Flow<InstallationState> {
        return installStates
            .filter { it.first == componentName }
            .map { it.second }
            .onStart {
                scope.launch {
                    mutex.withLock {
                        try {
                            // Check if the component is already installed
                            if (manager.installedModules.contains(componentName)) {
                                Log.d(TAG, "Component $componentName is already installed")
                                emit(InstallationState.Installed)
                                return@launch
                            }

                            // Build and start the installation request
                            val request = SplitInstallRequest.newBuilder()
                                .addModule(componentName)
                                .build()
                            emit(InstallationState.Installing)
                            manager.startInstall(request)
                                .addOnSuccessListener { sessionId ->
                                    Log.d(
                                        TAG,
                                        "${if (isRetry) "Retry" else ""} Installation started for $componentName with session ID: $sessionId"
                                    )
                                    installStates.tryEmit(componentName to InstallationState.Installed)
                                }
                                .addOnFailureListener { exception ->
                                    Log.e(
                                        TAG,
                                        "Failed to start ${if (isRetry) "retry" else ""} installation for $componentName",
                                        exception
                                    )
                                    installStates.tryEmit(
                                        componentName to InstallationState.Failed(
                                            exception.message ?: "Unknown error"
                                        )
                                    )
                                }
                        } catch (e: Exception) {
                            Log.e(
                                TAG,
                                "Error during ${if (isRetry) "retry" else ""} installation of $componentName",
                                e
                            )
                            installStates.tryEmit(
                                componentName to InstallationState.Failed(
                                    e.message ?: "Unknown error"
                                )
                            )
                        }
                    }
                }
            }
    }

    override fun isComponentInstalled(componentName: String): Boolean {
        return manager.installedModules.contains(componentName)
    }

    companion object {
        private const val TAG = "DFComponentInstaller"
    }
}