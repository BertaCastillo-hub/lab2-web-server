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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
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
    fun timeIsPlainTextWhenRequested() {
        mockMvc
            .perform(get("/time").param("zone", "UTC").header("Accept-Language", "en").accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andExpect(content().string("Current time is 2026-09-30 12:34:56 UTC"))
    }

    @Test
    fun jsonUsesRequestedLanguage() {
        mockMvc
            .perform(get("/time").header("Accept-Language", "es-ES").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("La hora actual es"))
            .andExpect(jsonPath("$.time").value("2026-09-30T12:34:56"))
    }

    @Test
    fun textAndHtmlUseRequestedLanguage() {
        mockMvc
            .perform(get("/time").param("zone", "UTC").header("Accept-Language", "es-ES").accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isOk)
            .andExpect(content().string("La hora actual es 2026-09-30 12:34:56 UTC"))

        mockMvc
            .perform(get("/time").param("zone", "UTC").header("Accept-Language", "es-ES").accept(MediaType.TEXT_HTML))
            .andExpect(status().isOk)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("<h2>La hora actual es</h2>")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Zona: UTC")))
    }

    @Test
    fun timeIsHtmlWhenRequested() {
        mockMvc
            .perform(get("/time").param("zone", "UTC").accept(MediaType.TEXT_HTML))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("<div class=\"time-widget\">")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("2026-09-30T12:34:56")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Zone: UTC")))
    }

    @Test
    fun timeDefaultsToJsonWhenAcceptIsMissing() {
        mockMvc
            .perform(get("/time"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.time").value("2026-09-30T12:34:56"))
    }

    @Test
    fun matchingEtagReturnsNotModified() {
        val etag =
            mockMvc
                .perform(get("/time").param("zone", "UTC").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andReturn()
                .response
                .getHeader("ETag") ?: error("Expected ETag response header")

        mockMvc
            .perform(get("/time").param("zone", "UTC").header("If-None-Match", etag).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotModified)
            .andExpect(content().string(""))
    }

    @Test
    fun matchingLastModifiedReturnsNotModified() {
        val lastModified =
            mockMvc
                .perform(get("/time").param("zone", "UTC").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andReturn()
                .response
                .getHeader("Last-Modified") ?: error("Expected Last-Modified response header")

        mockMvc
            .perform(get("/time").param("zone", "UTC").header("If-Modified-Since", lastModified).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotModified)
            .andExpect(content().string(""))
    }
}
