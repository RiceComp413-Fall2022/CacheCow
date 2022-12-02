package exception.base

import NodeId

/**
 * Base class representing all exceptions originating from a different node.
 */
abstract class CrossNodeException(status: Int, message: String, private var crossNodeId: NodeId): CacheNodeException(status, message) {

    /**
     * Returns the node id where the exception occurred.
     */
    fun getCrossNodeId(): Int {
        return crossNodeId
    }
}