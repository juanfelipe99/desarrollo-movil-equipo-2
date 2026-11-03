package com.equipo2.vinilos.support

import com.sun.net.httpserver.HttpServer
import java.io.Closeable
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap

/** Servidor HTTP local que responde lo que la prueba le indique, para probar la capa de datos sin BackVynils. */
class FakeApiServer : Closeable {

    private class Response(val code: Int, val body: String)

    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    private val responses = ConcurrentHashMap<String, Response>()

    /** Rutas pedidas, en orden. */
    val requests: MutableList<String> = mutableListOf()

    /** Cuerpo de la última petición a cada ruta que envió datos, como un POST. */
    val bodies = ConcurrentHashMap<String, String>()

    val baseUrl: String
        get() = "http://127.0.0.1:${server.address.port}/"

    init {
        server.createContext("/") { exchange ->
            val path = exchange.requestURI.path.removePrefix("/")
            synchronized(requests) { requests += path }
            val body = exchange.requestBody.bufferedReader().use { it.readText() }
            if (body.isNotEmpty()) bodies[path] = body

            // Lo que no se configuró responde 404, como una ruta que no existe
            val response = responses[path] ?: Response(404, """{"message":"Not Found"}""")
            val bytes = response.body.toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(response.code, if (bytes.isEmpty()) -1 else bytes.size.toLong())
            if (bytes.isNotEmpty()) exchange.responseBody.use { it.write(bytes) }
            exchange.close()
        }
        server.start()
    }

    fun respond(path: String, body: String, code: Int = 200) {
        responses[path] = Response(code, body)
    }

    override fun close() = server.stop(0)
}
