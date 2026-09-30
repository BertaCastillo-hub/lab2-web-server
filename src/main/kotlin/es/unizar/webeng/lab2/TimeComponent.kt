package es.unizar.webeng.lab2

import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.ZoneId

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
    @GetMapping("/time")
    fun time(
        @RequestParam(required = false) zone: String?,
    ): TimeDTO = (zone?.let { service.now(ZoneId.of(it)) } ?: service.now()).toDTO()
}
