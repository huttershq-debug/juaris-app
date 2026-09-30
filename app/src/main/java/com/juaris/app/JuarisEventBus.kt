package com.juaris.app

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object JuarisEventBus {

    // Ein threadsicherer, speicher-optimierter Flow ohne Memory-Leaks!
    private val _events = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 64)
    val events: SharedFlow<String> = _events.asSharedFlow()

    /**
     * Schießt ein Ereignis sicher in das System. 
     * Kann von jedem Thread (UI oder Hintergrund) ausfallsicher aufgerufen werden.
     */
    fun postEvent(event: String) {
        _events.tryEmit(event)
    }
}
