/**
 * Representation for different machine nodes.
 */
typealias NodeId = Int

/**
 * Represents a key-version pair, where the key is a string and the version is an integer.
 */
data class KeyVersionPair(val key: String, val version: Int)

/**
 * Represents an introduction by an auto-scaled node, providing its node id and host name
 */
data class IntroRequest(val senderId: Int, val senderHost: String)

/**
 * Represents a request to bulk copy key-value pairs.
 */
data class BulkCopyRequest(val count: Int, val map: Map<String, ByteArray>)