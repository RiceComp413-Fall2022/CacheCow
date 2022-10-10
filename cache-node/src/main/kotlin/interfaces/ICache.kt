package interfaces

import KeyVersionPair

/**
 * Interface for caching data.
 */
interface ICache {

    fun store(kvPair: KeyVersionPair, value: String)

    // TODO: Make this return json or agreed upon object.
    fun fetch(kvPair: KeyVersionPair) : String?

    fun isFull(): Boolean
}