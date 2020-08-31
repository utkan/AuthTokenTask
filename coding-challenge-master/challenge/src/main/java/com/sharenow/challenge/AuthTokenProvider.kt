package com.sharenow.challenge

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.Disposables
import io.reactivex.subjects.BehaviorSubject
import org.threeten.bp.ZonedDateTime

typealias TokenScheduler = AuthTokenScheduler.Impl
typealias FetchTokenUseCase = FetchAuthTokenUseCase.Impl

class AuthTokenProvider(

    /**
     * Scheduler used for background operations. Execute time relevant operations on this one,
     * so we can use [io.reactivex.schedulers.TestScheduler] within unit tests.
     */
    private val computationScheduler: Scheduler,

    /**
     * Single to be observed in order to get a new token.
     */
    private val refreshAuthToken: Single<AuthToken>,

    /**
     * Observable for the login state of the user. Will emit true, if he is logged in.
     */
    private val isLoggedInObservable: Observable<Boolean>,

    /**
     * Function that returns you the current time, whenever you need it. Please use this whenever you check the
     * current time, so we can manipulate time in unit tests.
     */
    private val currentTime: () -> ZonedDateTime,
    private val authTokenScheduler: AuthTokenScheduler = TokenScheduler(computationScheduler),
    private val authTokenFetcher: FetchAuthTokenUseCase = FetchTokenUseCase(
        refreshAuthToken,
        isLoggedInObservable,
        currentTime
    ),
    tokeUseCase: TokenUseCase = TokenUseCase.Impl(currentTime)
) {

    private var tokenSubject = BehaviorSubject.createDefault(EMPTY_TOKEN)
    private val token: Observable<AuthToken> = tokeUseCase
        .getTokenForLoggedInUser(tokenSubject, ::fetchToken)
        .share()
    private var timerDisposable = Disposables.disposed()

    /**
     * @return the observable auth token as a string
     */
    fun observeToken(): Observable<String> {
        return token
            .hide()
            .distinctUntilChanged()
            .map { it.token }
            .doOnDispose { timerDisposable.dispose() }
    }

    private fun fetchToken(): Observable<AuthToken> {
        return authTokenFetcher.fetcher(
            ifNotLoggedIn = invalidateTokenAndCancelTimer(),
            doOnNextEvents = listOf(
                tokenSubject::onNext,
                ::scheduleRefresh
            )
        )
    }

    private fun invalidateTokenAndCancelTimer(): Observable<Boolean> {
        return Observable.defer {
            timerDisposable.dispose()
            tokenSubject.onNext(EMPTY_TOKEN)
            Observable.just(false)
        }
    }

    private fun scheduleRefresh(token: AuthToken) {
        timerDisposable.dispose()
        timerDisposable = authTokenScheduler
            .scheduleRefresh(currentTime(), token.validUntil) { tokenSubject.onNext(EMPTY_TOKEN) }
            .subscribe()
    }

    companion object {
        val EMPTY_TOKEN = AuthToken("", ZonedDateTime.now())
    }
}
