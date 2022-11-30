package cache.distributed

import KeyValuePair
import NodeId
import cache.distributed.launcher.INodeLauncher

interface IScalableDistributedCache: IDistributedCache {

    fun scaleInProgress(): Boolean

    fun handleLaunchRequest(senderId: NodeId): Boolean

    fun handleScaleCompleteRequest()

    fun initiateLaunch()

    fun initiateCopy(newHostName: String)

    fun markCopyComplete(senderId: NodeId): Boolean

    fun bulkLocalStore(kvPairs: MutableList<KeyValuePair>)

    fun mockNodeLauncher(mockLauncher: INodeLauncher)
}