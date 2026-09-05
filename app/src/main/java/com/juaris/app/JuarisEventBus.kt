package com.juaris.app

object JuarisEventBus {
    private val listeners = mutableListOf<(String) -> Unit>()

    fun register(listener: (String) -> Unit) {
        listeners.add(listener)
    }

    fun postEvent(event: String) {
        for (listener in listeners) {
            listener(event)
        }
    }
}
