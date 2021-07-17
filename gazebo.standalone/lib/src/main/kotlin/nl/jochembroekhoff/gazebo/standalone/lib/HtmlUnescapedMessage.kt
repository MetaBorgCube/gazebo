package nl.jochembroekhoff.gazebo.standalone.lib

import org.metaborg.core.messages.IMessage

class HtmlUnescapedMessage(private val message: IMessage) : IMessage by message {

    companion object {
        fun unescape(message: String): String {
            // TODO: efficient replace
            return message
                .replace("&gt;", ">")
                .replace("&lt;", "<")
                .replace("&nbsp;", " ")
                .replace("<br>\n", "\n")
                .replace("<br>", "\n")
        }
    }

    override fun message(): String {
        return unescape(message.message())
    }
}
