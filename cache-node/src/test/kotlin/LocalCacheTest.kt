import cache.local.LocalCache
import exception.CacheFullException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.charset.Charset
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LocalCacheTest {

    private val maxCapacity = 5
    private lateinit var cache: LocalCache

    @BeforeEach
    internal fun beforeEach() {
        cache = LocalCache(maxCapacity)
    }

    @Test
    internal fun testHit() {
        val key = KeyVersionPair("key1", 0)

        cache.store(key, convertToBytes("value1"))

        assertEquals("value1", convertFromBytes(cache.fetch(key)))
    }

    @Test
    internal fun testMiss() {
        val key = KeyVersionPair("key1", 0)
        val invalidKey = KeyVersionPair("key2", 0)

        cache.store(key, convertToBytes("value1"))

        assertNull(cache.fetch(invalidKey))
    }

    @Test
    internal fun testCapacity() {
        val maxCapacityKey = KeyVersionPair("key$maxCapacity", 0)
        val overMaxCapacityKey = KeyVersionPair("key" + (maxCapacity + 1).toString(), 0)

        var key: KeyVersionPair?
        for (i in 1..maxCapacity) {
            key = KeyVersionPair("key$i", 0)
            cache.store(key, convertToBytes("value$i"))
        }

        assertThrows(CacheFullException::class.java) {
            cache.store(overMaxCapacityKey, convertToBytes("new value"))
        }

        assertEquals("value$maxCapacity", convertFromBytes(cache.fetch(maxCapacityKey)))
        assertNull(cache.fetch(overMaxCapacityKey))
    }
}

fun convertToBytes(value: String): ByteArray {
    return value.toByteArray(Charset.defaultCharset())
}

fun convertFromBytes(value: ByteArray?): String {
    return if (value == null) "" else String(value, Charset.defaultCharset())
}