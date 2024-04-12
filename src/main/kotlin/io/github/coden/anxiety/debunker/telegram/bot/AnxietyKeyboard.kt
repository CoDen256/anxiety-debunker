package io.github.coden.anxiety.debunker.telegram.bot

import io.github.coden.telegram.keyboard.Keyboard
import io.github.coden.telegram.keyboard.KeyboardButton
import io.github.coden.telegram.keyboard.keyboard

fun withNewAnxietyButtons(): Keyboard {
    return keyboard {
        row { b(FULFILL); b(UNFULFILL) }
    }
}

fun withResolvedAnxietyButtons(): Keyboard {
    return keyboard {
        row { b(UNRESOLVE) }
    }
}

fun withDeletedAnxietyButtons(): Keyboard {
    return keyboard {
        row { b(DELETE_MESSAGE) }
    }
}

val UNRESOLVE = KeyboardButton("↩\uFE0F Unresolve", "UNRESOLVE")
val DELETE_MESSAGE = KeyboardButton("\uD83D\uDC80 Delete", "DELETE_MESSAGE")
val FULFILL = KeyboardButton("❌ Fucked", "FULLFILL")
val UNFULFILL = KeyboardButton("✅ Fine", "UNFULFILLED")