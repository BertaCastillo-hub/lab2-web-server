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
            .perform(get("/time").param("zone", "UTC").accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andExpect(content().string("2026-09-30 12:34:56 UTC"))
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
}
