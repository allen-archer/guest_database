package com.allenarcher.guest.database

import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.orm.jpa.JpaSystemException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ExceptionHandler {
    private val logger = LoggerFactory.getLogger(ExceptionHandler::class.java)

    @ExceptionHandler(DataIntegrityViolationException::class, JpaSystemException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleConstraintViolation(e: Exception): Map<String, String?> {
        val cause = (e as? JpaSystemException)?.rootCause ?: (e as? DataIntegrityViolationException)?.rootCause
        val message = cause?.message ?: e.message
        logger.warn("Constraint violation: $message")
        return mapOf("error" to message)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleIllegalArgument(e: IllegalArgumentException): Map<String, String?> {
        logger.warn(e.message)
        return mapOf("error" to e.message)
    }
}
