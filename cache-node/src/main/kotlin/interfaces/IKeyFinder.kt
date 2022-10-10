package interfaces

import NodeId

/**
 * Interface for determining the node that a key is stored within.
 */
interface IKeyFinder {

    fun findNodeForKey(key: String, version: Int): NodeId
}