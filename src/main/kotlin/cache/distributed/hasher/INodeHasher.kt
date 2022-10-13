package cache.distributed.hasher

import KeyVersionPair
import NodeId

interface INodeHasher {

    fun primaryHash(kvPair: KeyVersionPair): NodeId

}