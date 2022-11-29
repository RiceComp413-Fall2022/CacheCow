import io.javalin.Javalin
import io.javalin.testtools.JavalinTest
import io.mockk.mockkClass
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.http.HttpStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import sender.ScalableSender
import kotlin.test.BeforeTest

class ScalableReceiverTest {

    private lateinit var distributedCache: ScalableDistributedCache

    private lateinit var app: Javalin

    private lateinit var sender: ScalableSender

    @BeforeTest
    fun beforeAll() {
        val nodeList = mutableListOf("localhost:7070", "localhost:7071")
        distributedCache = ScalableDistributedCache(0, nodeList, isAWS = false, isNewNode = false)
        app = distributedCache.getApp()
    }

    @BeforeEach
    internal fun beforeEach() {
        sender = mockkClass(ScalableSender::class)
        distributedCache.mockSender(sender)
    }

    @Test
    internal fun `GET hello world`() = JavalinTest.test(app) { _, client ->
        assertThat(client.get("/v1/hello-world").code).isEqualTo(HttpStatus.OK_200)
    }

    @Test
    internal fun `Add another node`() = JavalinTest.test(app) { _, client ->
        assertThat(client.post("/v1/blobs/a/1").code).isEqualTo(HttpStatus.BAD_REQUEST_400)
        assertThat(client.post("/v1/blobs/a/1.1", convertToBytes("1")).code).isEqualTo(HttpStatus.BAD_REQUEST_400)
        assertThat(client.post("/v1/blobs/a/0?senderId=1.1", convertToBytes("1")).code).isEqualTo(HttpStatus.BAD_REQUEST_400)
        assertThat(client.post("/v1/blobs/a/0?senderId=5", convertToBytes("1")).code).isEqualTo(HttpStatus.BAD_REQUEST_400)
    }
}