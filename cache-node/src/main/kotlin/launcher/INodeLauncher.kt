package launcher

import NodeId

/**
 * Interface specifying module that can launh an additional node.
 */
interface INodeLauncher {
    
    /**
     * Launches a new node with the given node id.
     *
     * @param nodeId of node to be laumnched
     */
    fun launchNode(nodeId: NodeId)
}
