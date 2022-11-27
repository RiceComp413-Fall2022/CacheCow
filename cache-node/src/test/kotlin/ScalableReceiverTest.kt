import io.javalin.Javalin
import io.javalin.testtools.JavalinTest
import io.mockk.mockkClass
import node.Node
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.http.HttpStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import sender.Sender
import kotlin.test.BeforeTest

class ScalableReceiverTest {

    private lateinit var node: Node

    private lateinit var app: Javalin

    private lateinit var sender: Sender

    @BeforeTest
    fun beforeAll() {
        val nodeList = mutableListOf("localhost:7070", "localhost:7071")
        node = Node(0, nodeList, 7070, isAWS = false, scalable = true, newNode = false)
        app = node.receiver.app
    }

    @BeforeEach
    internal fun beforeEach() {
        sender = mockkClass(Sender::class)
        node.distributedCache.mockSender(sender)
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