class MemoryStore : ICacheStore {

    override fun store(key: String) {
        print("Storing: $key\n")
    }

    override fun fetch(key: String) {
        print("Fetching: $key\n")
    }

}