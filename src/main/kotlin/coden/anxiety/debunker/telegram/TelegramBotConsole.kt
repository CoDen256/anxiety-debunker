package coden.anxiety.debunker.telegram

import coden.anxiety.debunker.core.api.Console
import coden.anxiety.debunker.telegram.bot.StartableLongPollingBot
import org.apache.logging.log4j.kotlin.Logging
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

class TelegramBotConsole(
    private vararg val bots: StartableLongPollingBot
): Console, Logging {

    private val api = TelegramBotsApi(DefaultBotSession::class.java)

    override fun start() {
        logger.info("Starting the polling of the telegram bots")
        try {
            for (bot in bots) {
                api.registerBot(bot)
                bot.run()
                logger.info("Started ${bot.name()}!")
            }
        }catch (e: TelegramApiException){
            logger.error("Telegram bot got exception: ${e.message}", e)
        }
    }

    override fun stop() {}
}