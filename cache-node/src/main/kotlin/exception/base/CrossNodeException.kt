package exception.base

import NodeId

abstract class CrossNodeException(status: Int, message: String, private var crossNodeId: NodeId): CacheNodeException(status, message) {
    fun getCrossNodeId(): Int {
        return crossNodeId
    }
}