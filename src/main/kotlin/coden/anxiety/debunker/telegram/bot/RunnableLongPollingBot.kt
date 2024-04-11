package coden.anxiety.debunker.telegram.bot

import org.telegram.telegrambots.meta.generics.LongPollingBot


interface RunnableLongPollingBot: LongPollingBot {
    fun name(): String { return this.javaClass.simpleName }
    fun run()
}