package nl.jochembroekhoff.gazebo.standalone.lib.imp

class IMPAnnotationParser {

    private var started = false
    private val entries = mutableListOf<IMPIdentifier>()

    fun feed(line: String): Boolean {
        if (line.startsWith("@")) {
            if (started) {
                return false
            }

            val startSplit = line.split(' ', limit=2)
            if (startSplit[0] != "@handles")
                return false
            started = true

            startSplit.getOrNull(1)?.let { shorthandLine -> return feed(shorthandLine) }
            return true
        } else if (line.startsWith('#')) {
            IMPParser.parseIdentifier(line.substringAfter('#'))?.let { ident ->
                entries.add(ident)
                return true
            }
            return false
        }
        return false
    }

    fun getAnnotation(): IMPAnnotation? {
        if (!started)
            return null

        if (entries.isEmpty())
            return null

        return IMPHandlesAnnotation(entries)
    }
}
