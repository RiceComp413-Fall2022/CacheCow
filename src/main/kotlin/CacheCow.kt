import node.Node

const val nodeCount = 2
const val capacity = 2

/**
 * The entry point to CacheCow.
 *
 * @param args The command-line arguments
 */
fun main(args: Array<String>) {
    var nodeId = 0
    if (args.isNotEmpty()) {
        try {
            nodeId = Integer.parseInt(args[0])
        } catch (e: NumberFormatException) {
            System.err.println("Invalid node ID.")
            return
        }
    }

    val node = Node(nodeId, nodeCount, capacity)

    node.start()
}