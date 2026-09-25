package com.avidetravel.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avidetravel.app.data.Agent
import com.avidetravel.app.data.AvideApi
import com.avidetravel.app.data.Service
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

data class AvideUiState(
    val services: List<Service> = emptyList(),
    val agents: List<Agent> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null
)

class AvideViewModel : ViewModel() {
    var state by mutableStateOf(AvideUiState())
        private set

    init {
        refresh(initial = true)
    }

    fun refresh(initial: Boolean = false) {
        viewModelScope.launch {
            state = state.copy(
                loading = initial && state.services.isEmpty(),
                refreshing = !initial,
                error = null
            )

            runCatching {
                coroutineScope {
                    val services = async { AvideApi.getServices() }
                    val agents = async { AvideApi.getAgents() }
                    services.await() to agents.await()
                }
            }.onSuccess { (services, agents) ->
                state = AvideUiState(
                    services = services,
                    agents = agents,
                    loading = false,
                    refreshing = false
                )
            }.onFailure { error ->
                state = state.copy(
                    loading = false,
                    refreshing = false,
                    error = error.message ?: "Could not load AvideTravel data."
                )
            }
        }
    }
}
