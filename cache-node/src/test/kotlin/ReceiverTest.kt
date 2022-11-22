import exception.CrossServerException
import exception.KeyNotFoundException
import io.javalin.Javalin
import io.javalin.testtools.JavalinTest
import io.mockk.every
import io.mockk.mockkClass
import node.Node
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.http.HttpStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import sender.Sender
import kotlin.test.BeforeTest

class ReceiverTest {

    private lateinit var node: Node

    private lateinit var app: Javalin

    private lateinit var sender: Sender

    @BeforeTest
    fun beforeAll() {
        val nodeList = mutableListOf("localhost:7070", "localhost:7071")
        node = Node(0, nodeList, 7070, isAWS = false, scalable = false, newNode = false)
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
    internal fun `Bad request errors on blob store`() = JavalinTest.test(app) { _, client ->
        assertThat(client.post("/v1/blobs/a/1").code).isEqualTo(HttpStatus.BAD_REQUEST_400)
        assertThat(client.post("/v1/blobs/a/1.1", convertToBytes("1")).code).isEqualTo(HttpStatus.BAD_REQUEST_400)
        assertThat(client.post("/v1/blobs/a/0?senderId=1.1", convertToBytes("1")).code).isEqualTo(HttpStatus.BAD_REQUEST_400)
        assertThat(client.post("/v1/blobs/a/0?senderId=5", convertToBytes("1")).code).isEqualTo(HttpStatus.BAD_REQUEST_400)
    }

    @Test
    internal fun `Bad request errors on blob fetch`() = JavalinTest.test(app) { _, client ->
        assertThat(client.get("/v1/blobs/a/1.1").code).isEqualTo(HttpStatus.BAD_REQUEST_400)
        assertThat(client.get("/v1/blobs/a/0?senderId=1.1").code).isEqualTo(HttpStatus.BAD_REQUEST_400)
        assertThat(client.get("/v1/blobs/a/0?senderId=5").code).isEqualTo(HttpStatus.BAD_REQUEST_400)
    }

    @Test
    internal fun `Not found error if value not stored`() = JavalinTest.test(app) { _, client ->
        assertThat(client.get("/v1/blobs/a/1").code).isEqualTo(HttpStatus.NOT_FOUND_404)
    }

    @Test
    internal fun `Store and fetch key-value pair locally`() = JavalinTest.test(app) { _, client ->
        val storeResponse = client.post("/v1/blobs/a/1", "123")
        assertThat(storeResponse.code).isEqualTo(HttpStatus.CREATED_201)

        val fetchResponse = client.get("/v1/blobs/a/1")
        assertThat(fetchResponse.code).isEqualTo(HttpStatus.OK_200)
        assertThat(fetchResponse.body).isNotNull
        assertThat(fetchResponse.body!!.string()).isEqualTo("123")
    }

    @Test
    internal fun `Store and fetch local key-value pair originating from different node`() = JavalinTest.test(app) { _, client ->
        val storeResponse = client.post("/v1/blobs/a/1?requestId=1", "123")
        assertThat(storeResponse.code).isEqualTo(HttpStatus.CREATED_201)

        val fetchResponse = client.get("/v1/blobs/a/1?requestId=1")
        assertThat(fetchResponse.code).isEqualTo(HttpStatus.OK_200)
        assertThat(fetchResponse.body).isNotNull
        assertThat(fetchResponse.body!!.string()).isEqualTo("123")
    }

    @Test
    internal fun `Store and fetch key-value pair to node that is not running`() = JavalinTest.test(app) { _, client ->
        val storeResponse = client.post("/v1/blobs/b/1", "123")
        assertThat(storeResponse.code).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR_500)

        val fetchResponse = client.get("/v1/blobs/b/1")
        assertThat(fetchResponse.code).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR_500)
    }

    @Test
    internal fun `Store and fetch key-value pair to node that returns error`() = JavalinTest.test(app) { _, client ->
        val mockSender = this.sender
        every { mockSender.storeToNode(any(), any(), 1) } throws CrossServerException(1)
        every { mockSender.fetchFromNode(any(), 1) } throws CrossServerException(1)

        val storeResponse = client.post("/v1/blobs/b/1", "123")
        assertThat(storeResponse.code).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR_500)

        val fetchResponse = client.get("/v1/blobs/b/1")
        assertThat(fetchResponse.code).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR_500)
    }

    @Test
    internal fun `Not found error if value not stored in destination node`() = JavalinTest.test(app) { _, client ->
        val mockSender = this.sender
        every { mockSender.fetchFromNode(any(), 1) } throws KeyNotFoundException("b")

        val fetchResponse = client.get("/v1/blobs/b/1")
        assertThat(fetchResponse.code).isEqualTo(HttpStatus.NOT_FOUND_404)
    }

    @Test
    internal fun `Store and fetch key-value pair to different node`() = JavalinTest.test(app) { _, client ->
        val mockSender = this.sender

        every { mockSender.storeToNode(any(), any(), 1) } returns Unit
        val storeResponse = client.post("/v1/blobs/b/1", "123")
        assertThat(storeResponse.code).isEqualTo(HttpStatus.CREATED_201)

        every { mockSender.fetchFromNode(any(), 1) } returns convertToBytes("123")
        val fetchResponse = client.get("/v1/blobs/b/1")
        assertThat(fetchResponse.code).isEqualTo(HttpStatus.OK_200)
        assertThat(fetchResponse.body).isNotNull
        assertThat(fetchResponse.body!!.string()).isEqualTo("123")
    }
}