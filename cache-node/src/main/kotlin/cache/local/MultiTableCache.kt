package cache.local

import KeyVersionPair

class MultiTableCache() : ILocalCache {
    override fun fetch(kvPair: KeyVersionPair): ByteArray {
        TODO("Not yet implemented")
    }

    override fun store(kvPair: KeyVersionPair, value: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun remove(kvPair: KeyVersionPair): ByteArray? {
        TODO("Not yet implemented")
    }

    override fun clear() {
        TODO("Not yet implemented")
    }

    override fun isFull(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getCacheInfo(): CacheInfo {
        TODO("Not yet implemented")
    }

}