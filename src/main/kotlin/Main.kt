fun main(args: Array<String>) {
    val nodeHasher = NodeHasher(2)
    if (args.isNotEmpty()) {
        try {
            Node(Integer.parseInt(args[0]), nodeHasher)
        } catch (e: NumberFormatException) {
            System.err.println("Invalid node ID.")
        }
    } else {
        Node(0, nodeHasher)
    }
}