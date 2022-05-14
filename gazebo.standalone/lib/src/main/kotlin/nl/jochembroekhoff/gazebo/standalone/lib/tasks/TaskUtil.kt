package nl.jochembroekhoff.gazebo.standalone.lib.tasks

object TaskUtil {
    fun <T, R> OverlayTask<T>.chain(nextFactory: (T) -> OverlayTask<R>): OverlayTask<R> {
        return ChainedTask(this, nextFactory)
    }
}
