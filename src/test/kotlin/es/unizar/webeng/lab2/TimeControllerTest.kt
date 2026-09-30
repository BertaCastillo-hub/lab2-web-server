package es.unizar.webeng.lab2

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@SpringBootTest
@AutoConfigureMockMvc
class TimeControllerTest {
    @TestConfiguration
    class FixedTimeConfiguration {
        @Bean
        @Primary
        fun fixedTimeProvider(): TimeProvider =
            object : TimeProvider {
                override fun now(): LocalDateTime = LocalDateTime.parse("2026-09-30T12:34:56")

                override fun now(zone: ZoneId): LocalDateTime = now().atZone(ZoneOffset.UTC).withZoneSameInstant(zone).toLocalDateTime()
            }

        @Bean
        fun testFailureController() = TestFailureController()
    }

    @RestController
    class TestFailureController {
        @GetMapping("/test/failure")
        fun fail(): String = throw IllegalStateException("Sensitive internal detail")
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun timeIsJson() {
        mockMvc
            .perform(get("/time").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.time").value("2026-09-30T12:34:56"))
    }

    @Test
    fun timeUsesRequestedZone() {
        mockMvc
            .perform(get("/time").param("zone", "Europe/Madrid").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.time").value("2026-09-30T14:34:56"))
    }

    @Test
    fun invalidTimeZoneReturnsProblemDetail() {
        mockMvc
            .perform(get("/time").param("zone", "Invalid/Zone").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("https://api.example.com/errors/invalid-timezone"))
            .andExpect(jsonPath("$.title").value("Invalid Time Zone Requested"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value("The zone 'Invalid/Zone' is not a valid ZoneId"))
            .andExpect(jsonPath("$.instance").value("/time"))
            .andExpect(jsonPath("$.invalidZone").value("Invalid/Zone"))
    }

    @Test
    fun invalidTimeZoneRendersHtmlForBrowsers() {
        val response =
            mockMvc
                .perform(get("/time").param("zone", "Invalid/Zone").accept(MediaType.TEXT_HTML))
                .andExpect(status().isBadRequest)
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andReturn()

        org.junit.jupiter.api.Assertions
            .assertTrue(response.response.contentAsString.contains("Error de solicitud"))
    }

    @Test
    fun unsupportedMethodReturnsProblemDetail() {
        mockMvc
            .perform(post("/time").accept(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(status().isMethodNotAllowed)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(405))
            .andExpect(jsonPath("$.instance").value("/time"))
    }

    @Test
    fun unexpectedExceptionReturnsProblemDetail() {
        mockMvc
            .perform(get("/test/failure").accept(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(status().isInternalServerError)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.instance").value("/test/failure"))
            .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
    }
}
