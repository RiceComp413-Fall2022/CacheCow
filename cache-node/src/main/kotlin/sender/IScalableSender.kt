package sender

import KeyVersionPair
import NodeId

interface IScalableSender: ISender {
    /**
     * Copy a list of key value pairs to another node.
     * @param kvPairs list of key value pairs
     * @param destNodeId node id of destination node
     * @return boolean indicating success
     */
    fun copyKvPairs(kvPairs: MutableList<Pair<KeyVersionPair, ByteArray>>, destNodeId: NodeId): Boolean
}