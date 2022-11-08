package cache.local

import KeyValuePair

interface ILocalScalableCache: ILocalEvictingCache {

    fun initializeCopy(start: Int, end: Int)

    fun streamCopyKeys(count: Int): MutableList<KeyValuePair>
}