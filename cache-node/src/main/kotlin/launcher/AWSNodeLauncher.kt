package launcher

import NodeId
import java.io.File

class AWSNodeLauncher: INodeLauncher {
    override fun launchNode(nodeId: NodeId) {
        print("AWS Launcher: Performing autoscaling for new node id $nodeId\n")
        val args = arrayOf("/bin/bash", "-c", "python3 pasture.py add 1 -s")
        val pb = ProcessBuilder(*args)

        pb.directory(File(".."))
        pb.redirectOutput(File("../pasture_out.txt"))
        pb.start()
    }
}