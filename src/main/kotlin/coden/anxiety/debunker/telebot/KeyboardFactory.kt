package coden.anxiety.debunker.telebot

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton


data class Keyboard(
    val lines: List<KeyboardLine>,
)

data class KeyboardLine(
    val buttons: List<KeyboardButton>,
)

data class KeyboardButton(
    val text: String,
    val data: String=text
)

class KeyboardLineBuilder{
    private val line = ArrayList<KeyboardButton>()

    fun b(text: String, data: String=text): KeyboardLineBuilder {
        return b(KeyboardButton(text, data))
    }

    fun b(button: KeyboardButton): KeyboardLineBuilder {
        line.add(button)
        return this
    }

    internal fun build(): KeyboardLine{
        return KeyboardLine(line)
    }
}

class KeyboardBuilder{
    private val lines = ArrayList<KeyboardLine>()

    fun row(line: KeyboardLineBuilder.() -> Unit): KeyboardBuilder{
        val new = KeyboardLineBuilder()
        line.invoke(new)
        lines.add(new.build())
        return this
    }

    internal fun build(): Keyboard{
        return Keyboard(lines)
    }
}

fun keyboard(keyboard: KeyboardBuilder.() -> Unit): ReplyKeyboard {
    val new = KeyboardBuilder()
    new.keyboard()
    return new.build().asReplyKeyboard()
}


fun Keyboard.asReplyKeyboard(): ReplyKeyboard {
    val markup = InlineKeyboardMarkup()
    val keyboard = ArrayList<List<InlineKeyboardButton>>()

    for (line in lines) {
        val inlineButtons = ArrayList<InlineKeyboardButton>()
        for (button in line.buttons){
            val b = InlineKeyboardButton()
            b.text = button.text
            b.callbackData = button.data
            inlineButtons.add(b)
        }
        keyboard.add(inlineButtons)
    }

    markup.keyboard = keyboard
    return markup
}

