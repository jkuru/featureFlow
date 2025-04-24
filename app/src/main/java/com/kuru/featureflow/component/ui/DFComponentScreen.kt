package com.kuru.featureflow.component.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun DFComponentScreen(viewModel: DFComponentViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    when (uiState) {
        is DFComponentState.Loading -> {
            CircularProgressIndicator()
        }
        is DFComponentState.Success -> {
            val successState = uiState as DFComponentState.Success
            Text("Feature: ${successState.feature}, State: ${successState.installationState}")
        }
        is DFComponentState.Error -> {
            val errorState = uiState as DFComponentState.Error
            Column {
                Text("Error: ${errorState.message}")
                if (errorState.errorType == ErrorType.INSTALLATION) {
                    Button(onClick = { viewModel.processIntent(DFComponentIntent.Retry) }) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}