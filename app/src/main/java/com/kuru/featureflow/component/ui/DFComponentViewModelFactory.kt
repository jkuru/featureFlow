package com.kuru.featureflow.component.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kuru.featureflow.component.boot.DFSDKBootStrap

class DFComponentViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DFComponentViewModel::class.java)) {
            return DFComponentViewModel( stateStore = DFSDKBootStrap.getStateStore(),
                registry = DFSDKBootStrap.getComponentRegistry(),
                context = context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}