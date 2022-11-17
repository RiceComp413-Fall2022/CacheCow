import node.Node
import java.io.File

const val nodeListPath = "nodes.txt"

/**
 * The entry point to CacheCow.
 *
 * @param args The command-line arguments
 */
fun main(args: Array<String>) {
    var nodeId = 0
    var port = 6060
    var scalable = false

    if (args.size >= 2) {
        try {
            nodeId = Integer.parseInt(args[0])
            port = Integer.parseInt(args[1])
            if (args.size == 3 && args[2] == "-s") {
                scalable = true
            }
        } catch (e: NumberFormatException) {
            System.err.println("Invalid node ID or port.")
            return
        }
    }

    val node = Node(nodeId, File(nodeListPath).bufferedReader().readLines().toMutableList(), port, scalable)

    node.start()
}