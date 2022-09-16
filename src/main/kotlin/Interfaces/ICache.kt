package Interfaces

/**
 * Interface for caching data.
 */
interface ICache {

    fun store(key: String)

    fun fetch(key: String)
}