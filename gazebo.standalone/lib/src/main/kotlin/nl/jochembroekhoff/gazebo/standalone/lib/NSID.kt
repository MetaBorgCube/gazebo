package nl.jochembroekhoff.gazebo.standalone.lib

data class NSID(
    val namespace: String,
    val name: List<String>,
) {
    fun toMinecraftString(): String {
        return "$namespace:${name.joinToString("/")}"
    }
}
