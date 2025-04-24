package com.kuru.featureflow.component.state

sealed class InstallationState {
    object NotInstalled : InstallationState()
    data object Installing : InstallationState()
    object Installed : InstallationState()
    data class Failed(val error: String) : InstallationState()
}