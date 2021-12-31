package net.navibot.aliucord.plugins

import com.discord.widgets.chat.MessageContent
import net.navibot.aliucord.plugins.error.ParseException


fun MessageContent.set(text: String) {
    val field = MessageContent::class.java.getDeclaredField("textContent").apply {
        isAccessible = true
    }

    field.set(this, text)
}

class Utils {
    companion object {
        private val map = (('A'..'F').mapIndexed { i, c -> Pair(c, "\u200B".repeat(i + 1)) }.toMap() +
                ('0'..'9').mapIndexed { i, c -> Pair(c, "\u200B".repeat((i + 7) * 2)) }.toMap())

        fun encode(hex: String): String {
            val builder = StringBuilder("\u200D")

            hex.forEach { c ->
                builder.append(map[c.uppercaseChar()] ?: throw ParseException("Invalid HEX Provided!")).append("\u200C")
            }

            return builder.append("\u200D").toString()
        }

        fun decode(data: String): String {
            if (!data.matches(Regex("^\u200D(.*)\u200D(.*)$"))) {
                throw ParseException("No valid encoded HEX found!")
            }

            val chunk = data.substring(data.indexOf("\u200D") + 1, data.lastIndexOf("\u200D"))
            if (!chunk.matches(Regex("^[\u200C\u200B]+$"))) {
                throw ParseException("No valid encoded HEX found!")
            }

            val result = chunk.split("\u200C").filter { e1 -> e1.isNotEmpty() }.map { e1 ->
                map.entries.first { e2 -> e2.value.length == e1.length }.key
            }.toCharArray()

            return String(result)

        }
    }
}