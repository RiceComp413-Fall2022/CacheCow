package cache.distributed

interface ITestableDistributedCache<T>: ITestableJavalinApp {

    fun mockSender(mockSender: T)

}