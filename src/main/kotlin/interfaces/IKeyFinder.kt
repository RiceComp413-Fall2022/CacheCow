package interfaces

/**
 * Interface for determining the node that a key is stored within.
 */
interface IKeyFinder {

    fun findNodeForKey(key: String, version: Int): NodeId
}

public typealias NodeId = Int