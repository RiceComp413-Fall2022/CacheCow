package launcher

import NodeId
import java.io.File

/**
 * Concrete node launcher that uses pasture.py to launch another node on ec2.
 */
class AWSNodeLauncher: INodeLauncher {
    override fun launchNode(nodeId: NodeId) {
        print("AWS LAUNCHER: Launching new node with node id $nodeId\n")
        val args = arrayOf("/bin/bash", "-c", "python3 pasture.py add 1 -s")
        val pb = ProcessBuilder(*args)

        pb.directory(File(".."))
        pb.redirectOutput(File("../pasture_out.txt"))
        pb.start()
    }
}