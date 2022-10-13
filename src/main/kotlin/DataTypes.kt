/**
 * Representation for different machine nodes.
 */
typealias NodeId = Int

/**
 * Represents a key-version pair, where the key is a string and the version is an integer.
 */
data class KeyVersionPair(val key: String, val version: Int)