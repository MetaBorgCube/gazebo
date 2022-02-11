package nl.jochembroekhoff.gazebo.standalone.lib.tasks

object TaskUtil {
    fun <T, R> AdditionalTask<T>.chain(nextFactory: (T) -> AdditionalTask<R>): AdditionalTask<R> {
        return ChainedTask(this, nextFactory)
    }
}
