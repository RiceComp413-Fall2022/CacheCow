package cache.distributed.launcher

import NodeId

interface INodeLauncher {
    fun launchNode(nodeId: NodeId)
}