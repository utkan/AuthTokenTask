package com.sharenow.challenge

import com.google.common.truth.Truth.assertThat
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.TestScheduler
import org.junit.Before
import org.junit.Test
import org.threeten.bp.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * The unit test checking the acceptance criteria of the [AuthTokenProvider].
 *
 * Please, do not change this file!
 * (If you need more unit tests, please implement them in a separate file)
 */
class AuthTokenProviderTest {

    private lateinit var testStartTime: ZonedDateTime

    private val isLoggedInObservable = BehaviorRelay.createDefault(true)

    val currentTimeFunction = CurrentTimeFunction()

    inner class CurrentTimeFunction : () -> ZonedDateTime {

        lateinit var time: ZonedDateTime

        override fun invoke(): ZonedDateTime {
            return time
        }
    }

    private val refreshAuthToken = Single.defer {
        Single.just(createAuthToken())
    }

    private val testScheduler = TestScheduler()

    private val provider = AuthTokenProvider(
        computationScheduler = testScheduler,
        refreshAuthToken = refreshAuthToken,
        isLoggedInObservable = isLoggedInObservable,
        currentTime = currentTimeFunction
    )

    @Before
    fun setUp() {
        testStartTime = ZonedDateTime.now()
        currentTimeFunction.time = testStartTime
    }

    /**
     * Getting an auth token should be an authenticated API call. Calling it without being logged in will never succeed
     * and should be prevented.
     */
    @Test
    fun `No refresh for logged out users`() {
        // Given
        val refreshAuthToken = Single.error<AuthToken>(IllegalStateException("Should never be called"))
        val isLoggedInObservable = BehaviorRelay.createDefault(false)

        // When
        val testObserver = AuthTokenProvider(
            computationScheduler = testScheduler,
            refreshAuthToken = refreshAuthToken,
            isLoggedInObservable = isLoggedInObservable,
            currentTime = currentTimeFunction
        ).observeToken().test()

        // Then
        testObserver.assertNoValues()
        testObserver.assertNoCompleteOrErrors()
    }

    /**
     * Whenever the user is logged in and subscribes to [AuthTokenProvider.observeToken] for the first time, a token
     * should be requested by subscribing on the refreshAuthToken Single.
     */
    @Test
    fun `First subscriber triggers a refresh`() {
        // Given
        val token = "some token"
        val refreshAuthToken =
            Single.just(AuthToken(token, ZonedDateTime.now().plusMinutes(TOKEN_VALID_MINUTES)))

        // When
        val testObserver = AuthTokenProvider(
            computationScheduler = testScheduler,
            refreshAuthToken = refreshAuthToken,
            isLoggedInObservable = isLoggedInObservable,
            currentTime = currentTimeFunction
        ).observeToken().test()

        // Then
        testObserver.assertValue(token)
        testObserver.assertNoCompleteOrErrors()
    }

    /**
     * Whenever there is more than one subscriber on [AuthTokenProvider.observeToken], but there is no token cached yet,
     * there should only be one request performed for a new auth token.
     */
    @Test
    fun `Observable is shared between multiple subscribers and does not call refresh multiple times simultaneously`() {
        // Given
        var count = 0
        val refreshAuthToken = Single.never<AuthToken>()
            .doOnSubscribe { count++ }

        val provider = AuthTokenProvider(
            computationScheduler = testScheduler,
            refreshAuthToken = refreshAuthToken,
            isLoggedInObservable = isLoggedInObservable,
            currentTime = currentTimeFunction
        )

        val testObserver = provider.observeToken().test()

        // When
        val testObserver2 = provider.observeToken().test()

        // Then
        assertThat(count).isEqualTo(1)
        assertThat(testObserver.values()).containsExactlyElementsIn(testObserver2.values())
        testObserver.assertNoCompleteOrErrors()

    }


