package com.sharenow.challenge

import io.reactivex.Completable
import io.reactivex.Scheduler
import org.threeten.bp.Duration
import org.threeten.bp.ZonedDateTime
import java.util.concurrent.TimeUnit

interface AuthTokenScheduler {

    fun scheduleRefresh(
        start: ZonedDateTime,
        end: ZonedDateTime,
        doOnComplete: () -> Unit
    ): Completable

    class Impl constructor(
        private val computationScheduler: Scheduler
    ) : AuthTokenScheduler {

        override fun scheduleRefresh(
            start: ZonedDateTime,
            end: ZonedDateTime,
            doOnComplete: () -> Unit
        ): Completable {
            return getTimer(start, end).doOnComplete { doOnComplete() }
        }

        private fun getTimer(start: ZonedDateTime, end: ZonedDateTime): Completable {
            return Completable.timer(
                Duration.between(start, end).toMillis(),
                TimeUnit.MILLISECONDS,
                computationScheduler
            )
        }

    }
}
