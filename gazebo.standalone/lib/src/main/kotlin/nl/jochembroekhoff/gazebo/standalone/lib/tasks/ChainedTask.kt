package nl.jochembroekhoff.gazebo.standalone.lib.tasks

import org.metaborg.core.project.IProject
import org.metaborg.spoofax.core.Spoofax
import org.metaborg.spoofax.core.build.ISpoofaxBuildOutput
import org.metaborg.spoofax.core.shell.CLIUtils

internal class ChainedTask<T, U>(
    private val first: OverlayTask<T>,
    private val nextFactory: (T) -> OverlayTask<U>
) : OverlayTask<U>("${first.key}.chain") {

    private var second: OverlayTask<U>? = null

    override fun run(spoofax: Spoofax, cli: CLIUtils, project: IProject, output: ISpoofaxBuildOutput) {
        first.run(spoofax, cli, project, output)

        val second = nextFactory(first.result())
        this.second = second
        second.run(spoofax, cli, project, output)
    }

    override fun result(): U {
        return second!!.result()
    }
}
