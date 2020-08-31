package com.sharenow.challenge

import io.reactivex.Observable
import io.reactivex.Single
import org.threeten.bp.ZonedDateTime

typealias NextEvents = (AuthToken) -> Unit

interface FetchAuthTokenUseCase {

    fun fetcher(
        ifNotLoggedIn: Observable<Boolean>,
        doOnNextEvents: List<NextEvents> = emptyList()
    ): Observable<AuthToken>

    class Impl constructor(
        private val refreshAuthToken: Single<AuthToken>,
        private val isLoggedInObservable: Observable<Boolean>,
        private val currentTime: () -> ZonedDateTime
    ) : FetchAuthTokenUseCase {

        override fun fetcher(
            ifNotLoggedIn: Observable<Boolean>,
            doOnNextEvents: List<NextEvents>
        ): Observable<AuthToken> {
            var authTokenFetcher = refreshAuthToken
                .retry(1)
                .filter { it.isValid(currentTime) }
                .switchIfEmpty(refreshAuthToken)
                .toObservable()

            doOnNextEvents.forEach { event ->
                authTokenFetcher = authTokenFetcher.doOnNext { event(it) }
            }
            return isLoggedInObservable
                .filter { it }
                .switchIfEmpty(ifNotLoggedIn)
                .filter { it }
                .flatMap { authTokenFetcher }
        }

    }
}
