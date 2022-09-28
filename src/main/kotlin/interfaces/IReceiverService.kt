package interfaces

import KeyVersionPair
import NodeId

interface IReceiverService {

    fun storeClient(kvPair: KeyVersionPair, value: String)

    fun storeNode(kvPair: KeyVersionPair, value: String, senderId: NodeId)

    fun fetchClient(kvPair: KeyVersionPair): String

    fun fetchNode(kvPair: KeyVersionPair, senderId: NodeId): String
}