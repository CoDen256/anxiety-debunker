package coden.anxiety.debunker.telegram.bot

import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer


interface StartableLongPollingBot: LongPollingUpdateConsumer {
    fun name(): String { return this.javaClass.simpleName }
    fun run()
    fun token(): String
    fun onRegister()
}