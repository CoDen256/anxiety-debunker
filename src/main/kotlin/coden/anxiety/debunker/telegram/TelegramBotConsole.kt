package coden.anxiety.debunker.telegram

import coden.anxiety.debunker.core.api.Console
import coden.anxiety.debunker.telegram.bot.StartableLongPollingBot
import org.apache.logging.log4j.kotlin.Logging
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication
import org.telegram.telegrambots.longpolling.util.DefaultGetUpdatesGenerator
import org.telegram.telegrambots.meta.TelegramUrl
import org.telegram.telegrambots.meta.api.methods.updates.GetUpdates
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

class TelegramBotConsole(
    private vararg val bots: StartableLongPollingBot
): Console, Logging {

    private val allowedUpdates: List<String> = listOf(
        "message_reaction",
        "update_id",
        "edited_message",
        "message",
        "callback_query",
        "chosen_inline_result",
        "inline_query"
    )
    private val api = TelegramBotsLongPollingApplication()
    private val generator = object: DefaultGetUpdatesGenerator(){
        override fun apply(lastReceivedUpdate: Int): GetUpdates {
            return GetUpdates
                .builder()
                .limit(1)
                .timeout(1)
                .offset(lastReceivedUpdate + 1)
                .allowedUpdates(allowedUpdates)
                .build();
        }
    }

    override fun start() {
        logger.info("Starting the polling of the telegram bots")
        try {
            for (bot in bots) {
                api.registerBot(bot.token(), { TelegramUrl.DEFAULT_URL}, generator, bot)
                bot.onRegister()
                bot.run()
                logger.info("Started ${bot.name()}!")
            }
        }catch (e: TelegramApiException){
            logger.error("Telegram bot got exception: ${e.message}", e)
        }
    }

    override fun stop() {}
}