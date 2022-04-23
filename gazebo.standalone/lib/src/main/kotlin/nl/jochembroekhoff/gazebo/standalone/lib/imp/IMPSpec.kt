package nl.jochembroekhoff.gazebo.standalone.lib.imp

import nl.jochembroekhoff.gazebo.standalone.lib.NSID

data class IMPSpec(
    val name: NSID,
    val comments: List<String>,
    val annotations: List<IMPAnnotation>,
)
