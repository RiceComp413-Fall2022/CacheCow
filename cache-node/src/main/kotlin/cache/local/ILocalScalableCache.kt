package cache.local

import KeyVersionPair

interface ILocalScalableCache: ILocalEvictingCache {

    fun initializeCopy(start: Int, end: Int)

    fun streamCopyKeys(count: Int): MutableList<Pair<KeyVersionPair, ByteArray>>
}