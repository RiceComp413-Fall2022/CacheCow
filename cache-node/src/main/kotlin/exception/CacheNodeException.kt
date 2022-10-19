package exception

abstract class CacheNodeException(val status: Int, override val message: String): Exception(message) {

    /**
     * Returns the exception id unique to the exception type.
     */
    abstract fun getExceptionID(): Int
}