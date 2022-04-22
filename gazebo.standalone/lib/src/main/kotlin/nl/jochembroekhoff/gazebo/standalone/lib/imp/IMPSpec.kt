package nl.jochembroekhoff.gazebo.standalone.lib.imp

data class IMPSpec(
    val name: IMPIdentifier,
    val comments: List<String>,
    val annotations: List<IMPAnnotation>,
)
