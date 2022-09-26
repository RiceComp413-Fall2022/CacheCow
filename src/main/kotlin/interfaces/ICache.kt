package interfaces

import KeyVersionPair

/**
 * Interface for caching data.
 */
interface ICache {

    fun store(key: KeyVersionPair, value: String)

    // TODO: Make this return json or agreed upon object.
    fun fetch(key: KeyVersionPair) : String?

    fun isFull(): Boolean
}