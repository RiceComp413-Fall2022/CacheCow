package interfaces

/**
 * Interface for caching data.
 */
interface ICache {

    fun store(key: String, version: Int, value: String)

    // TODO: Make this return json or agreed upon object.
    fun fetch(key: String, version: Int) : String?

    fun isFull(): Boolean
}