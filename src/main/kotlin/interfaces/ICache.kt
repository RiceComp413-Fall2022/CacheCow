package interfaces

/**
 * Interface for caching data.
 */
interface ICache {

    fun store(key: String, value: String)

    // TODO: Make this return json or agreed upon object.
    fun fetch(key: String) : String?
}