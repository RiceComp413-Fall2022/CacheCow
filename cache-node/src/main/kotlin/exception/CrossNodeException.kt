package exception

import NodeId

abstract class CrossNodeException(status: Int, message: String, private var crossNodeId: NodeId): CacheNodeException(status, message) {
}