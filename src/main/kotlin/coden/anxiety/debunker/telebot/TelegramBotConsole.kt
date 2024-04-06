package coden.anxiety.debunker.telebot

import coden.anxiety.debunker.core.api.Console
import org.apache.logging.log4j.kotlin.Logging
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.generics.LongPollingBot
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

class TelegramBotConsole(
    private val bot: LongPollingBot
): Console, Logging {

    private val api = TelegramBotsApi(DefaultBotSession::class.java)

    override fun start() {
        logger.info("Started polling of the telegram bot.")
        try {
            api.registerBot(bot)
        }catch (e: TelegramApiException){
            logger.error("Telegram bot got exception: ${e.message}", e)
        }
    }

    override fun stop() {}
}