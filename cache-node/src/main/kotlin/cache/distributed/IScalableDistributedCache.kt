package cache.distributed

import KeyValuePair
import NodeId
import launcher.INodeLauncher

/**
 * An interface specifying the behavior of a distributed data cache that supports scaling
 * additional nodes.
 */
interface IScalableDistributedCache: IDistributedCache {

    /**
     * Returns boolean indicating whether cluster is currently scaling.
     */
    fun scaleInProgress(): Boolean

    /**
     * Handles request from given sender id indicating intention to launch another node,
     * return boolean indicating whether it gives permission.
     *
     * @param senderId node indicating intention to launch
     * @return boolean indicating whether this node gives permission
     */
    fun handleLaunchRequest(senderId: NodeId): Boolean

    /**
     * Handles request indicating that the scaling process has completed by resetting all
     * scale-related state in order to prepare for future scale.
     */
    fun handleScaleCompleteRequest()

    /**
     * Initiates launch of a new node, broadcasts launch intentions to all other nodes in
     * the cluster.
     */
    fun initiateLaunch()

    /**
     * The new node is up and running, updates key distribution protocol and copy all keys
     * whose primary node changed to new node.
     *
     * @param newHostName host name of new node that was launched
     */
    fun initiateCopy(newHostName: String)

    /**
     * If this is the final node to complete copying its data, then broadcasts scale
     * complete message to all other nodes.
     *
     * @param senderId node that has finished copying redistributed data to this node
     * @return boolean indicating whether this is the final node to complete
     */
    fun markCopyComplete(senderId: NodeId): Boolean

    /**
     * Stores the given key-value pairs to the local cache in bulk.
     *
     * @param kvPairs key-value pairs copied from another node
     */
    fun bulkLocalStore(kvPairs: MutableList<KeyValuePair>)

    /**
     * Mocks the node launcher field (used for testing).
     *
     * @param mockLauncher node launcher that is mocked
     */
    fun mockNodeLauncher(mockLauncher: INodeLauncher)
}