package cache.distributed

import io.javalin.Javalin

/**
 * An interface that allows for testing modules running Javalin.
 */
interface ITestableJavalinApp {

    /**
     * Returns the Javalin application used by this module.
     */
    fun getJavalinApp(): Javalin

}