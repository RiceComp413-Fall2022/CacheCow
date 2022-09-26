fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        try {
            Node(Integer.parseInt(args[0]))
        } catch (e: NumberFormatException) {
            System.err.println("Invalid node ID.")
        }
    } else {
        Node(0)
    }
}