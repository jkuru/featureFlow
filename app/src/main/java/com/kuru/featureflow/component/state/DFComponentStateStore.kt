package com.kuru.featureflow.component.state

import kotlinx.coroutines.flow.StateFlow

/**
 * Role of DFComponentStateStore
 * The DFComponentStateStore is designed as the single source of truth for the raw, application-wide state of features. Here’s what it does:
 *
 * Centralized State Management: It holds the installation states (InstallationState) and interceptor states (InterceptorState) for all features, accessible across the app.
 * Persistence: It uses SharedPreferences to store data like the last attempted URI, ensuring durability across app restarts.
 * Raw Data: It provides states like NotInstalled, Installing, Installed, or Failed, which reflect the actual status of feature installations.
 */
interface DFComponentStateStore {
    fun getLastAttemptedFeature(): String?
    fun setLastAttemptedFeature(uri: String)
    fun getInstallationState(feature: String): InstallationState
    fun setInstallationState(feature: String, state: InstallationState)
    fun getInterceptorState(interceptorId: String): InterceptorState
    fun setInterceptorState(interceptorId: String, state: InterceptorState)
    fun getInstallationStateFlow(feature: String): StateFlow<InstallationState>
}
