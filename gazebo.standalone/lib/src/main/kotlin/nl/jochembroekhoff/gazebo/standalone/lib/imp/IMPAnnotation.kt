package nl.jochembroekhoff.gazebo.standalone.lib.imp

sealed class IMPAnnotation

data class IMPHandlesAnnotation(val handles: List<IMPIdentifier>) : IMPAnnotation()
