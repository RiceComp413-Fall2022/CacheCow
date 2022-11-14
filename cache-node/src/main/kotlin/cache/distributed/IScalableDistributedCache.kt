package cache.distributed

import KeyValuePair
import NodeId

interface IScalableDistributedCache: IDistributedCache {

    fun broadcastLaunchIntentions()

    fun initiateLaunch(): Boolean

    fun initiateCopy(hostName: String)

    fun markCopyComplete(senderId: NodeId)

    fun bulkLocalStore(kvPairs: MutableList<KeyValuePair>)
}