package nl.jochembroekhoff.gazebo.standalone.gui_quick

import javafx.application.Platform
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import org.metaborg.util.task.IProgress

class ProgressBarProgressImpl(
    private val progressBar: ProgressBar?,
    private val statusLabel: Label?,
    private val root: ProgressBarProgressImpl? = null
) : IProgress {

    private var workSize = 0
    private var workDone = 0

    override fun work(ticks: Int) {
        (root ?: this).run {
            workDone += ticks
            if (progressBar == null) return

            Platform.runLater {
                progressBar.progress = when (workDone) {
                    0 -> 0.0
                    else -> workDone.toDouble() / workSize
                }
            }
        }
    }

    override fun setDescription(description: String) {
        if (statusLabel == null) return

        Platform.runLater {
            statusLabel.text = description
        }
    }

    override fun setWorkRemaining(ticks: Int) {
        (root ?: this).run {
            workSize += ticks
            work(0)
        }
    }

    override fun subProgress(ticks: Int): IProgress {
        return ProgressBarProgressImpl(null, statusLabel, root ?: this)
    }

}
