package interfaces

import KeyVersionPair
import NodeId

interface IReceiverService {

    fun store(kvPair: KeyVersionPair, value: String, senderId: NodeId?)

    fun fetch(kvPair: KeyVersionPair, senderId: NodeId?): String
}