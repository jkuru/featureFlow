package com.kuru.featureflow.component.googleplay

import android.content.Context
import android.util.Log
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallSessionState
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import com.kuru.featureflow.component.state.DFErrorCode
import com.kuru.featureflow.component.state.DFInstallProgress
import com.kuru.featureflow.component.state.InstallationState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DFComponentInstallerManager @Inject constructor(
    private val splitInstallManager: SplitInstallManager,
    @ApplicationContext private val context: Context
) : DFComponentInstaller {

    companion object {
        private const val TAG = "DFComponentInstallerMgr"
    }

    private val activeListeners = ConcurrentHashMap<Int, SplitInstallStateUpdatedListener>()

    override fun isComponentInstalled(componentName: String): Boolean {
        val installed = splitInstallManager.installedModules.contains(componentName)
        Log.d(TAG, "isComponentInstalled($componentName): $installed")
        return installed
    }

    override fun installComponent(componentName: String): Flow<DFInstallProgress> { // Changed return type
        if (isComponentInstalled(componentName)) {
            Log.i(TAG, "Install requested for already installed component: $componentName")
            // Wrap the final state in InstallProgress
            return flowOf(DFInstallProgress(InstallationState.Installed))
        }

        val request = SplitInstallRequest.newBuilder()
            .addModule(componentName)
            .build()

        return callbackFlow { // Flow<InstallProgress>
            var currentSessionId = 0

            val listener = SplitInstallStateUpdatedListener { state -> // state is SplitInstallSessionState
                if (state.sessionId() == currentSessionId) {
                    // Map the Play Core state to our framework state AND create InstallProgress
                    val installProgress = mapSessionStateToInstallProgress(state, componentName) // Use new helper
                    val newState = installProgress.frameworkState // Extract framework state for logging/logic

                    Log.d(TAG, "Listener received state for $componentName (Session $currentSessionId): $newState (Raw Status: ${state.status()})")

                    // Try sending the InstallProgress object
                    val success = trySend(installProgress).isSuccess

                    if (isTerminalState(newState) || !success) {
                        Log.i(
                            TAG,
                            "Terminal state ($newState) or channel closed ($success) for $componentName (Session $currentSessionId). Cleaning up listener."
                        )
                        cleanupListener(currentSessionId)
                        if (!isClosedForSend) {
                            close()
                        }
                    }
                } else {
                    Log.v(TAG, "Listener ignored state update for unrelated session ${state.sessionId()}")
                }
            }

            try {
                Log.i(TAG, "Requesting install for $componentName...")
                currentSessionId = splitInstallManager.startInstall(request).await()
                Log.i(TAG, "Install request initiated for $componentName. Session ID: $currentSessionId")

                val existingListener = activeListeners.putIfAbsent(currentSessionId, listener)

                if (existingListener == null) {
                    splitInstallManager.registerListener(listener)
                    Log.i(TAG, "Registered listener for $componentName (Session $currentSessionId).")

                    // Emit an initial state based on current session status or Pending
                    val currentSessionState = splitInstallManager.getSessionState(currentSessionId).await()
                    val initialProgress = if (currentSessionState != null) {
                        mapSessionStateToInstallProgress(currentSessionState, componentName) // Use new helper
                    } else {
                        DFInstallProgress(InstallationState.Pending) // Fallback initial state
                    }
                    Log.d(TAG, "Emitting initial state for $componentName (Session $currentSessionId): ${initialProgress.frameworkState}")
                    trySend(initialProgress)
                } else {
                    Log.w(TAG, "Listener already active for session $currentSessionId. New listener not registered.")
                    val currentSessionState = splitInstallManager.getSessionState(currentSessionId).await()
                    if (currentSessionState != null) {
                        trySend(mapSessionStateToInstallProgress(currentSessionState, componentName)) // Use new helper
                    } else {
                        trySend(DFInstallProgress(InstallationState.Unknown)) // Or Failed state
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start install request or register listener for $componentName", e)
                trySend(DFInstallProgress(InstallationState.Failed(DFErrorCode.UNKNOWN_ERROR))) // Wrap in InstallProgress
                close(e)
            }

            awaitClose {
                Log.i(TAG, "Flow closing (awaitClose) for $componentName (Session $currentSessionId). Ensuring listener cleanup.")
                cleanupListener(currentSessionId)
            }
        }
    }

    override fun retryComponentInstall(componentName: String): Flow<DFInstallProgress> { // Changed return type
        Log.i(TAG, "Retry requested for $componentName. Starting installation flow.")
        return installComponent(componentName)
    }

    private fun cleanupListener(sessionId: Int) {
        val listenerToRemove = activeListeners.remove(sessionId)
        if (listenerToRemove != null) {
            try {
                splitInstallManager.unregisterListener(listenerToRemove)
                Log.i(TAG, "Listener successfully unregistered for session $sessionId")
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering listener for session $sessionId", e)
            }
        } else {
            Log.w(TAG, "Attempted to clean up listener for session $sessionId, but it was not found in active map.")
        }
    }

    /**
     * Maps the Play Core SplitInstallSessionState to the framework's InstallProgress wrapper.
     * Includes the original Play Core state in the wrapper when confirmation is required.
     */
    private fun mapSessionStateToInstallProgress(state: SplitInstallSessionState, moduleName: String): DFInstallProgress {
        if (!state.moduleNames().contains(moduleName)) {
            Log.w(TAG, "State update for session ${state.sessionId()} does not list module $moduleName. Status: ${state.status()}")
            // Decide how to handle this - maybe return Unknown state?
        }

        val frameworkState: InstallationState = when (state.status()) {
            SplitInstallSessionStatus.PENDING -> InstallationState.Pending
            SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> InstallationState.RequiresConfirmation // Mapped state
            SplitInstallSessionStatus.DOWNLOADING -> {
                val totalBytes = state.totalBytesToDownload()
                val progress = if (totalBytes > 0) {
                    ((state.bytesDownloaded() * 100) / totalBytes).toInt()
                } else { 0 }
                InstallationState.Downloading(progress.coerceIn(0, 100))
            }
            SplitInstallSessionStatus.DOWNLOADED -> InstallationState.Installing(0)
            SplitInstallSessionStatus.INSTALLING -> InstallationState.Installing(100)
            SplitInstallSessionStatus.INSTALLED -> InstallationState.Installed
            SplitInstallSessionStatus.FAILED -> InstallationState.Failed(DFErrorCode.fromSplitInstallErrorCode(state.errorCode()))
            SplitInstallSessionStatus.CANCELING -> InstallationState.Canceling
            SplitInstallSessionStatus.CANCELED -> InstallationState.Canceled
            SplitInstallSessionStatus.UNKNOWN -> InstallationState.Unknown
            else -> InstallationState.Unknown
        }

        // Create the InstallProgress object, including the raw state if confirmation is needed
        return DFInstallProgress(
            frameworkState = frameworkState,
            playCoreState = if (frameworkState is InstallationState.RequiresConfirmation) state else null
        )
    }


    private fun isTerminalState(state: InstallationState): Boolean {
        return when (state) {
            is InstallationState.Installed,
            is InstallationState.Failed,
            is InstallationState.Canceled -> true
            else -> false
        }
    }
}