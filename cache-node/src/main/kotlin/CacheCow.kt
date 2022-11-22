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
    var port = 7070
    var isAWS = false
    var scalable = false
    var isNewNode = false

    if (args.size >= 2) {
        try {
            isAWS = args[0] == "aws"
            nodeId = Integer.parseInt(args[1])
            port = Integer.parseInt(args[2])
            if (args.size >= 4) {
                scalable = args[3] == "-s"
                if (args.size >= 5 && scalable) {
                    isNewNode = args[4] == "-n"
                }
            }
        } catch (e: NumberFormatException) {
            System.err.println("Invalid node ID or port.")
            return
        }
    }

    val node = Node(nodeId, File(nodeListPath).bufferedReader().readLines().toMutableList(), port, isAWS, scalable, isNewNode)

    node.start()
}