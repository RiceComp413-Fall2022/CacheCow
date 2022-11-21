package exception

abstract class LocalNodeException(status: Int, message: String): CacheNodeException(status, message) {
}