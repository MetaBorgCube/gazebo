package nl.jochembroekhoff.gazebo.standalone.cli

import picocli.CommandLine.IVersionProvider

class ImplementationVersionProvider : IVersionProvider {
    override fun getVersion(): Array<String> {
        return arrayOf(
            javaClass.`package`.implementationTitle ?: "",
            javaClass.`package`.implementationVersion ?: "dev"
        )
    }
}
