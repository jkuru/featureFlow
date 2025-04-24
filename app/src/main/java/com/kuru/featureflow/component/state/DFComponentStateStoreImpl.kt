package com.kuru.featureflow.component.state

import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DFComponentStateStoreImpl(
    private val scope: CoroutineScope,
    private val prefs: SharedPreferences?
) : DFComponentStateStore {
    private val _installationStates = MutableStateFlow<Map<String, InstallationState>>(emptyMap())
    private val _interceptorStates = MutableStateFlow<Map<String, InterceptorState>>(emptyMap())

    override fun getLastAttemptedFeature(): String? {
        return prefs?.getString(LAST_ATTEMPTED_FEATURE_INSTALL, null)
    }

    override fun setLastAttemptedFeature(uri: String) {
        prefs?.edit()?.putString(LAST_ATTEMPTED_FEATURE_INSTALL, uri)?.apply()
    }

    override fun getInstallationState(feature: String): InstallationState {
        return _installationStates.value[feature] ?: InstallationState.NotInstalled
    }

    override fun setInstallationState(feature: String, state: InstallationState) {
        val currentStates = _installationStates.value.toMutableMap()
        currentStates[feature] = state
        _installationStates.value = currentStates
    }

    override fun getInterceptorState(interceptorId: String): InterceptorState {
        return _interceptorStates.value[interceptorId] ?: InterceptorState.Inactive
    }

    override fun setInterceptorState(interceptorId: String, state: InterceptorState) {
        val currentStates = _interceptorStates.value.toMutableMap()
        currentStates[interceptorId] = state
        _interceptorStates.value = currentStates
    }

    override fun getInstallationStateFlow(feature: String): StateFlow<InstallationState> {
        return _installationStates.map { it[feature] ?: InstallationState.NotInstalled }
            .stateIn(scope, SharingStarted.Lazily, InstallationState.NotInstalled)
    }

    companion object {
        const val LAST_ATTEMPTED_FEATURE_INSTALL = "last_attempted_feature_install"
    }
}