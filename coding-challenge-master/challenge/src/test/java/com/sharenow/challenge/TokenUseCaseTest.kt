package com.sharenow.challenge

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import org.threeten.bp.ZonedDateTime
import java.util.*

class TokenUseCaseTest {

    private lateinit var testStartTime: ZonedDateTime
    private val currentTimeFunction = CurrentTimeFunction()

    private class CurrentTimeFunction : () -> ZonedDateTime {

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
    fun `when subject is empty, getTokenForLoggedInUser return RND_TOKEN`() {
        // given
        val tokenUseCase = TokenUseCase.Impl(
            currentTimeFunction
        )
        val tokenSubject: BehaviorSubject<AuthToken> = BehaviorSubject.createDefault(EMPTY_TOKEN)
        val fetchToken: () -> Observable<AuthToken> = {
            Observable.just(RND_TOKEN)
        }
        // when
        val output = tokenUseCase.getTokenForLoggedInUser(
            tokenSubject = tokenSubject,
            fetchToken = fetchToken
        ).test()

        // then
        output.assertValue(RND_TOKEN)
    }

    @Test
    fun `when subject is not empty, getTokenForLoggedInUser return RND_TOKEN`() {
        // given
        val tokenUseCase = TokenUseCase.Impl(
            currentTimeFunction
        )
        val tokenSubject: BehaviorSubject<AuthToken> = BehaviorSubject.create()
        val fetchToken: () -> Observable<AuthToken> = {
            Observable.empty()
        }
        // when
        tokenSubject.onNext(RND_TOKEN)
        val output = tokenUseCase.getTokenForLoggedInUser(
            tokenSubject = tokenSubject,
            fetchToken = fetchToken
        ).test()

        // then
        val actual = output.values().first()
        assertEquals(RND_TOKEN.token, actual.token)
    }

    companion object {
        val EMPTY_TOKEN = AuthToken("", ZonedDateTime.now())
        val RND_TOKEN = AuthToken(UUID.randomUUID().toString(), ZonedDateTime.now().plusSeconds(5))
    }
}
