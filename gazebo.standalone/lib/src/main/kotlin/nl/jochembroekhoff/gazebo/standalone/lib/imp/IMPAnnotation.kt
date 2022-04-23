package nl.jochembroekhoff.gazebo.standalone.lib.imp

import nl.jochembroekhoff.gazebo.standalone.lib.NSID

sealed class IMPAnnotation

data class IMPHandlesAnnotation(val handles: List<NSID>) : IMPAnnotation()
