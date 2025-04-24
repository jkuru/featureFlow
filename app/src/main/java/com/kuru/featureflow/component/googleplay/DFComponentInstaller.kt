package com.kuru.featureflow.component.googleplay

import com.kuru.featureflow.component.state.InstallationState
import kotlinx.coroutines.flow.Flow

interface DFComponentInstaller {
    fun isComponentInstalled(componentName: String): Boolean
    fun installComponent(componentName: String): Flow<InstallationState>
    fun retryComponentInstall(componentName: String): Flow<InstallationState>
}