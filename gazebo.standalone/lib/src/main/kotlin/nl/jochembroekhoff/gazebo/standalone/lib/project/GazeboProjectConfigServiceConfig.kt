package nl.jochembroekhoff.gazebo.standalone.lib.project

import nl.jochembroekhoff.gazebo.standalone.lib.constants.GazeboExt
import nl.jochembroekhoff.gazebo.standalone.lib.constants.GazeboLang

data class GazeboProjectConfigServiceConfig(
    val languages: Set<GazeboLang>? = null,
    val extensions: Set<GazeboExt>? = null,
    val libs: Set<String>? = null,
)
