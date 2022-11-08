package cache.distributed

import KeyVersionPair

interface IScalableDistributedCache: IDistributedCache {
    fun testCopy()

    fun bulkLocalStore(kvPairs: MutableList<Pair<KeyVersionPair, ByteArray>>)
}