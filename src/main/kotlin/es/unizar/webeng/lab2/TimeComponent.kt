package es.unizar.webeng.lab2

import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.HtmlUtils
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class TimeDTO(
    val time: LocalDateTime,
)

interface TimeProvider {
    fun now(): LocalDateTime

    fun now(zone: ZoneId): LocalDateTime = now().atZone(ZoneId.systemDefault()).withZoneSameInstant(zone).toLocalDateTime()
}

@Service
class TimeService : TimeProvider {
    override fun now(): LocalDateTime = LocalDateTime.now()

    override fun now(zone: ZoneId): LocalDateTime = LocalDateTime.now(zone)
}

fun LocalDateTime.toDTO(): TimeDTO = TimeDTO(time = this)

@RestController
class TimeController(
    private val service: TimeProvider,
) {
    @GetMapping("/time", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun timeJson(
        @RequestParam(required = false) zone: String?,
    ): TimeDTO = (zone?.let { service.now(ZoneId.of(it)) } ?: service.now()).toDTO()

    @GetMapping("/time", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun timeText(
        @RequestParam(required = false) zone: String?,
    ): String {
        val zoneId = zone?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        val now = currentTime(zone)
        return now.atZone(zoneId).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"))
    }

    @GetMapping("/time", produces = [MediaType.TEXT_HTML_VALUE])
    fun timeHtml(
        @RequestParam(required = false) zone: String?,
    ): String {
        val zoneId = zone?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        val now = currentTime(zone)
        val safeZone = HtmlUtils.htmlEscape(zoneId.id)
        return listOf(
            "<div class=\"time-widget\">",
            "<h2>Current Server Time</h2>",
            "<time datetime=\"$now\">$now</time>",
            "<p>Zone: $safeZone</p>",
            "</div>",
        ).joinToString("\n")
    }

    private fun currentTime(zone: String?): LocalDateTime = zone?.let { service.now(ZoneId.of(it)) } ?: service.now()
}
