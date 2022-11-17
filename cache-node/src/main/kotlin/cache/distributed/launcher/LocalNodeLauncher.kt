package cache.distributed.launcher

import NodeId
import java.io.File

class LocalNodeLauncher: INodeLauncher {
    override fun launchNode(nodeId: NodeId): Boolean {
        val args = arrayOf("/bin/bash", "-c", "./gradlew run --args '$nodeId ${6060 + nodeId} -s'")
        val pb = ProcessBuilder(*args)
        val currentDirectory = System.getProperty("user.dir")
        print("Command: ${pb.command()}\n")
        print("Current directory: $currentDirectory\n")

        pb.directory(File(currentDirectory))
        pb.redirectOutput(File("$currentDirectory/out$nodeId.txt"))
        pb.start()

        return true
    }
}