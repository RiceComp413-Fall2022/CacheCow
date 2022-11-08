package cache.distributed

import KeyValuePair
import NodeId

interface IScalableDistributedCache: IDistributedCache {

    fun broadcastLaunchIntentions()

    fun initiateCopy(hostName: String)

    fun markCopyComplete(nodeId: NodeId)

    fun bulkLocalStore(kvPairs: MutableList<KeyValuePair>)
}