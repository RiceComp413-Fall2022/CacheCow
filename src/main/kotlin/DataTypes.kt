/*
 * Defines project-wide data types.
 */

/**
 * Representation for different machine nodes.
 */
typealias NodeId = Int

/**
 * Represents key-version pair, where the key is a string and the version is an integer.
 * This data class is our base representation of a "key" for the data
 */
data class KeyVersionPair(val key: String, val version: Int)

/**
 * Represents key-value pair, where the key is a concatenation of the key and version.
 * Class is used to print a representation of data received from the user.
 */
data class KeyValueReply(val key: String?, val value: String?)