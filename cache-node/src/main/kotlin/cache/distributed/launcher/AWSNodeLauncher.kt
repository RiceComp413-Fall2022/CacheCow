package cache.distributed.launcher

import NodeId
import java.io.File

class AWSNodeLauncher: INodeLauncher {
    override fun launchNode(nodeId: NodeId): Boolean {
        print("AWS Launcher: Performing autoscaling for new node id $nodeId\n")
        val args = arrayOf("/bin/bash", "-c", "python3 pasture.py add 1 -s")
        val pb = ProcessBuilder(*args)
        val currentDirectory = System.getProperty("user.dir")
        print("Command: ${pb.command()}\n")
        print("Current directory: $currentDirectory\n")

        pb.directory(File(currentDirectory))
        pb.redirectOutput(File("$currentDirectory/pasture_out.txt"))
        pb.start()
        return true
    }
}