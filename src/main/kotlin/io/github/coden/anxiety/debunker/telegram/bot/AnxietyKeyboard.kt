package io.github.coden.anxiety.debunker.telegram.bot

import io.github.coden.telegram.keyboard.KeyboardButton
import io.github.coden.telegram.keyboard.keyboard
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup

fun withNewAnxietyButtons(): InlineKeyboardMarkup {
    return keyboard {
        row { b(FULFILL); b(UNFULFILL) }
    }
}

fun withResolvedAnxietyButtons(): InlineKeyboardMarkup {
    return keyboard {
        row { b(UNRESOLVE) }
    }
}

fun withDeletedAnxietyButtons(): InlineKeyboardMarkup {
    return keyboard {
        row { b(DELETE_MESSAGE) }
    }
}

val UNRESOLVE = KeyboardButton("↩\uFE0F Unresolve", "UNRESOLVE")
val DELETE_MESSAGE = KeyboardButton("\uD83D\uDC80 Delete", "DELETE_MESSAGE")
val FULFILL = KeyboardButton("❌ Fucked", "FULLFILL")
val UNFULFILL = KeyboardButton("✅ Fine", "UNFULFILLED")