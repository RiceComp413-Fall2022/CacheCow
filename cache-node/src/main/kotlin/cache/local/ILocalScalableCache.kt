package cache.local

import KeyValuePair

interface ILocalScalableCache: ILocalEvictingCache {

    fun initializeCopy(copyRanges: MutableList<Pair<Int, Int>>)

    fun streamCopyKeys(count: Int): MutableList<KeyValuePair>

    fun cleanupCopy()
}