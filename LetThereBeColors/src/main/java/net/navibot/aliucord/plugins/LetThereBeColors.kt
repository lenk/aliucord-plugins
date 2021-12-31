package net.navibot.aliucord.plugins

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.view.View
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.SettingsAPI
import com.aliucord.entities.Plugin
import com.aliucord.patcher.after
import com.aliucord.patcher.before
import com.aliucord.views.TextInput
import com.aliucord.widgets.BottomSheet
import com.discord.utilities.view.text.SimpleDraweeSpanTextView
import com.discord.utilities.view.text.TextWatcher
import com.discord.widgets.chat.MessageContent
import com.discord.widgets.chat.MessageManager
import com.discord.widgets.chat.input.ChatInputViewModel
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage
import com.discord.widgets.chat.list.entries.MessageEntry
import net.navibot.aliucord.plugins.error.ParseException

@AliucordPlugin(requiresRestart = true)
class LetThereBeColors : Plugin() {
    init {
        settingsTab = SettingsTab(
            TextColorSettings::class.java,
            SettingsTab.Type.BOTTOM_SHEET
        ).withArgs(settings)
    }

    override fun start(context: Context) {

        try {
            patcher.after<WidgetChatListAdapterItemMessage>("processMessageText", SimpleDraweeSpanTextView::class.java, MessageEntry::class.java) {
                val textView = it.args[0] as SimpleDraweeSpanTextView
                val entry = it.args[1] as MessageEntry

                if (entry.message.isLoading) {
                    return@after
                }

                try {
                    val decode = net.navibot.aliucord.plugins.Utils.decode(entry.message.content)
                    textView.setTextColor(Color.parseColor("#$decode"))
                } catch (num: ParseException) {
                    // ignored, most likely just doesn't have a color set
                }
            }

            patcher.before<ChatInputViewModel>( "sendMessage",
                Context::class.java,
                MessageManager::class.java,
                MessageContent::class.java,
                List::class.java,
                Boolean::class.javaPrimitiveType!!,
                Function1::class.java) {
                try {
                    val color = settings.getString("globalTextColor", null)

                    if (!color.isNullOrEmpty()) {
                        val content = it.args[2] as MessageContent
                        content.set(
                            net.navibot.aliucord.plugins.Utils.encode(
                                color.replace("#", "")
                            ) + content.textContent
                        )

                        it.args[2]
                    }
                } catch (e: Exception) {
                    logger.error(e)
                }
            }
        } catch (e: Exception) {
            logger.error(e)
        }
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }

    class TextColorSettings(private val settings: SettingsAPI) : BottomSheet() {
        override fun onViewCreated(view: View, bundle: Bundle?) {
            super.onViewCreated(view, bundle)
            val ctx = view.context

            TextInput(ctx, "Global text color for LetThereBeColors plugin users!").run {
                editText.run {
                    maxLines = 1
                    setText(settings.getString("globalTextColor", null))

                    addTextChangedListener(object : TextWatcher() {
                        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                        override fun afterTextChanged(s: Editable) {
                            if (s.toString().length <= 9) {
                                settings.setString("globalTextColor", s.toString())
                                Utils.promptRestart()
                            } else {
                                Utils.showToast("Invalid HEX provided!")
                            }
                        }
                    })
                }

                linearLayout.addView(this)
            }
        }
    }
}