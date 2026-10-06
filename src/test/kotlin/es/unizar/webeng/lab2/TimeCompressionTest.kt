package es.unizar.webeng.lab2

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import java.io.ByteArrayInputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.zip.GZIPInputStream

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "server.ssl.enabled=false",
        "server.compression.enabled=true",
        "server.compression.min-response-size=1",
        "server.compression.mime-types=application/json,application/problem+json,text/html,text/plain",
    ],
)
class TimeCompressionTest {
    @Value("\${local.server.port}")
    private var port: Int = 0

    @Test
    fun gzipIsUsedWhenAccepted() {
        val request =
            HttpRequest
                .newBuilder(URI.create("http://localhost:$port/time"))
                .header("Accept", "application/json")
                .header("Accept-Encoding", "gzip")
                .GET()
                .build()
        val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofByteArray())

        assertEquals(200, response.statusCode())
        assertEquals("gzip", response.headers().firstValue("Content-Encoding").orElse(null))
        val body = GZIPInputStream(ByteArrayInputStream(response.body())).readBytes().decodeToString()
        assertTrue(body.contains("\"time\""))
    }
}
