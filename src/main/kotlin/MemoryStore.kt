import jdk.nashorn.internal.objects.Global.print

class MemoryStore : ICacheStore {

    override fun store(key: String) {
        print("Storing: $key")
    }

    override fun fetch(key: String) {
        print("Fetching: $key")
    }

}