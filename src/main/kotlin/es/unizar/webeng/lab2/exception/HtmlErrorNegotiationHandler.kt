package es.unizar.webeng.lab2.exception

import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.resource.NoResourceFoundException

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class HtmlErrorNegotiationHandler {
    @ExceptionHandler(NoResourceFoundException::class, produces = [MediaType.TEXT_HTML_VALUE])
    fun handleNotFound(
        ex: NoResourceFoundException,
        request: HttpServletRequest,
    ): ModelAndView = htmlError(ex.statusCode, request)

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class, produces = [MediaType.TEXT_HTML_VALUE])
    fun handleMethodNotAllowed(request: HttpServletRequest): ModelAndView = htmlError(HttpStatus.METHOD_NOT_ALLOWED, request)

    @ExceptionHandler(InvalidTimeZoneException::class, produces = [MediaType.TEXT_HTML_VALUE])
    fun handleInvalidTimeZone(request: HttpServletRequest): ModelAndView = htmlError(HttpStatus.BAD_REQUEST, request)

    private fun htmlError(
        status: HttpStatusCode,
        request: HttpServletRequest,
    ): ModelAndView =
        ModelAndView("error").apply {
            addObject("status", status.value())
            addObject("path", request.requestURI)
            this.status = status
        }
}
