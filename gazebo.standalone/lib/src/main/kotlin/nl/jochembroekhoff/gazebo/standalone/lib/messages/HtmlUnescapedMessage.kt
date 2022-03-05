package nl.jochembroekhoff.gazebo.standalone.lib.messages

import org.apache.commons.lang3.StringUtils
import org.metaborg.core.messages.IMessage

class HtmlUnescapedMessage(private val message: IMessage) : IMessage by message {

    companion object {

        private val replaceSearch = arrayOf("&gt;", "&lt;", "&nbsp;", "<br>\n", "<br>")
        private val replaceReplacement = arrayOf(">", "<", " ", "\n", "\n")

        fun unescape(message: String): String {
            return StringUtils.replaceEach(message, replaceSearch, replaceReplacement)
        }
    }

    override fun message(): String {
        return unescape(message.message())
    }
}
