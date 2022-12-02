/**
 * Representation for different machine nodes.
 */
typealias NodeId = Int

/**
 * Represents a key-version pair, where the key is a string and the version is an integer.
 */
data class KeyVersionPair(val key: String, val version: Int)

/**
 * Represents a key-version-value group, where the key is a string, the version is an integer,
 * and the value is a byte array.
 */
data class KeyValuePair(val key: String, val version: Int, val value: ByteArray)


/**
 * Distinguishes types of messages used during scaling process.
 */
enum class ScalableMessageType {
    LAUNCH_NODE, READY, COPY_COMPLETE, SCALE_COMPLETE
}

/**
 * Data packet encapsulating a scalable message and information about the sender node.
 */
data class ScalableMessage(val nodeId: NodeId, val hostName: String, val type: ScalableMessageType)

/**
 * Data packet containing map of key-value pairs to be stored locally.
 */
data class BulkCopyRequest(val nodeId: NodeId, val values: MutableList<KeyValuePair>)