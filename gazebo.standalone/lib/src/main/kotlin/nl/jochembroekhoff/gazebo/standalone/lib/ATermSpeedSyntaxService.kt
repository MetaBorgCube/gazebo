package nl.jochembroekhoff.gazebo.standalone.lib

import com.google.inject.Inject
import org.metaborg.spoofax.core.syntax.ISpoofaxSyntaxService
import org.metaborg.spoofax.core.syntax.SpoofaxSyntaxService
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit
import org.metaborg.spoofax.core.unit.ISpoofaxUnitService
import org.metaborg.spoofax.core.unit.ParseContrib
import org.metaborg.util.log.LoggerUtils
import org.metaborg.util.task.ICancel
import org.metaborg.util.task.IProgress
import org.spoofax.interpreter.terms.ITermFactory

class ATermSpeedSyntaxService @Inject constructor(
    private val spoofaxSyntaxService: SpoofaxSyntaxService,
    private val termFactory: ITermFactory,
    private val unitService: ISpoofaxUnitService
) : ISpoofaxSyntaxService by spoofaxSyntaxService {

    companion object {
        private val logger = LoggerUtils.logger(ATermSpeedSyntaxService::class.java)
    }

    override fun parse(input: ISpoofaxInputUnit, progress: IProgress, cancel: ICancel): ISpoofaxParseUnit {
        val inputName = input.source()?.name
        return if (inputName?.baseName?.let { it.endsWith(".aterm-speed.gzbc") || it.endsWith(".aterm-speed.llmc") } == true) {
            cancel.throwIfCancelled()

            logger.debug("ATerm speed syntax load for input {}", inputName)
            progress.setDescription("ATerm speed syntax load")
            progress.setWorkRemaining(1)

            val textTerm = termFactory.parseFromString(input.text())

            progress.work(1)
            cancel.throwIfCancelled()

            unitService.parseUnit(input, ParseContrib(textTerm))
        } else {
            spoofaxSyntaxService.parse(input, progress, cancel)
        }
    }

}