    /**
     * The tokens retrieved through the refreshAuthToken Single should be memory cached as long as they are valid. This should
     * even survive all observers being disposed. Whenever a new observer subscribes, the cached value should be
     * retrieved without the need for any network request.
     */
    @Test
    fun `Token is memory cached between non-concurrent subscribers`() {
        // Given
        val testObserver = provider.observeToken().test()
        testObserver.dispose()
        val token = testObserver.values().last()

        // When
        val testObserver2 = provider.observeToken().test()
        assertThat(testObserver2.valueCount()).isEqualTo(1)
        val token2 = testObserver2.values().last()

        // Then
        assertThat(token).isEqualTo(token2)
        testObserver.assertNoCompleteOrErrors()
    }

    /**
     * Whenever we cache a token in memory, we need to make sure that this cache is cleared whenever the user logs out, so that
     * it cannot be reused by the next user that logs in.
     */
    @Test
    fun `Memory cached token will be cleared on logout and not be reused on next login`() {
        // Given
        val isLoggedInRelay = BehaviorRelay.createDefault(true)
        val provider = AuthTokenProvider(
            computationScheduler = testScheduler,
            refreshAuthToken = refreshAuthToken,
            isLoggedInObservable = isLoggedInRelay,
            currentTime = currentTimeFunction
        )

        val testObserver = provider.observeToken().test()
        val token = testObserver.values().last()

        // When
        isLoggedInRelay.accept(false)
        isLoggedInRelay.accept(true)

        // Then
        val token2 = testObserver.values().last()

        // Then
        assertThat(token).isNotEqualTo(token2)
        testObserver.assertNoCompleteOrErrors()
    }

    /**
     * Whenever we cache a token in memory, we need to make sure that this cache is cleared whenever the token becomes invalid.
     * If all observers of the auth token are gone for enough time to let it become invalid, the next observer that
     * subscribes to an auth token will trigger a new auth token request through the refreshAuthToken Single.
     */
    @Test
    fun `A memory cached token that has expired should not be emitted to a future subscriber`() {
        // Given
        val testObserver = provider.observeToken().test()
        testObserver.dispose()
        val token = testObserver.values().last()

        advanceTimeBy(TOKEN_VALID_MINUTES)

        // When
        val testObserver2 = provider.observeToken().test()
        val token2 = testObserver2.values().last()

        // Then
        assertThat(token).isNotEqualTo(token2)
        testObserver.assertNoCompleteOrErrors()
    }

    /**
     * Auth tokens need to be refreshed, whenever they become invalid. However, if there is no observer that actually
     * needs one, these network requests are unnecessary and should be prevented.
     */
    @Test
    fun `Token is not refreshed while there is no subscriber`() {
        // Given
        var count = 0

        val provider = AuthTokenProvider(
            computationScheduler = testScheduler,
            refreshAuthToken = refreshAuthToken.doOnSubscribe { count++ },
            isLoggedInObservable = isLoggedInObservable,
            currentTime = currentTimeFunction
        )

        val testObserver = provider.observeToken().test()
        testObserver.dispose()

        // When
        advanceTimeBy(TOKEN_VALID_MINUTES)

        // Then
        assertThat(count).isEqualTo(1)
        testObserver.assertNoCompleteOrErrors()
    }

    /**
     * Auth tokens need to be refreshed, whenever they become invalid. However, whenever the user logs out, these
     * network requests become unnecessary and should be prevented.
     */
    @Test
    fun `Token is not refreshed whenever the user logs out`() {
        // Given
        var count = 0

        val provider = AuthTokenProvider(
            computationScheduler = testScheduler,
            refreshAuthToken = refreshAuthToken.doOnSubscribe { count++ },
            isLoggedInObservable = isLoggedInObservable,
            currentTime = currentTimeFunction
        )

        val testObserver = provider.observeToken().test()
        isLoggedInObservable.accept(false)

        // When
        advanceTimeBy(TOKEN_VALID_MINUTES)

        // Then
        assertThat(count).isEqualTo(1)
        testObserver.assertNoCompleteOrErrors()
    }

