package nl.jochembroekhoff.gazebo.standalone.lib

data class GazeboProjectConfigServiceConfig(
    val languages: Set<GazeboLang>? = null,
    val extensions: Set<GazeboExt>? = null,
    val libs: Set<String>? = null,
)
