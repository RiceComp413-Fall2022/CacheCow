package cache.local

import KeyValuePair

/**
 * An interface specifying the behavior of a local data cache that can copy data on scale.
 */
interface IScalableLocalCache: IEvictingCache {

    /**
     * Initializes the set of keys that must be copied as all locally stored keys having
     * hash values in one of the given integer ranges.
     *
     * @param copyRanges integer hash value ranges specifying which keys must be copied
     */
    fun initializeCopy(copyRanges: MutableList<Pair<Int, Int>>)

    /**
     * Streams the next count key-value pairs to be copied from the local cache. The
     * copying process must first have been initialized.
     *
     * @param count number of keys to send in next copy request
     * @return list of key-value pairs to be copied with size count or less.
     */
    fun streamCopyKeys(count: Int): MutableList<KeyValuePair>

    /**
     * Cleans up the copying process.
     */
    fun cleanupCopyKeys()
}