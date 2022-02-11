package nl.jochembroekhoff.gazebo.standalone.lib.tasks

import org.apache.commons.vfs2.FileObject
import org.metaborg.core.build.CommonPaths
import org.metaborg.core.project.IProject
import org.metaborg.spoofax.core.Spoofax
import org.metaborg.spoofax.core.build.ISpoofaxBuildOutput
import org.metaborg.spoofax.core.shell.CLIUtils

abstract class AdditionalTask<T>(val key: String) {
    abstract fun run(spoofax: Spoofax, cli: CLIUtils, project: IProject, output: ISpoofaxBuildOutput)
    abstract fun result(): T

    protected fun taskDefaultOutput(project: IProject): FileObject {
        return CommonPaths(project.location()).targetDir().resolveFile("gazebo-standalone/task/$key")
    }
}
