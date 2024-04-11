package io.github.coden.anxiety.debunker.telegram

data class TelegramBotConfig (
    val token: String,
    val target: Long,
    val username: String,
    val intro: String
)