    /**
     * If the call within refreshAuthToken fails, we expect a very minimal error handling of "Just try it one more time".
     * It does not matter, if the request fails after that one additional attempt.
     */
    @Test
    fun `If the refresh call fails, it will be retried once`() {
        // Given
        var count = 0
        val token = createAuthToken()

        val provider = AuthTokenProvider(
            computationScheduler = testScheduler,
            refreshAuthToken = Single.fromCallable {
                if (count == 0) {
                    count++
                    throw IllegalStateException("Some error")
                } else {
                    token
                }
            },
            isLoggedInObservable = isLoggedInObservable,
            currentTime = currentTimeFunction
        )

        // When
        val testObserver = provider.observeToken().test()


        // Then
        assertThat(testObserver.valueCount()).isEqualTo(1)
        assertThat(testObserver.values().last()).isEqualTo(token.token)
        testObserver.assertNoCompleteOrErrors()
    }

    /**
     * If the call within refreshAuthToken returns us an invalid auth token (one that is not valid anymore), we should
     * treat this as an error and retry the call.
     */
    @Test
    fun `If the refreshed token is not valid, another refresh will be triggered`() {
        // Given
        var count = 0

        val provider = AuthTokenProvider(
            computationScheduler = testScheduler,
            refreshAuthToken = Single.fromCallable {
                if (count == 0) {
                    count++
                    AuthToken(
                        token = UUID.randomUUID().toString(),
                        validUntil = currentTimeFunction().minusSeconds(1)
                    )
                } else {
                    createAuthToken()
                }
            },
            isLoggedInObservable = isLoggedInObservable,
            currentTime = currentTimeFunction
        )

        // When
        val testObserver = provider.observeToken().test()


        // Then
        assertThat(testObserver.valueCount()).isEqualTo(1)
        testObserver.assertNoCompleteOrErrors()
    }

    /**
     * Whenever we cache a token in memory, we need to make sure that this cache is cleared whenever the token becomes invalid.
     * Also, we expect that a new one is requested automatically, whenever the current token turns invalid.
     *
     * Hint: You probably want to fix this one last.
     */
    @Test
    fun `If the memory cached token expired, fetch a new one`() {
        // Given
        val testObserver = provider.observeToken().test()

        // When
        advanceTimeBy(TOKEN_VALID_MINUTES)

        // Then
        val tokens = testObserver.values()
        assertThat(tokens).hasSize(2)
        testObserver.assertNoCompleteOrErrors()
    }

    /**
     * If tokens are valid for just a short amount of time, the refreshing mechanism still needs to work.
     */
    @Test
    fun `Refreshing works for short token validity`() {
        // Given
        val provider = AuthTokenProvider(
            computationScheduler = testScheduler,
            refreshAuthToken = Single.defer { Single.just(AuthToken(
                token = UUID.randomUUID().toString(),
                validUntil = currentTimeFunction().plusSeconds(5L)
            ))
            },
            isLoggedInObservable = isLoggedInObservable,
            currentTime = currentTimeFunction
        )
        val testObserver = provider.observeToken().test()

        // When
        currentTimeFunction.time = currentTimeFunction.time.plusSeconds(2L)
        testScheduler.advanceTimeBy(2L, TimeUnit.SECONDS)

        // Then
        val tokens = testObserver.values()
        assertThat(tokens).hasSize(1)

        // When
        currentTimeFunction.time = currentTimeFunction.time.plusSeconds(3L)
        testScheduler.advanceTimeBy(3L, TimeUnit.SECONDS)

        // Then
        val latestTokens = testObserver.values()
        assertThat(latestTokens).hasSize(2)

        testObserver.assertNoCompleteOrErrors()
    }

    private fun createAuthToken(): AuthToken {
        return AuthToken(
            token = UUID.randomUUID().toString(),
            validUntil = currentTimeFunction().plusMinutes(TOKEN_VALID_MINUTES)
        )
    }

    @Suppress("SameParameterValue")
    private fun advanceTimeBy(minutes: Long) {
        currentTimeFunction.time = currentTimeFunction.time.plusMinutes(minutes)
        testScheduler.advanceTimeBy(minutes, TimeUnit.MINUTES)
    }

    companion object {
        private const val TOKEN_VALID_MINUTES = 5L
    }
}

private fun <T> TestObserver<T>.assertNoCompleteOrErrors() {
    assertNoErrors()
    assertNotComplete()
}