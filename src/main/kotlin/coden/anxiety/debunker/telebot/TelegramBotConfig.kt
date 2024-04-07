package coden.anxiety.debunker.telebot

data class TelegramBotConfig (
    val token: String,
    val target: Long,
    val username: String,
    val intro: String
)
