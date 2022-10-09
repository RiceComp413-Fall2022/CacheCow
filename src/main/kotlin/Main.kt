const val nodeCount = 1

fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        try {
            Node(Integer.parseInt(args[0]), nodeCount)
        } catch (e: NumberFormatException) {
            System.err.println("Invalid node ID.")
        }
    } else {
        Node(0, nodeCount)
    }
}