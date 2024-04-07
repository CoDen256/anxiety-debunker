package coden.anxiety.debunker.telebot

import org.telegram.telegrambots.meta.generics.LongPollingBot

interface StartableLongPollingBot: LongPollingBot {
    fun start()
}