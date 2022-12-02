import node.launcher.LocalNodeLauncher
import exception.CrossServerException
import exception.KeyNotFoundException
import io.javalin.Javalin
import io.javalin.testtools.JavalinTest
import io.mockk.every
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
        val nodeList = mutableListOf("localhost:7070", "localhost:7071", "localhost:7072")
        distributedCache = ScalableDistributedCache(0, nodeList, isAWS = false, isNewNode = false)
        app = distributedCache.getJavalinApp()
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
    internal fun `Bad request errors on inform endpoint`() = JavalinTest.test(app) { _, client ->
        assertThat(client.post("/v1/inform", ScalableMessage(1, "", ScalableMessageType.SCALE_COMPLETE)).code).isEqualTo(HttpStatus.BAD_REQUEST_400)
        assertThat(client.post("/v1/inform", ScalableMessage(4, "", ScalableMessageType.LAUNCH_NODE)).code).isEqualTo(HttpStatus.BAD_REQUEST_400)
    }

    @Test
    internal fun `Bad request errors for bulk copy`() = JavalinTest.test(app) { _, client ->
        val bulkCopyResponse = client.post("/v1/bulk-copy", BulkCopyRequest(2, mutableListOf(KeyValuePair("c", 1, convertToBytes("123")))))
        assertThat(bulkCopyResponse.code).isEqualTo(HttpStatus.BAD_REQUEST_400)
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
        val storeResponse = client.post("/v1/blobs/c/1", "123")
        assertThat(storeResponse.code).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR_500)

        val fetchResponse = client.get("/v1/blobs/c/1")
        assertThat(fetchResponse.code).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR_500)
    }

    @Test
    internal fun `Store and fetch key-value pair to node that returns error`() = JavalinTest.test(app) { _, client ->
        every { sender.storeToNode(any(), any(), 2) } throws CrossServerException(1)
        every { sender.fetchFromNode(any(), 2) } throws CrossServerException(1)

        val storeResponse = client.post("/v1/blobs/c/1", "123")
        assertThat(storeResponse.code).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR_500)

        val fetchResponse = client.get("/v1/blobs/c/1")
        assertThat(fetchResponse.code).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR_500)
    }

    @Test
    internal fun `Not found error if value not stored in destination node`() = JavalinTest.test(app) { _, client ->
        every { sender.fetchFromNode(any(), 2) } throws KeyNotFoundException("c")

        val fetchResponse = client.get("/v1/blobs/c/1")
        assertThat(fetchResponse.code).isEqualTo(HttpStatus.NOT_FOUND_404)
    }

    @Test
    internal fun `Store and fetch key-value pair to different node`() = JavalinTest.test(app) { _, client ->
        every { sender.storeToNode(any(), any(), 2) } returns Unit
        val storeResponse = client.post("/v1/blobs/c/1", "123")
        assertThat(storeResponse.code).isEqualTo(HttpStatus.CREATED_201)

        every { sender.fetchFromNode(any(), 2) } returns convertToBytes("123")
        val fetchResponse = client.get("/v1/blobs/c/1")
        assertThat(fetchResponse.code).isEqualTo(HttpStatus.OK_200)
        assertThat(fetchResponse.body).isNotNull
        assertThat(fetchResponse.body!!.string()).isEqualTo("123")
    }

    @Test
    internal fun `Different node starts launching process`() = JavalinTest.test(app) { _, client ->
        val launchResponse = client.post("/v1/inform", ScalableMessage(1, "", ScalableMessageType.LAUNCH_NODE))
        assertThat(launchResponse.code).isEqualTo(HttpStatus.ACCEPTED_202)

        val newResponse = client.post("/v1/inform", ScalableMessage(2, "", ScalableMessageType.LAUNCH_NODE))
        assertThat(newResponse.code).isEqualTo(HttpStatus.CONFLICT_409)

        val wrongReadyResponse = client.post("/v1/inform", ScalableMessage(2, "localhost:7072", ScalableMessageType.READY))
        assertThat(wrongReadyResponse.code).isEqualTo(HttpStatus.BAD_REQUEST_400)

        val readyResponse = client.post("/v1/inform", ScalableMessage(3, "localhost:7073", ScalableMessageType.READY))
        assertThat(readyResponse.code).isEqualTo(HttpStatus.ACCEPTED_202)

        val completeResponse = client.post("/v1/inform", ScalableMessage(3, "localhost:7073", ScalableMessageType.SCALE_COMPLETE))
        assertThat(completeResponse.code).isEqualTo(HttpStatus.ACCEPTED_202)
    }

    @Test
    internal fun `This node starts launching process`() = JavalinTest.test(app) { _, client ->
        val mockLauncher = mockkClass(LocalNodeLauncher::class)
        every { mockLauncher.launchNode(3) } returns Unit
        distributedCache.mockNodeLauncher(mockLauncher)

        val launchResponse = client.post("/v1/launch-node")
        assertThat(launchResponse.code).isEqualTo(HttpStatus.OK_200)

        val newResponse = client.post("/v1/inform", ScalableMessage(1, "", ScalableMessageType.LAUNCH_NODE))
        assertThat(newResponse.code).isEqualTo(HttpStatus.CONFLICT_409)
    }
}