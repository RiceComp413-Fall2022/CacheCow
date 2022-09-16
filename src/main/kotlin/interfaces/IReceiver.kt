package interfaces

import io.javalin.http.Handler

/**
 * API between the user http request and the receiver.
 */
interface IReceiver {

    /**
     * Handler to print help messages in the event of an invalid user request.
     */
    val helpMessageHandler: Handler

    /**
     * Initializes all receiver HTTP responses.
     */
    fun initReceiver() {
        initializeHelloWorld()
        initializeStore()
        initializeFetch()
    }

    /**
     * Initialize HTTP response to print 'Hello World!'. Useful for testing.
     */
    fun initializeHelloWorld()

    /**
     * Initialize HTTP response to store an object from the memory cache.
     */
    fun initializeStore()

    /**
     * Initialize HTTP response to fetch an object from the memory cache.
     */
    fun initializeFetch()
}