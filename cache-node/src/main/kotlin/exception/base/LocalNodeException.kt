package exception.base

/**
 * Base class representing all exceptions originating from this node.
 */
abstract class LocalNodeException(status: Int, message: String): CacheNodeException(status, message) {
}