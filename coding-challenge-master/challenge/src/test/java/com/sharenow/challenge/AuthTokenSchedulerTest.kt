package com.sharenow.challenge

import io.reactivex.schedulers.TestScheduler
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.threeten.bp.ZonedDateTime
import java.util.concurrent.TimeUnit

class AuthTokenSchedulerTest {

    private val testScheduler = TestScheduler()
    private lateinit var testStartTime: ZonedDateTime
    private val currentTimeFunction = CurrentTimeFunction()

    class CurrentTimeFunction : () -> ZonedDateTime {

        lateinit var time: ZonedDateTime

        override fun invoke(): ZonedDateTime {
            return time
        }
    }

    @Before
    fun setUp() {
        testStartTime = ZonedDateTime.now()
        currentTimeFunction.time = testStartTime
    }

    @Test
    fun scheduleRefresh() {
        // given
        val authTokenScheduler = AuthTokenScheduler.Impl(testScheduler)
        var counter = 1

        // when
        val testObserver = authTokenScheduler.scheduleRefresh(
            start = testStartTime,
            end = testStartTime.plusSeconds(1),
            doOnComplete = {
                counter++
            }
        ).test()

        currentTimeFunction.time = currentTimeFunction.time.plusSeconds(1)
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)

        // then
        assertEquals(2, counter)
        testObserver.assertComplete()
    }
}
