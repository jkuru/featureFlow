package com.kuru.featureflow.component.state


/**
 * Application Level State Store
 * Handle all the lister data for UniDirectional flow of data
 *
 *  Status of Install
 *  Status of Interceptors
 */
sealed class DFComponentStateStore {
    data object NotInstalled : DFComponentStateStore()
    data object Loading : DFComponentStateStore()
    data class Downloading(val progress: Float) : DFComponentStateStore()
    data object Installed : DFComponentStateStore()
    data class ConfirmationRequired(val confirm: () -> Unit) : DFComponentStateStore()
    data class Error(val message: String) : DFComponentStateStore()
}
