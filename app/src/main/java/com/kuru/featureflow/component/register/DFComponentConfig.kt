package com.kuru.featureflow.component.register

/**
 * This will be leveraged by the feature teams in their projects
 *  to
 *  A) define the entry
 *  B)  behaviour
 *      1) Retry by lifecycle of App
 *      2) Mapping of session/meta data as part of google API to State store
 *      3) UX
 *      4) Post initialization intercepts
 *  c) Pre Condition functions
 *     1)  Compatibility function
 *     2)  Network
 *  d) define the module name
 *  e) B and C are Interceptor, that can be chained function, these functions will return boolean
 *     if any boolean return false , then logs are part of State Store
 *
 */
data class DFComponentConfig(
    val route: String,  //route will be the module name
    // *** ADDED: The fully qualified name of the class to load/initialize ***
    // This could be the FeatureRegistrar implementation itself, or an object
    // whose init block starts the process. Let's assume it's the Registrar for simplicity.
    // val registrarClassName: String, Will take service loader approach
    val retryCount: Int = 0, // MVP it will be 1
    val listOfDFComponentInterceptor: List<DFComponentInterceptor> = emptyList()
)