package com.kuru.featureflow.component.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuru.featureflow.component.boot.DFSDKBootStrap
import com.kuru.featureflow.component.register.DFComponentEntry
import com.kuru.featureflow.component.register.DFComponentRegistry
import com.kuru.featureflow.component.state.DFComponentStateStore
import com.kuru.featureflow.component.state.InstallationState
import com.kuru.featureflow.component.state.InterceptorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.ServiceLoader

sealed class DFComponentState {
    data object Loading : DFComponentState()
    data class Error(
        val message: String,
        val errorType: ErrorType,
        val feature: String? = null
    ) : DFComponentState()

    data class Success(
        val feature: String,
        val installationState: InstallationState
    ) : DFComponentState()
}

enum class ErrorType {
    NETWORK,
    INSTALLATION,
    VALIDATION,
    UNKNOWN,
    PRE_INSTALL_INTERCEPTOR,
    POST_INSTALL_INTERCEPTOR
}

sealed class DFComponentIntent {
    data class LoadFeature(val feature: String) : DFComponentIntent()
    object Retry : DFComponentIntent()
    data class InstallComponent(val feature: String) : DFComponentIntent()
    data class InstallationResult(val feature: String, val state: InstallationState) :
        DFComponentIntent()
}

class DFComponentViewModel(
    private val stateStore: DFComponentStateStore,
    private val registry: DFComponentRegistry,
    @SuppressLint("StaticFieldLeak") private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "DFComponentViewModel"
    }

    private val _currentFeature = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow<DFComponentState>(DFComponentState.Loading)
    val uiState: StateFlow<DFComponentState> = _uiState.asStateFlow()

    fun processIntent(intent: DFComponentIntent) {
        when (intent) {
            is DFComponentIntent.LoadFeature -> loadFeature(intent.feature)
            is DFComponentIntent.Retry -> retryLastFeature()
            is DFComponentIntent.InstallComponent -> installComponent(intent.feature)
            is DFComponentIntent.InstallationResult -> handleInstallationResult(
                intent.feature,
                intent.state
            )
        }
    }

    private fun loadFeature(feature: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading feature: $feature")
                _currentFeature.value = feature
                stateStore.setLastAttemptedFeature(feature)

                // Validate feature name
                if (feature.isBlank()) {
                    val error = "Feature name cannot be empty"
                    Log.e(TAG, error)
                    stateStore.setInstallationState(feature, InstallationState.Failed(error))
                    updateUiState(feature, ErrorType.VALIDATION, error)
                    return@launch
                }

                // Get installation state from store
                val installationState = stateStore.getInstallationState(feature)
                Log.d(TAG, "Current installation state for $feature: $installationState")

                // Update installation state based on current state
                when (installationState) {
                    is InstallationState.NotInstalled -> {
                        Log.d(TAG, "Starting installation for $feature")
                        installComponent(feature)
                    }

                    is InstallationState.Installing -> {
                        Log.d(TAG, "Feature $feature is already installing")
                        updateUiState(feature, null, null)
                    }

                    is InstallationState.Installed -> {
                        Log.d(TAG, "Feature $feature is already installed")
                        updateUiState(feature, null, null)
                    }

                    is InstallationState.Failed -> {
                        Log.e(TAG, "Previous installation failed: ${installationState.error}")
                        updateUiState(feature, ErrorType.INSTALLATION, installationState.error)
                    }
                }
            } catch (e: Exception) {
                val error = "Failed to load feature $feature: ${e.message}"
                Log.e(TAG, error, e)
                stateStore.setInstallationState(feature, InstallationState.Failed(error))
                updateUiState(feature, ErrorType.UNKNOWN, error)
            }
        }
    }

    private fun retryLastFeature() {
        val lastFeature = stateStore.getLastAttemptedFeature()
        if (lastFeature != null) {
            Log.d(TAG, "Retrying last feature: $lastFeature")
            viewModelScope.launch {
                val installer = DFSDKBootStrap.getComponentInstaller(context, "featureflow_prefs")
                installer.retryComponentInstall(lastFeature).collect { state ->
                    processIntent(DFComponentIntent.InstallationResult(lastFeature, state))
                }
            }
        } else {
            val error = "No feature to retry"
            Log.e(TAG, error)
            updateUiState(null, ErrorType.VALIDATION, error)
        }
    }

    private fun installComponent(feature: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Installing component: $feature")

                //TODO the pre interceptors will be standard checks , not configurable , need to fix this
                // Retrieve DFComponentConfig from registry
                val config = registry.getConfig(feature) ?: run {
                    val error = "No configuration found for feature: $feature"
                    Log.e(TAG, error)
                    stateStore.setInstallationState(feature, InstallationState.Failed(error))
                    updateUiState(feature, ErrorType.VALIDATION, error)
                    return@launch
                }

                // Execute pre-install interceptors
                val preInstallInterceptors =
                    config.listOfDFComponentInterceptor.filter { it.preInstall }
                for ((index, interceptor) in preInstallInterceptors.withIndex()) {
                    val interceptorId = "$feature-pre-$index"
                    stateStore.setInterceptorState(interceptorId, InterceptorState.Active)
                    val result = interceptor.task()
                    stateStore.setInterceptorState(
                        interceptorId,
                        if (result) InterceptorState.Completed else InterceptorState.Failed("Pre-install interceptor $index failed")
                    )
                    if (!result) {
                        val error = "Pre-install interceptor $index failed for $feature"
                        Log.e(TAG, error)
                        stateStore.setInstallationState(feature, InstallationState.Failed(error))
                        updateUiState(feature, ErrorType.PRE_INSTALL_INTERCEPTOR, error)
                        return@launch
                    }
                }

                // Proceed with installation
                stateStore.setInstallationState(feature, InstallationState.Installing)
                updateUiState(feature, null, null)
                val installer = DFSDKBootStrap.getComponentInstaller(
                    context,
                    DFComponentActivity.SHARED_PREFS_NAME
                )
                installer.installComponent(feature).collect { state ->
                    processIntent(DFComponentIntent.InstallationResult(feature, state))
                }
            } catch (e: Exception) {
                val error = "Installation failed for $feature: ${e.message}"
                Log.e(TAG, error, e)
                stateStore.setInstallationState(feature, InstallationState.Failed(error))
                updateUiState(feature, ErrorType.INSTALLATION, error)
            }
        }
    }

    /**
     * How It Works
     * 1) Defining the Service Provider Interface (SPI)
     *   The framework defines an interface, say DFComponentEntry, which acts as the contract that all dynamic feature modules must implement.
     *   This is the "entry point" interface that the framework expects.
     * 2) Dynamic Feature Module Implementation
     *    Each dynamic feature module provides its own implementation of DFComponentEntry.
     *    For example, a "plants" module might have a class called PlantEntry that implements DFComponentEntry.
     * 3) Service Configuration with META-INF
     *     Each dynamic feature module includes a special configuration file located at:
     *     META-INF/services/com.kuru.featureflow.component.register.DFComponentEntry
     *     The file’s name is the fully qualified name of the SPI interface (DFComponentEntry in this case).
     *     Inside this file, the module lists the fully qualified name of its implementation class, e.g.:
     *     com.kuru.featureflow.plant.PlantEntry
     *  This file acts as a bridge, telling the ServiceLoader which class to load for the given interface.
     *  4) Loading with ServiceLoader
     *      When the framework executes:
     *     val serviceLoader = ServiceLoader.load(DFComponentEntry::class.java, context.classLoader)
     *     The ServiceLoader scans the entire classpath (via the provided context.classLoader) for all files
     *     named META-INF/services/com.kuru.featureflow.component.register.DFComponentEntry.
     *     It reads the class names listed in these files (e.g., com.kuru.featureflow.plant.PlantEntry).
     *     Using reflection, it instantiates these classes, creating instances of the DFComponentEntry implementations.
     *  5) Dynamic Feature Modules and Classpath
     *     When a dynamic feature module is installed (e.g., via Play Feature Delivery or another mechanism),
     *     its classes and resources—including the META-INF/services/ directory—become part of the application’s classpath.
     *     The context.classLoader ensures that the ServiceLoader can access this updated classpath, allowing it to find
     *     and load the entry point classes from the newly installed modules.
     *  6) Initializing the Modules
     *     After loading, the framework can iterate over the ServiceLoader and call methods on the loaded implementations, like:
     *     serviceLoader.forEach { entry -> entry.initialize() }
     *     This allows each module to register itself or perform any necessary setup with the framework.
     *
     *     What’s Happening in Point 3?
     * Point 3 refers to setting up a configuration file in the "plants" dynamic feature module that tells the ServiceLoader where to find the PlantEntry class. This file lives in a specific directory (META-INF/services/) and follows a standard Java convention for service discovery. Let’s dive into how this works.
     *
     * The Setup: Interface and Implementation
     * First, let’s establish the pieces involved:
     *
     * The Interface (SPI - Service Provider Interface)
     * You have an interface defined in your framework:
     * kotlin
     *
     * Copy
     * package com.kuru.featureflow.component.register
     *
     * interface DFComponentEntry {
     *     fun initialize()
     * }
     * This is the contract that all dynamic feature modules must follow.
     * The Implementation in the "plants" Module
     * In your "plants" module, you’ve created a class that implements this interface:
     * kotlin
     *
     * Copy
     * package com.kuru.featureflow.plant
     *
     * object PlantEntry : DFComponentEntry {
     *     override fun initialize() {
     *         // Initialization logic for the plants module
     *     }
     * }
     * PlantEntry is the entry point for the "plants" module, but the framework doesn’t know about it yet. We need a way to connect the two.
     * The Role of META-INF/services/
     * To bridge the gap between the interface (DFComponentEntry) and its implementation (PlantEntry), you use a configuration file in the META-INF/services/ directory. Here’s how you set it up:
     *
     * Directory and File Creation
     * Inside the "plants" module, you create the following structure:
     * text
     *
     * Copy
     * plants-module/
     * ├── src/
     * │   └── com/kuru/featureflow/plant/
     * │       └── PlantEntry.kt
     * ├── META-INF/
     * │   └── services/
     * │       └── com.kuru.featureflow.component.register.DFComponentEntry
     * The file is named exactly after the fully qualified name of the interface: com.kuru.featureflow.component.register.DFComponentEntry.
     * This naming convention is critical—it’s how ServiceLoader knows which interface this file corresponds to.
     * File Contents
     * Inside the com.kuru.featureflow.component.register.DFComponentEntry file, you write one line:
     * text
     *
     * Copy
     * com.kuru.featureflow.plant.PlantEntry
     * This is the fully qualified name of the implementation class (PlantEntry).
     * The file acts like a pointer, saying: “Hey, if you’re looking for implementations of DFComponentEntry, here’s one: PlantEntry.”
     * How ServiceLoader Uses This to Find PlantEntry
     * Now, let’s connect this to the ServiceLoader. In your framework, you likely have code like this:
     *
     * kotlin
     *
     * Copy
     * val serviceLoader = ServiceLoader.load(DFComponentEntry::class.java, context.classLoader)
     * Here’s what happens when this line runs:
     *
     * Scanning the Classpath
     * The ServiceLoader searches the entire classpath (which includes the "plants" module once it’s installed) for any files located at META-INF/services/com.kuru.featureflow.component.register.DFComponentEntry.
     * The context.classLoader ensures it can see all loaded modules, including dynamic ones like "plants."
     * Reading the File
     * When it finds the file META-INF/services/com.kuru.featureflow.component.register.DFComponentEntry in the "plants" module, it opens it and reads the contents: com.kuru.featureflow.plant.PlantEntry.
     * Loading the Class
     * Using reflection, the ServiceLoader loads the PlantEntry class based on the name it found (com.kuru.featureflow.plant.PlantEntry).
     * Since PlantEntry is a Kotlin object (a singleton), it retrieves the instance of PlantEntry.
     * If it were a regular class, it would create a new instance (assuming it has a no-arg constructor).
     * Providing the Instance
     * The ServiceLoader then makes PlantEntry available to your code as an implementation of DFComponentEntry. You can iterate over all found implementations like this:
     * kotlin
     *
     * Copy
     * serviceLoader.forEach { entry ->
     *     entry.initialize() // Calls PlantEntry.initialize()
     * }
     */
    private fun handleInstallationResult(feature: String, state: InstallationState) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Handling installation result for $feature: $state")
                stateStore.setInstallationState(feature, state)

                when (state) {
                    is InstallationState.Installed -> {
                        // Link using service loaders
                        // Initialize the service loader which will make the feature components register route and post interceptors
                        val serviceLoader =
                            ServiceLoader.load(DFComponentEntry::class.java, context.classLoader)
                        serviceLoader.forEach { entry ->
                            try {
                                entry.initialize() // Registers the feature's route and configuration
                            } catch (e: Exception) {
                                Log.e(
                                    TAG,
                                    "Failed to initialize DFComponentEntry for $feature: ${e.message}",
                                    e
                                )
                            }
                        }

                        // Retrieve DFComponentConfig from registry
                        val config = registry.getConfig(feature) ?: run {
                            val error = "No configuration found for feature: $feature"
                            Log.e(TAG, error)
                            stateStore.setInstallationState(
                                feature,
                                InstallationState.Failed(error)
                            )
                            updateUiState(feature, ErrorType.VALIDATION, error)
                            return@launch
                        }

                        // Post Install Interceptors using service loader pattern
                        val postInstallInterceptors =
                            config.listOfDFComponentInterceptor.filter { !it.preInstall }
                        for ((index, interceptor) in postInstallInterceptors.withIndex()) {
                            val interceptorId = "$feature-post-$index"
                            stateStore.setInterceptorState(interceptorId, InterceptorState.Active)
                            val result = interceptor.task()
                            stateStore.setInterceptorState(
                                interceptorId,
                                if (result) InterceptorState.Completed else InterceptorState.Failed(
                                    "Post-install interceptor $index failed"
                                )
                            )
                            if (!result) {
                                val error = "Post-install interceptor $index failed for $feature"
                                Log.w(TAG, error)
                                updateUiState(feature, ErrorType.POST_INSTALL_INTERCEPTOR, error)
                                // Note: Post-install failure does not fail the installation
                            }
                        }

                        // Update UI state to indicate success
                        updateUiState(feature, null, null)
                    }
                    // Other cases remain unchanged
                    is InstallationState.Failed -> {
                        updateUiState(feature, ErrorType.INSTALLATION, state.error)
                    }

                    else -> {
                        updateUiState(feature, null, null) // Installing or other transient states
                    }
                }
            } catch (e: Exception) {
                val error = "Failed to handle installation result for $feature: ${e.message}"
                Log.e(TAG, error, e)
                stateStore.setInstallationState(feature, InstallationState.Failed(error))
                updateUiState(feature, ErrorType.UNKNOWN, error)
            }
        }
    }

    private fun updateUiState(feature: String?, errorType: ErrorType?, errorMessage: String?) {
        when {
            errorType != null && errorMessage != null -> {
                _uiState.value = DFComponentState.Error(
                    message = errorMessage,
                    errorType = errorType,
                    feature = feature
                )
            }

            feature != null && errorType == null -> {
                _uiState.value = DFComponentState.Success(
                    feature = feature,
                    installationState = stateStore.getInstallationState(feature)
                )
            }

            else -> {
                _uiState.value = DFComponentState.Loading
            }
        }
    }
}