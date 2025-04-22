package com.kuru.featureflow.component.zrefregister

import android.util.Log


interface FeatureInitializer {
    fun initialize()
}

object FeatureRegistry {
    private val initializers = mutableListOf<FeatureInitializer>()
    private const val TAG = "DynamicFeatureManager"

    fun registerInitializer(initializer: FeatureInitializer) {
        Log.d(TAG,"FeatureRegistry --> registerInitializer ")
        initializers.add(initializer)
    }

    fun initializeAll() {
        Log.d(TAG,"FeatureRegistry --> initializeAll ")
        initializers.forEach { it.initialize() }
    }
}