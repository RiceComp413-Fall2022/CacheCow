import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CacheTest {

    private val maxCapacity = 5
    private lateinit var cache: Cache

    @BeforeEach
    internal fun beforeEach(): Unit {
        this.cache = Cache(maxCapacity);
    }

    @Test
    internal fun testHit() {
        this.cache.store("key1", "value1")
        assertEquals("value1", this.cache.fetch("key1"))
    }

    @Test
    internal fun testMiss() {
        this.cache.store("key1", "value1")
        assertNull(this.cache.fetch("key2"))
    }

    @Test
    internal fun testCapacity() {
        for (i in 1..maxCapacity + 1) {
            this.cache.store("key$i", "value$i")
        }
        assertEquals("value$maxCapacity", this.cache.fetch("key$maxCapacity"))
        assertNull(this.cache.fetch("key" + (maxCapacity + 1).toString()))
    }
}