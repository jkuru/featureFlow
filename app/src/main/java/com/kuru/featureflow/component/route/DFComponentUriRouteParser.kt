package com.kuru.featureflow.component.route

import android.net.Uri

/**
 * Utility class for parsing URIs into DFComponentRoute objects.
 * Provides a static method to extract route information from a given URI.
 */
class DFComponentUriRouteParser {

    companion object {
        /**
         * Extracts the DFComponentRoute from the provided raw URI.
         *
         * @param rawURI The URI string to parse.
         * @return A DFComponentRoute object with the parsed route information or a failed status if parsing fails.
         */
        fun extractRoute(rawURI: String): DFComponentRoute {
            val uri = try {
                Uri.parse(rawURI)
            } catch (e: Exception) {
                return DFComponentRoute(
                    path = "",
                    route = "",
                    navigationKey = "",
                    params = emptyList(),
                    status = "failed"
                )
            }

            // Ensure the URI starts with the expected prefix
            if (!uri.path?.startsWith("/chase/df/")!!) {
                return DFComponentRoute(
                    path = "",
                    route = "",
                    navigationKey = "",
                    params = emptyList(),
                    status = "failed"
                )
            }

            val pathSegments = uri.pathSegments
            val path = uri.path ?: return DFComponentRoute(
                path = "",
                route = "",
                navigationKey = "",
                params = emptyList(),
                status = "failed"
            )

            // Determine if it's a route or navigation key based on the URI structure
            val isRoute = pathSegments.contains("route")
            val isNavigation = pathSegments.contains("navigation/key")

            // Extract parameters from query and path
            val params = mutableListOf<String>()
            uri.queryParameterNames.forEach { paramName ->
                uri.getQueryParameter(paramName)?.let { params.add("$paramName=$it") }
            }

            // Extract path parameters for parameterized paths (e.g., "/chase/df/route/{id}")
            val pathParts = path.split('/')
            pathParts.forEach { part ->
                if (part.startsWith("{") && part.endsWith("}")) {
                    val paramName = part.removeSurrounding("{", "}")
                    val paramIndex = pathParts.indexOf(part)
                    if (paramIndex < pathSegments.size) {
                        params.add("$paramName=${pathSegments[paramIndex]}")
                    }
                }
            }

            return when {
                isRoute -> {
                    // For routes: chase/df/route/someRoute?param1=value1
                    val routeSegmentIndex = pathSegments.indexOf("route")
                    val routeValue = pathSegments.getOrNull(routeSegmentIndex + 1) ?: ""
                    DFComponentRoute(
                        path = path,
                        route = routeValue,
                        navigationKey = "",
                        params = params,
                        status = "success"
                    )
                }
                isNavigation -> {
                    // For navigation: chase/df/navigation/key/someKey?param1=value1
                    val keySegmentIndex = pathSegments.indexOf("key")
                    val keyValue = pathSegments.getOrNull(keySegmentIndex + 1) ?: ""
                    DFComponentRoute(
                        path = path,
                        route = "",
                        navigationKey = keyValue,
                        params = params,
                        status = "success"
                    )
                }
                else -> {
                    // Default case for basic chase/df/ paths
                    DFComponentRoute(
                        path = path,
                        route = pathSegments.lastOrNull() ?: "",
                        navigationKey = "",
                        params = params,
                        status = "success"
                    )
                }
            }
        }
    }
}