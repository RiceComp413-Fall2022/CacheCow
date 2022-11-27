import cache.local.LocalCache
import cache.local.LocalEvictingCache
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalEvictingCacheTest {

    private lateinit var cache: LocalEvictingCache

    @BeforeEach
    internal fun beforeEach() {
        this.cache = LocalEvictingCache()
    }

    @Test
    internal fun testHit() {
        val key = KeyVersionPair("key1", 0)

        this.cache.store(key, convertToBytes("value1"))

        assertEquals("value1", convertFromBytes(this.cache.fetch(key)))
    }

    @Test
    internal fun testMiss() {
        val key = KeyVersionPair("key1", 0)
        val invalidKey = KeyVersionPair("key2", 0)

        this.cache.store(key, convertToBytes("value1"))

        assertNull(this.cache.fetch(invalidKey))
    }

//    @Test
//    internal fun testCapacity() {
//        var failed = false
//        val maxCapacity = cache.fetchJVMUsage().maxMemory
//        var key: KeyVersionPair?
//        for (i in 1..10) {
//            key = KeyVersionPair("x", 0)
//            val value = "y".repeat((maxCapacity / 10).toInt())
//            failed = failed || this.cache.store(key, convertToBytes(value))
//            TimeUnit.SECONDS.sleep(1)
//
//        }
//        assertFalse(failed)
//    }

    @Test
    internal fun testExceedCapacity() {
        var failed = false
        val maxCapacity = cache.fetchJVMUsage().maxMemory
        var key: KeyVersionPair?
        for (i in 1..1000) {
            key = KeyVersionPair("x", 0)
            val value = "y".repeat((maxCapacity / 1000).toInt())
            failed = failed || this.cache.store(key, convertToBytes(value))
        }
        assertTrue(failed)
    }
}