package coden.anxiety.debunker.telegram.bot

import coden.anxiety.debunker.telegram.keyboard.KeyboardButton
import coden.anxiety.debunker.telegram.keyboard.keyboard
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

val UNRESOLVE = KeyboardButton("↩\uFE0F Unresolve", "UNRESOLVE")
val FULFILL = KeyboardButton("❌ Fucked", "FULLFILL")
val UNFULFILL = KeyboardButton("✅ Fine", "UNFULFILLED")