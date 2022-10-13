package cache.local

import KeyVersionPair

/**
 * Interface for caching data.
 */
interface ILocalCache {

    fun store(kvPair: KeyVersionPair, value: String): Boolean

    fun fetch(kvPair: KeyVersionPair) : String?

    fun isFull(): Boolean

}