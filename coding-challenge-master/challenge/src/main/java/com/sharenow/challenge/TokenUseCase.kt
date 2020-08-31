package com.sharenow.challenge

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import org.threeten.bp.ZonedDateTime

interface TokenUseCase {

    fun getTokenForLoggedInUser(
        tokenSubject: BehaviorSubject<AuthToken>,
        fetchToken: () -> Observable<AuthToken>
    ): Observable<AuthToken>

    class Impl constructor(private val currentTime: () -> ZonedDateTime) : TokenUseCase {

        override fun getTokenForLoggedInUser(
            tokenSubject: BehaviorSubject<AuthToken>,
            fetchToken: () -> Observable<AuthToken>
        ): Observable<AuthToken> {
            return tokenSubject.flatMap { getToken(tokenSubject, fetchToken) }.hide()
        }

        private fun getToken(
            tokenSubject: BehaviorSubject<AuthToken>,
            fetchToken: () -> Observable<AuthToken>
        ): Observable<AuthToken> {
            return extractTokenFromSubject(tokenSubject)?.toObservable().getOrElse(fetchToken())
        }

        private fun extractTokenFromSubject(tokenSubject: BehaviorSubject<AuthToken>): AuthToken? {
            return tokenSubject.value?.takeIf { it.isValid(currentTime) }
        }

        private fun AuthToken.toObservable(): Observable<AuthToken> {
            return Observable.just(this)
        }

        private fun Observable<AuthToken>?.getOrElse(orElse: Observable<AuthToken>): Observable<AuthToken> {
            return this ?: orElse
        }
    }
}
