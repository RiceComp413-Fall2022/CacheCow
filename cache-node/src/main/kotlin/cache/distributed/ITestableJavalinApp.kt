package cache.distributed

import io.javalin.Javalin

interface ITestableJavalinApp {

    fun getApp(): Javalin

}