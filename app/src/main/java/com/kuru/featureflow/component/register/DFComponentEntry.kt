package com.kuru.featureflow.component.register

/**
 * This will be implemented by the dynamic feature, this is entry point for dynamic feature
 *  the link using service loaders
 *  It will register DFComponentRegistry to DFComponentRegistryManager
 *  The Post Interceptors will be executed in framework
 */
interface DFComponentEntry {
    fun initialize()  //
}