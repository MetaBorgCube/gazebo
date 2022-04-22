package nl.jochembroekhoff.gazebo.standalone.lib.imp

import org.apache.commons.vfs2.FileContent

/**
 * This is a crude parser for the [IMP spec](https://github.com/Arcensoth/imp-spec).
 * It only implements the parts that are actually emitted internally.
 * So, this is by no means a complete implementation, but just what is necessary to make it work.
 */
object IMPParser {
    fun parseSpecFromMcfunction(mcFunctionFileContent: FileContent): IMPSpec? {
        var started = false
        /** workaround var because can't break out of [forEachLine] */
        var stopped = false
        var name: IMPIdentifier? = null
        val comments = mutableListOf<String>()
        val annotations = mutableListOf<IMPAnnotation>()
        var annotationParser: IMPAnnotationParser? = null
        mcFunctionFileContent.inputStream.reader().forEachLine {
            if (stopped)
                return@forEachLine

            if (!started) {
                if (!it.startsWith("#> "))
                    return@forEachLine
                started = true
                name = parseIdentifier(it.substringAfter("#> "))
                return@forEachLine
            }

            // stop when the first non-comment line is seen
            if (!it.startsWith("#")) {
                stopped = true
                return@forEachLine
            }
            val line = it.substring(1).trim()

            // try to advance the annotation parser if active
            annotationParser?.let { ap ->
                val accepted = ap.feed(line)
                if (!accepted) {
                    ap.getAnnotation()?.let { annotation -> annotations.add(annotation) }
                    annotationParser = null
                } else {
                    return@forEachLine
                }
            }

            // start a new annotation parser
            if (line.startsWith("@")) {
                annotationParser = IMPAnnotationParser()
                // try to start it, but give up if it doesn't even accept this first line
                if (annotationParser?.feed(line) == true) {
                    return@forEachLine
                } else {
                    annotationParser = null
                }
            }

            // in all other cases, just add it as a comment
            comments.add(line)
        }

        // add final annotation if this was the last anno
        annotationParser?.getAnnotation()?.let { anno -> annotations.add(anno) }
        annotationParser = null

        if (!started || name == null)
            return null

        return IMPSpec(name!!, comments, annotations)
    }

    fun parseIdentifier(input: String): IMPIdentifier? {
        val split1 = input.split(':', limit=2)
        if (split1.size != 2)
            return null

        val ns = split1[0]
        val name = split1[1].split('/')

        return IMPIdentifier(ns, name)
    }
}
