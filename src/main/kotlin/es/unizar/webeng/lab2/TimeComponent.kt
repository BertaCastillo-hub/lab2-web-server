package es.unizar.webeng.lab2

import org.springframework.context.MessageSource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.WebRequest
import org.springframework.web.util.HtmlUtils
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    private val messageSource: MessageSource,
) {
    @GetMapping("/time", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun timeJson(
        @RequestParam(required = false) zone: String?,
        request: WebRequest,
        locale: Locale,
    ): ResponseEntity<Map<String, String>> {
        val (now, zoneId) = currentTime(zone)
        val greeting = messageSource.getMessage("time.greeting", null, locale)
        return conditionalResponse(
            request,
            now,
            zoneId,
            mapOf("message" to greeting, "time" to now.toString()),
        )
    }

    @GetMapping("/time", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun timeText(
        @RequestParam(required = false) zone: String?,
        request: WebRequest,
        locale: Locale,
    ): ResponseEntity<String> {
        val (now, zoneId) = currentTime(zone)
        val greeting = messageSource.getMessage("time.greeting", null, locale)
        val time = now.atZone(zoneId).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z", locale))
        return conditionalResponse(request, now, zoneId, "$greeting $time")
    }

    @GetMapping("/time", produces = [MediaType.TEXT_HTML_VALUE])
    fun timeHtml(
        @RequestParam(required = false) zone: String?,
        request: WebRequest,
        locale: Locale,
    ): ResponseEntity<String> {
        val (now, zoneId) = currentTime(zone)
        val greeting = HtmlUtils.htmlEscape(messageSource.getMessage("time.greeting", null, locale))
        val zoneLabel = HtmlUtils.htmlEscape(messageSource.getMessage("time.zone", null, locale))
        val safeZone = HtmlUtils.htmlEscape(zoneId.id)
        val body =
            listOf(
                "<div class=\"time-widget\">",
                "<h2>$greeting</h2>",
                "<time datetime=\"$now\">$now</time>",
                "<p>$zoneLabel: $safeZone</p>",
                "</div>",
            ).joinToString("\n")
        return conditionalResponse(request, now, zoneId, body)
    }

    private fun currentTime(zone: String?): Pair<LocalDateTime, ZoneId> {
        val zoneId = zone?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        val now = if (zone == null) service.now() else service.now(zoneId)
        return now to zoneId
    }

    private fun <T : Any> conditionalResponse(
        request: WebRequest,
        now: LocalDateTime,
        zoneId: ZoneId,
        body: T,
    ): ResponseEntity<T> {
        val lastModified = now.atZone(zoneId).toInstant().toEpochMilli()
        val vary = "Accept, Accept-Language, Accept-Encoding"
        if (request.checkNotModified(lastModified)) {
            return ResponseEntity
                .status(HttpStatus.NOT_MODIFIED)
                .lastModified(lastModified)
                .header(HttpHeaders.VARY, vary)
                .build()
        }
        return ResponseEntity
            .ok()
            .lastModified(lastModified)
            .header(HttpHeaders.VARY, vary)
            .body(body)
    }
}
