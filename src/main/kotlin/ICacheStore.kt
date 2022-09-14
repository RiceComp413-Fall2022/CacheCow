interface ICacheStore {

    fun store(key: String)

    fun fetch(key: String)
}