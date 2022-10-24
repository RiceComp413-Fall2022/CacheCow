import cache.local.LocalCache
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CacheTest {

    private val maxCapacity = 5
    private lateinit var cache: LocalCache

    @BeforeEach
    internal fun beforeEach() {
        this.cache = LocalCache(maxCapacity)
    }

    @Test
    internal fun testHit() {
        val key = KeyVersionPair("key1", 0)

        this.cache.store(key,"value1")

        assertEquals("value1", this.cache.fetch(key))
    }

    @Test
    internal fun testMiss() {
        val key = KeyVersionPair("key1", 0)
        val invalidKey = KeyVersionPair("key2", 0)

        this.cache.store(key, "value1")

        assertNull(this.cache.fetch(invalidKey))
    }

    @Test
    internal fun testCapacity() {
        val maxCapacityKey = KeyVersionPair("key$maxCapacity", 0)
        val overMaxCapacityKey = KeyVersionPair("key" + (maxCapacity + 1).toString(), 0)

        var key: KeyVersionPair?
        for (i in 1..maxCapacity + 1) {
            key = KeyVersionPair("key$i", 0)
            this.cache.store(key, "value$i")
        }

        assertEquals("value$maxCapacity", this.cache.fetch(maxCapacityKey))
        assertNull(this.cache.fetch(overMaxCapacityKey))
    }
}