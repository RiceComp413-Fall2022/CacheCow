package cache.distributed.launcher

import NodeId
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

class LocalNodeLauncher: INodeLauncher {
    override fun launchNode(nodeId: NodeId): Boolean {

        val currentDirectory = System.getProperty("user.dir")
        val newPort = 7070 + nodeId
        val writer = BufferedWriter(FileWriter("$currentDirectory/nodes.txt"))
        writer.write("localhost:$newPort")

        val args = arrayOf("/bin/bash", "-c", "./gradlew run --args 'local $nodeId $newPort -s -n'")
        val pb = ProcessBuilder(*args)
        pb.directory(File(currentDirectory))
        pb.redirectOutput(File("$currentDirectory/out$nodeId.txt"))
        pb.start()

        return true
    }
}