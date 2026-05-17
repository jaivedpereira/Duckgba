package com.duckgba.ui.home

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.duckgba.DuckgbaApplication
import com.duckgba.data.RomEntry
import com.duckgba.data.RomRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RomRepository = (application as DuckgbaApplication).romRepository

    val roms: StateFlow<List<RomEntry>> get() = repository.roms

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun importRom(uri: Uri, displayName: String?) {
        viewModelScope.launch {
            val result = repository.importRom(uri, displayName)
            if (result.isSuccess) {
                _events.send(HomeEvent.ImportSuccess(result.getOrThrow().displayName))
            } else {
                _events.send(HomeEvent.ImportFailure(result.exceptionOrNull()?.message))
            }
        }
    }

    fun deleteRom(entry: RomEntry) {
        viewModelScope.launch { repository.deleteRom(entry) }
    }
}

sealed interface HomeEvent {
    data class ImportSuccess(val name: String) : HomeEvent
    data class ImportFailure(val message: String?) : HomeEvent
}
