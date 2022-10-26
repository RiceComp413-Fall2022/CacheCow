import node.Node

const val nodeCount = 3
const val capacity = 2

/**
 * The entry point to CacheCow.
 *
 * @param args The command-line arguments
 */
fun main(args: Array<String>) {
    var nodeId = 0
    var port = 7070
    if (args.size == 2) {
        try {
            nodeId = Integer.parseInt(args[0])
            port = Integer.parseInt(args[1])
        } catch (e: NumberFormatException) {
            System.err.println("Invalid node ID or port.")
            return
        }
    }

    val node = Node(nodeId, port, nodeCount, capacity)

    node.start()
}