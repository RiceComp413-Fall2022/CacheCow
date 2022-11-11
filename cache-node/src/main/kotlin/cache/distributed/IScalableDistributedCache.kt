package cache.distributed

import KeyValuePair
import NodeId

interface IScalableDistributedCache: IDistributedCache {

    fun broadcastLaunchIntentions()

    fun initiateCopy(hostName: String)

    fun markCopyComplete(senderId: NodeId)

    fun bulkLocalStore(kvPairs: MutableList<KeyValuePair>)
}