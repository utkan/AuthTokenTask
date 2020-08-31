package com.sharenow.challenge

import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.threeten.bp.ZonedDateTime
import java.util.*

class FetchAuthTokenUseCaseTest {

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
    fun `fetcher not fetches if user is not loggedIn`() {
        // given
        val refreshAuthToken = Single.just(EMPTY_TOKEN)
        val isLoggedInObservable = Observable.just(false)
        val ifNotLoggedIn = Observable.just(false)
        val doOnNextEvents = listOf<NextEvents> {
            throw IllegalAccessError()
        }

        val fetchAuthTokenUseCase = FetchAuthTokenUseCase.Impl(
            refreshAuthToken = refreshAuthToken,
            isLoggedInObservable = isLoggedInObservable,
            currentTime = currentTimeFunction
        )

        // when
        val output = fetchAuthTokenUseCase.fetcher(
            ifNotLoggedIn = ifNotLoggedIn,
            doOnNextEvents = doOnNextEvents
        ).test()

        // then
        output.assertNoErrors()
        output.assertNoValues()
    }

    @Test
    fun `fetcher fetches if user is loggedIn`() {
        // given
        var counter = 1
        val refreshAuthToken = Single.just(RND_TOKEN)
        val isLoggedInObservable = Observable.just(true)
        val ifNotLoggedIn = Observable.just(true)
        val doOnNextEvents = listOf<NextEvents> {
            counter++
        }

        val fetchAuthTokenUseCase = FetchAuthTokenUseCase.Impl(
            refreshAuthToken = refreshAuthToken,
            isLoggedInObservable = isLoggedInObservable,
            currentTime = currentTimeFunction
        )

        // when
        val output = fetchAuthTokenUseCase.fetcher(
            ifNotLoggedIn = ifNotLoggedIn,
            doOnNextEvents = doOnNextEvents
        ).test()

        // then
        assertEquals(2, counter)
        output.assertValue(RND_TOKEN)
    }

    companion object {
        val EMPTY_TOKEN = AuthToken("", ZonedDateTime.now())
        val RND_TOKEN = AuthToken(
            UUID.randomUUID().toString(), ZonedDateTime.now()
                .plusSeconds(5)
        )
    }
}
