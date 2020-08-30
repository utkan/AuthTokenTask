package com.sharenow.challenge

import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.ChronoUnit

/**
 * DTO for an auth token.
 *
 * Please, do not change this file!
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
data class AuthToken(
    val token: String,
    val validUntil: ZonedDateTime
) {

    /**
     * Checks if this token has passed its validUntil date time.
     */
    fun isValid(currentTime: () -> ZonedDateTime): Boolean {
        return millisUntilInvalid(currentTime) > 0
    }

    /**
     * Calculates the time left for this token being valid in milliseconds.
     */
    fun millisUntilInvalid(currentTime: () -> ZonedDateTime): Long {
        return currentTime().until(validUntil, ChronoUnit.MILLIS)
    }
}