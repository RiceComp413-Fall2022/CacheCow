package node.launcher

import NodeId

interface INodeLauncher {
    fun launchNode(nodeId: NodeId)
}