package cache.local

import KeyValuePair

interface IScalableLocalCache: IEvictingCache {

    fun initializeCopy(copyRanges: MutableList<Pair<Int, Int>>)

    fun streamCopyKeys(count: Int): MutableList<KeyValuePair>

    fun cleanupCopy()
}